import {addDoc, collection, doc, getDoc, getDocs, setDoc} from "firebase/firestore";
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
const createScrumPoker = async (teamId,cardType) => {
    const docRef = await addDoc(collection(db, "teams", teamId,"scrumPoker"), {
        cardType: cardType
    });
}
const updateScrumPokerCardType = async (teamId,id,cardType) => {
    const docRef = doc(db, "teams", teamId,"scrumPoker",id);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        data.cardType = cardType
        setDoc(docRef, data);
    }
}

export {getScrumPokerFromTeam,createScrumPoker,updateScrumPokerCardType}