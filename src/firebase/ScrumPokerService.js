import {addDoc, collection, deleteDoc, doc, getDoc, getDocs, onSnapshot, setDoc} from "firebase/firestore";
import {db} from "./Firebase.js";

const getScrumPokerFromTeam = async (teamId, setterFunc) => {
    const collectionRef = collection(db, "teams", teamId,"scrumPoker");
    const docSnap = await getDocs(collectionRef);
    if (docSnap.docs.length > 0){
        setterFunc(docSnap.docs.map(d => ({id: d.id, ...d.data()})))
    }else{
        setterFunc(null)
    }
}
const updateScrumPokerCardType = async (teamId,cardType) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        data.pokerCardType = cardType
        console.log(data)
        await setDoc(docRef, data);
    }
}
const updateScrumPokerVote = async (teamId,email,vote) => {
   const docRef = doc(db, "teams", teamId,"scrumPoker",email);
   await setDoc(docRef, {vote: vote})
}
const leaveScrumPoker = async (teamId,email) => {
    const docRef = doc(db, "teams", teamId,"scrumPoker",email);
   //delete
    await deleteDoc(docRef);
}
const joinScrumPoker = async (teamId,email) => {
    await updateScrumPokerVote(teamId, email, "-")
}
const listenScrumPoker = async (teamId, setterFunc) => {
    const collectionRef = collection(db, "teams", teamId,"scrumPoker");
    onSnapshot(collectionRef, (querySnapshot) => {
        setterFunc(querySnapshot.docs.map(d => ({email: d.id, ...d.data()})))
    })
}
const setVotesVisible = async (teamId,votesVisible) => {
    const docRef = doc(db, "teams", teamId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        data.votesVisible = votesVisible
        await setDoc(docRef, data);
    }
}
const listenVotesVisible = async (teamId, setterFunc) => {
    const docRef = doc(db, "teams", teamId);
    onSnapshot(docRef, (doc) => {
        setterFunc(doc.data().votesVisible)
    })
}

export {getScrumPokerFromTeam,updateScrumPokerCardType,updateScrumPokerVote,leaveScrumPoker,joinScrumPoker,listenScrumPoker,setVotesVisible,listenVotesVisible}