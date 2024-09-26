import {app,db} from "./Firebase.js";
import {
    query,
    where,
    getDocs,
    collection,
    getDoc,
    setDoc,
    doc,
    addDoc,
    deleteDoc,
    onSnapshot
} from 'firebase/firestore';
import {getAuth} from "firebase/auth";
import {authService} from "./AuthService.js";


const getTeams = async (setterFunc) => {
    const q = query(collection(db, "teams"), where("members", "array-contains", authService.currentUser.email));
    const querySnapshot = await getDocs(q);
    const teamList= []
    querySnapshot.forEach((doc) => {

        teamList.push( {...doc.data(),id : doc.id})
    });
    setterFunc( teamList)
}
const listenTeams = (setterFunc) => {

    const q = query(collection(db, "teams"), where("members", "array-contains", localStorage.getItem("user")));
     onSnapshot(q, (querySnapshot) => {
        const teamList= []
        querySnapshot.forEach((doc) => {
            teamList.push( {...doc.data(),id : doc.id})
        });
        setterFunc( teamList)
    });
}
const getTeamById = async (teamId, setterFunc) => {
    console.log(teamId)
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        setterFunc( docSnap.data())
    }
}
const createTeam = async (teamName,email) => {

    const docRef = await addDoc(collection(db, "teams"), {
        teamName: teamName,
        adminEmail: email,
        members: [email],
    });
}
const addUserToTeam = async (email,teamId) => {

    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if(!data.members){
            docSnap.data().members = []
        }
        if (data.members.includes(email)) {
            return
        }
        data.members.push(email)
        setDoc(docRef, data);
    }
}

const removeUserFromTeam = async (teamName,email) => {
    const docRef = doc(db, "teams", teamName);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        docSnap.data().members = docSnap.data().members.filter((member) => member !== email)
        setDoc(docRef, docSnap.data());
    }
}
const removeTeam = async (teamName) => {
    const docRef = doc(db, "teams", teamName);
    await deleteDoc(docRef);
}

export {getTeams,createTeam,addUserToTeam,removeUserFromTeam,removeTeam,getTeamById,listenTeams}