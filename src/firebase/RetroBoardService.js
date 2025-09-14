import {collection, query, getDoc, setDoc, doc, addDoc, getDocs, deleteDoc, where} from 'firebase/firestore';
import {onSnapshot} from 'firebase/firestore';
import {db} from "./Firebase.js";
import {createToast} from "mosha-vue-toastify";

// Connection pool ve throttling için
const activeListeners = new Map();
const throttleTimers = new Map();

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
        columns: columns,
        createdDate: new Date()
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

    // Include votes data with items for better performance
    const itemsWithVotes = await Promise.all(docSnap.docs.map(async (itemDoc) => {
        const itemData = {id: itemDoc.id, ...itemDoc.data()};

        // Get votes for this item
        try {
            const votesCollectionRef = collection(db, "teams", teamId,"retroBoards",retroBoardId,columnName,itemDoc.id,"votes");
            const votesSnap = await getDocs(votesCollectionRef);
            itemData.votes = votesSnap.docs.map(d => ({id: d.id, ...d.data()}));
        } catch (error) {
            console.warn('Error loading votes for item:', itemDoc.id, error);
            itemData.votes = [];
        }

        return itemData;
    }));

    setterFunc(itemsWithVotes);
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

// Optimized listener with throttling and connection management
const listenRetroItemsChange = (teamId, retroBoardId, columnName, setterFunc) => {
    const listenerId = `${teamId}_${retroBoardId}_${columnName}`;

    // Check if we already have a listener for this collection
    if (activeListeners.has(listenerId)) {
        const existingListener = activeListeners.get(listenerId);
        existingListener.callbacks.push(setterFunc);
        return () => {
            // Remove callback from existing listener
            const index = existingListener.callbacks.indexOf(setterFunc);
            if (index > -1) {
                existingListener.callbacks.splice(index, 1);
            }
            // If no more callbacks, clean up the listener
            if (existingListener.callbacks.length === 0) {
                existingListener.unsubscribe();
                activeListeners.delete(listenerId);
            }
        };
    }

    // Create new listener with throttling
    const callbacks = [setterFunc];
    const unsubscribe = onSnapshot(collection(db, "teams", teamId,"retroBoards", retroBoardId,columnName), (snapshot) => {
        // Throttle updates to prevent excessive re-renders
        if (throttleTimers.has(listenerId)) {
            clearTimeout(throttleTimers.get(listenerId));
        }

        throttleTimers.set(listenerId, setTimeout(async () => {
            try {
                // Include votes data with items
                const itemsWithVotes = await Promise.all(snapshot.docs.map(async (itemDoc) => {
                    const itemData = {id: itemDoc.id, ...itemDoc.data()};

                    try {
                        const votesCollectionRef = collection(db, "teams", teamId,"retroBoards",retroBoardId,columnName,itemDoc.id,"votes");
                        const votesSnap = await getDocs(votesCollectionRef);
                        itemData.votes = votesSnap.docs.map(d => ({id: d.id, ...d.data()}));
                    } catch (error) {
                        itemData.votes = [];
                    }

                    return itemData;
                }));

                // Call all registered callbacks
                callbacks.forEach(callback => {
                    if (typeof callback === 'function') {
                        callback(itemsWithVotes);
                    }
                });
            } catch (error) {
                console.error('Error in listener:', error);
            }

            throttleTimers.delete(listenerId);
        }, 100)); // 100ms throttle
    });

    activeListeners.set(listenerId, {
        unsubscribe,
        callbacks
    });

    return () => {
        const index = callbacks.indexOf(setterFunc);
        if (index > -1) {
            callbacks.splice(index, 1);
        }
        if (callbacks.length === 0) {
            unsubscribe();
            activeListeners.delete(listenerId);
            if (throttleTimers.has(listenerId)) {
                clearTimeout(throttleTimers.get(listenerId));
                throttleTimers.delete(listenerId);
            }
        }
    };
}

// Deprecated - replaced with optimized version above
const listenRetroItemVotes = async (teamId, retroBoardId, columnName,itemId, setterFunc) => {
    // This function is now deprecated as votes are included in the main item listener
    console.warn('listenRetroItemVotes is deprecated. Votes are now included with items automatically.');
    return () => {}; // Return empty cleanup function
}

export {
    getRetroBoardsFromTeam,
    createRetroBoard,
    updateRetroBoard,
    removeRetroBoardFromTeam,
    addRetroBoardColumn,
    removeRetroBoardItem,
    getRetroBoard,
    createRetroItem,
    getRetroItems,
    getRetroItemComments,
    createRetroItemComment,
    removeRetroItemComment,
    updateRetroItemStatus,
    setVote,
    getVotes,
    removeVote,
    listenRetroItemsChange,
    listenRetroItemVotes,
    updateRetroItem
}
