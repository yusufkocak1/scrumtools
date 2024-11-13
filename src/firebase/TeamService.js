import {db} from "./Firebase.js";
import {addDoc, collection, deleteDoc, doc, getDoc, getDocs, onSnapshot, query, setDoc} from 'firebase/firestore';
import {createToast} from "mosha-vue-toastify";
import {getAuth} from "firebase/auth";


const listenTeams = (setterFunc) => {

    let email = localStorage.getItem("user");
    console.log(email)
    const q = query(collection(db, "teams"));
     onSnapshot(q, (querySnapshot) => {
        const teamList= []
        querySnapshot.forEach((doc) => {
            if (doc.data().members[email]) {
                teamList.push({...doc.data(), id: doc.id})
            }
        });
        setterFunc( teamList)
    });
}
const getTeams = async (setterFunc) => {
    let email = localStorage.getItem("user");
getDocs(collection(db, "teams")).then((querySnapshot) => {
    const teamList= []
    querySnapshot.forEach((doc) => {
        if (doc.data().members[email]) {
            teamList.push({...doc.data(), id: doc.id})
        }
    });
    setterFunc( teamList)
})
}
const getTeamById = async (teamId, setterFunc) => {
    console.log(teamId)
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        setterFunc( docSnap.data())
    }
}
const createTeam = async (teamName,email,displayName) => {
    const docRef = await addDoc(collection(db, "teams"), {
        teamName: teamName,
        adminEmail: email,
        members: {[email]:{displayName:displayName}},
    });
}
const addUserToTeam = async (email,displayName,teamId) => {

    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        if(!data.members){
            docSnap.data().members = {}
        }
        if (data.members[email]) {
            createToast('User already in team',{type:'danger',position:'top-center'})
            return
        }
        data.members[email] = {
            displayName: displayName
        }
        setDoc(docRef, data);
    }
}

const removeUserFromTeam = async (teamName,email) => {
    const docRef = doc(db, "teams", teamName);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        delete data.members[email];
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