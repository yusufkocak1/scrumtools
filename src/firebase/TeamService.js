import {db} from "./Firebase.js";
import {addDoc, collection, deleteDoc, doc, getDoc, getDocs, onSnapshot, query, setDoc, where} from 'firebase/firestore';
import {createToast} from "mosha-vue-toastify";
import {getAuth} from "firebase/auth";

const listenTeams = (setterFunc) => {
    let email = localStorage.getItem("user");
    const q = query(collection(db, "teams"), where("memberEmails", "array-contains", email));
    onSnapshot(q, (querySnapshot) => {
        const teamList= []
        querySnapshot.forEach((doc) => {
            teamList.push({...doc.data(), id: doc.id})
        });
        setterFunc(teamList)
    });
}

const getTeams = async (setterFunc) => {
    const auth = getAuth();
    const user = auth.currentUser;

    if (!user || !user.email) {
        setterFunc([]);
        return;
    }

    const email = user.email;
    const q = query(collection(db, "teams"), where("memberEmails", "array-contains", email));
    getDocs(q).then((querySnapshot) => {
        const teamList = []
        querySnapshot.forEach((doc) => {
            teamList.push({...doc.data(), id: doc.id})
        });
        setterFunc(teamList)
    }).catch((error) => {
        setterFunc([]);
    });
}

const getTeamById = async (teamId, setterFunc) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        setterFunc({teamId,...docSnap.data()})
    }
}

const createTeam = async (teamName, teamCode, email, displayName) => {
    const docRef = await addDoc(collection(db, "teams"), {
        teamName: teamName,
        teamCode: teamCode,
        adminEmail: email,
        members: {
            [email]: {
                displayName: displayName,
                role: 'admin',
                skills: []
            }
        },
        memberEmails: [email],
    });
}

const addUserToTeam = async (email, displayName, teamId) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if(!data.members){
            data.members = {}
        }
        if (data.members[email]) {
            createToast('Kullanıcı zaten takımda',{type:'danger',position:'top-center'})
            return
        }
        // Yeni üye observer rolü ile eklenir
        data.members[email] = {
            displayName: displayName,
            role: 'observer',
            skills: []
        }
        if (!data.memberEmails) data.memberEmails = [];
        if (!data.memberEmails.includes(email)) data.memberEmails.push(email);
        await setDoc(docRef, data);
        createToast('Takıma başarıyla katıldınız',{type:'success',position:'top-center'})
    }
}

const updateMemberRole = async (teamId, memberEmail, role, skills = []) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if (data.members && data.members[memberEmail]) {
            data.members[memberEmail] = {
                ...data.members[memberEmail],
                role: role,
                skills: skills
            };
            await setDoc(docRef, data);
            createToast('Üye bilgileri güncellendi',{type:'success',position:'top-center'})
        }
    }
}

const removeUserFromTeam = async (teamId, email) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();

        // Admin kendini kaldıramaz
        if (email === data.adminEmail) {
            createToast('Takım yöneticisi kendini takımdan kaldıramaz',{type:'danger',position:'top-center'})
            return;
        }

        if (data.members && data.members[email]) {
            delete data.members[email];
        }

        if (data.memberEmails && data.memberEmails.includes(email)) {
            data.memberEmails = data.memberEmails.filter(memberEmail => memberEmail !== email);
        }

        await setDoc(docRef, data);
    }
}

// Rol güncelleme fonksiyonu ekle
const updateMemberRoleAndSkills = async (teamId, memberEmail, newRole, newSkills = []) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if (data.members && data.members[memberEmail]) {
            data.members[memberEmail].role = newRole;
            data.members[memberEmail].skills = newSkills;
            await setDoc(docRef, data);
            return true;
        }
    }
    return false;
}

const updateDisplayNameFromTeam = async (newDisplayName, userEmail) => {
    try {
        // Kullanıcının üye olduğu tüm takımları bul
        const q = query(collection(db, "teams"), where("memberEmails", "array-contains", userEmail));
        const querySnapshot = await getDocs(q);

        // Her takımda kullanıcının displayName'ini güncelle
        const updatePromises = [];
        querySnapshot.forEach((docSnapshot) => {
            const teamData = docSnapshot.data();
            if (teamData.members && teamData.members[userEmail]) {
                teamData.members[userEmail].displayName = newDisplayName;
                updatePromises.push(setDoc(doc(db, "teams", docSnapshot.id), teamData));
            }
        });

        await Promise.all(updatePromises);
        createToast('Takımlardaki isim bilginiz güncellendi', {type: 'success', position: 'top-center'});
    } catch (error) {
        console.error('Error updating display name in teams:', error);
        createToast('Takımlardaki isim güncellenirken hata oluştu', {type: 'danger', position: 'top-center'});
    }
}

export {
    listenTeams,
    getTeams,
    getTeamById,
    createTeam,
    addUserToTeam,
    updateMemberRole,
    removeUserFromTeam,
    updateMemberRoleAndSkills,
    updateDisplayNameFromTeam
}
