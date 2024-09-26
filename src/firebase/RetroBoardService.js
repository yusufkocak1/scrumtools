import {collection, query, getDoc, setDoc, doc, addDoc, getDocs, deleteDoc, where} from 'firebase/firestore';
import {onSnapshot} from 'firebase/firestore';
import {db} from "./Firebase.js";
import {createToast} from "mosha-vue-toastify";

const getRetroBoardsFromTeam = async (teamId, setterFunc) => {

    const collectionRef = collection(db, "teams", teamId,"retroBoards");
    const docSnap = await getDocs(collectionRef);
    setterFunc(docSnap.docs.map(d => ({id: d.id, ...d.data()})))

}
const getRetroBoard= async (teamId,boardId, setterFunc)=>{
    const docRef = doc(db, "teams", teamId,"retroBoards",boardId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        setterFunc( docSnap.data())
    }
}
const createRetroBoard = async (teamId,retroBoardName,columns) => {
    const docRef = await addDoc(collection(db, "teams", teamId,"retroBoards"), {
        retroBoardName: retroBoardName,
        columns: columns
    });
}
const updateRetroBoard = async (teamId,retroId,retroBoardName,setterFunc) => {
    const docRef = doc(db, "teams", teamId,"retroBoards", retroId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let data = docSnap.data();
        data.retroBoardName = retroBoardName
        setDoc(docRef, data);
    }

}

const removeRetroBoardFromTeam = async (teamID,retroBoardId) => {
    const docRef = doc(db, "teams", teamID,"retroBoards", retroBoardId);
    await deleteDoc(docRef);

}
const addRetroBoardColumn = async (teamName,retroBoardName,columnName) => {
   //add retroBoardColumn
    const docRef = doc(db, "teams", teamName,"retroBoards", retroBoardName);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        docSnap.data().columns = [columnName]
        setDoc(docRef, docSnap.data());
    }
}


const removeRetroBoardItem = async (teamId,retroBoardId,columnName,itemId) => {
    const docRef = doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId);
    await deleteDoc(docRef);
}
const createRetroItem = async (teamId,retroBoardId,columnName,item) => {
     await addDoc(collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName), item);

}
const createRetroItemComment = async (teamId,retroBoardId,columnName,itemId,comment) => {
    await addDoc(collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"comments"), comment);
}
const getRetroItems = async (teamId, retroBoardId,columnName, setterFunc) => {
    const collectionRef = collection(db, "teams", teamId,"retroBoards",retroBoardId,columnName);
    const docSnap = await getDocs(collectionRef);
    setterFunc(docSnap.docs.map(d => ({id: d.id, ...d.data()})))
}

const getRetroItemComments = async (teamId, retroBoardId,columnName,itemId, setterFunc) => {
    const collectionRef = collection(db, "teams", teamId,"retroBoards",retroBoardId,columnName,itemId,"comments");
    const docSnap = await getDocs(collectionRef);
    setterFunc(docSnap.docs.map(d => ({id: d.id, ...d.data()})))
}
const removeRetroItemComment = async (teamId, retroBoardId,columnName,itemId,commentId) => {
    const docRef = doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"comments",commentId);
    await deleteDoc(docRef);
}
const updateRetroItem= async (teamId, retroBoardId,columnName,itemId,value)=>{
    const docRef = doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let item = {...docSnap.data(),value:value}
        setDoc(docRef, item);
    }
}
const updateRetroItemStatus = async (teamId, retroBoardId,columnName,itemId,status) => {
    const docRef = doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        let item = {...docSnap.data(),status:status}
        setDoc(docRef, item);
    }
}
const setVote = async (teamId, retroBoardId, columnName, itemId, vote) => {
    await setDoc(doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"votes",vote.owner), vote);
}
const getVotes = async (teamId, retroBoardId, columnName, itemId, setterFunc) => {
    const collectionRef = collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"votes");
    const docSnap = await getDocs(collectionRef);
        setterFunc(docSnap.docs.map(d => ({id: d.id, ...d.data()})))

}
const removeVote = async (teamId, retroBoardId, columnName, itemId, owner) => {
    const docRef = doc(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"votes",owner);
    await deleteDoc(docRef);
}
const listenRetroItemsChange = async (teamId, retroBoardId, columnName, setterFunc) => {
     onSnapshot(collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName), (snapshot) => {
            setterFunc(snapshot.docs.map(d => ({id: d.id, ...d.data()})))
        });
}
const listenRetroItemVotes = async (teamId, retroBoardId, columnName,itemId, setterFunc) => {
     onSnapshot(collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName,itemId,"votes"), (snapshot) => {
        setterFunc(snapshot.docs.map(d => ({id: d.id, ...d.data()})))
    });
}


export {getRetroBoardsFromTeam,createRetroBoard,updateRetroBoard,removeRetroBoardFromTeam,addRetroBoardColumn,removeRetroBoardItem,getRetroBoard,createRetroItem,getRetroItems,getRetroItemComments,createRetroItemComment,removeRetroItemComment,updateRetroItemStatus,setVote,getVotes,removeVote,listenRetroItemsChange,listenRetroItemVotes,updateRetroItem}