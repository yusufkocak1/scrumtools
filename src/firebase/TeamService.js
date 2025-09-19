import {db} from "./Firebase.js";
import {addDoc, collection, deleteDoc, doc, getDoc, getDocs, onSnapshot, query, setDoc, where} from 'firebase/firestore';
import {createToast} from "mosha-vue-toastify";
import {getAuth} from "firebase/auth";


const listenTeams = (setterFunc) => {
    let email = localStorage.getItem("user");
    // Doğru sorgu: memberEmails dizisinde email var mı?
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
        setterFunc( {teamId,...docSnap.data()})
    }
}
const createTeam = async (teamName,email,displayName) => {
    const docRef = await addDoc(collection(db, "teams"), {
        teamName: teamName,
        adminEmail: email,
        members: {[email]:{displayName:displayName}},
        memberEmails: [email], // Yeni: email dizisi
    });
}
const addUserToTeam = async (email,displayName,teamId) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if(!data.members){
            data.members = {}
        }
        if (data.members[email]) {
            createToast('User already in team',{type:'danger',position:'top-center'})
            return
        }
        data.members[email] = {
            displayName: displayName
        }
        // Yeni: memberEmails dizisine ekle
        if (!data.memberEmails) data.memberEmails = [];
        if (!data.memberEmails.includes(email)) data.memberEmails.push(email);
        setDoc(docRef, data);
    }
}

const removeUserFromTeam = async (teamId,email) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        delete data.members[email];
        // Yeni: memberEmails dizisinden çıkar
        if (data.memberEmails) {
            data.memberEmails = data.memberEmails.filter(e => e !== email);
        }
        setDoc(docRef, data);
    }
}
const removeTeam = async (teamName) => {
    const docRef = doc(db, "teams", teamName);
    await deleteDoc(docRef);
}
const updateDisplayNameFromTeam = async (teamId,displayName,email) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if(!data.members){
            docSnap.data().members = {}
        }
        data.members[email] = {
            displayName: displayName
        }
        setDoc(docRef, data);
    }
}

export {createTeam,addUserToTeam,removeUserFromTeam,removeTeam,getTeamById,listenTeams,getTeams,updateDisplayNameFromTeam}