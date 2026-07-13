import {addDoc, collection, deleteDoc, doc, getDoc, getDocs, onSnapshot, setDoc, updateDoc} from "firebase/firestore";
import {db} from "./Firebase.js";

// Cache için
const cache = new Map()
let listenersCache = new Map()

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
    try {
        const docRef = doc(db, "teams", teamId);
        // updateDoc kullanarak sadece gerekli alanı güncelle
        await updateDoc(docRef, { pokerCardType: cardType });
        cache.delete(`team_${teamId}`); // Cache'i temizle
    } catch (error) {
        console.error("Error updating poker card type:", error);
        throw error;
    }
}

// Batch operations için queue sistemi
let updateQueue = new Map()
let updateTimer = null

const updateScrumPokerVote = async (teamId, email, vote) => {
    try {
        const docRef = doc(db, "teams", teamId, "scrumPoker", email);
        await setDoc(docRef, { vote: vote, timestamp: Date.now() });
    } catch (error) {
        console.error("Error updating vote:", error);
        throw error;
    }
}

const leaveScrumPoker = async (teamId,email) => {
    try {
        const docRef = doc(db, "teams", teamId,"scrumPoker",email);
        await deleteDoc(docRef);
    } catch (error) {
        console.error("Error leaving scrum poker:", error);
        throw error;
    }
}

const joinScrumPoker = async (teamId,email) => {
    await updateScrumPokerVote(teamId, email, "-")
}

const INACTIVITY_LIMIT_MS = 24 * 60 * 60 * 1000 // 24 saat

// 24 saattir aktif olmayan (timestamp'i eski veya hiç olmayan) kullanıcıları session'dan çıkar
const cleanupInactiveUsers = async (teamId, currentUserEmail) => {
    try {
        const collectionRef = collection(db, "teams", teamId, "scrumPoker");
        const docSnap = await getDocs(collectionRef);
        const now = Date.now();
        const staleDocs = docSnap.docs.filter(d => {
            if (d.id === currentUserEmail) return false;
            const timestamp = d.data().timestamp;
            return !timestamp || (now - timestamp) > INACTIVITY_LIMIT_MS;
        });
        await Promise.all(staleDocs.map(d => deleteDoc(d.ref)));
    } catch (error) {
        console.error("Error cleaning up inactive users:", error);
    }
}

// Sayfası açık olan kullanıcının timestamp'ini yeniler; cleanup tarafından silinmesini önler
const refreshPresence = async (teamId, email) => {
    try {
        const docRef = doc(db, "teams", teamId, "scrumPoker", email);
        await setDoc(docRef, { timestamp: Date.now() }, { merge: true });
    } catch (error) {
        console.error("Error refreshing presence:", error);
    }
}

// Optimize edilmiş listener - sadece değişiklikleri takip et
const listenScrumPoker = (teamId, setterFunc) => {
    // Mevcut listener'ı temizle
    if (listenersCache.has(`votes_${teamId}`)) {
        listenersCache.get(`votes_${teamId}`)()
    }

    const collectionRef = collection(db, "teams", teamId, "scrumPoker");
    const unsubscribe = onSnapshot(collectionRef, (querySnapshot) => {
        const changes = querySnapshot.docChanges();
        if (changes.length === 0) return; // Değişiklik yoksa işlem yapma

        setterFunc(querySnapshot.docs.map(d => ({email: d.id, ...d.data()})))
    }, (error) => {
        console.error("Error listening to scrum poker:", error);
    });

    listenersCache.set(`votes_${teamId}`, unsubscribe);
    return unsubscribe;
}

const setVotesVisible = async (teamId, votesVisible) => {
    try {
        const docRef = doc(db, "teams", teamId);
        // updateDoc kullanarak sadece gerekli alanı güncelle
        await updateDoc(docRef, { votesVisible: votesVisible });
    } catch (error) {
        console.error("Error setting votes visibility:", error);
        throw error;
    }
}

const listenVotesVisible = (teamId, setterFunc) => {
    // Mevcut listener'ı temizle
    if (listenersCache.has(`visibility_${teamId}`)) {
        listenersCache.get(`visibility_${teamId}`)()
    }

    const docRef = doc(db, "teams", teamId);
    const unsubscribe = onSnapshot(docRef, (doc) => {
        if (doc.exists()) {
            setterFunc(doc.data().votesVisible)
        }
    }, (error) => {
        console.error("Error listening to votes visibility:", error);
    });

    listenersCache.set(`visibility_${teamId}`, unsubscribe);
    return unsubscribe;
}

// Cleanup function - tüm listener'ları temizle
const cleanupListeners = () => {
    listenersCache.forEach(unsubscribe => unsubscribe());
    listenersCache.clear();
}

export {
    getScrumPokerFromTeam,
    updateScrumPokerCardType,
    updateScrumPokerVote,
    leaveScrumPoker,
    joinScrumPoker,
    cleanupInactiveUsers,
    refreshPresence,
    listenScrumPoker,
    setVotesVisible,
    listenVotesVisible,
    cleanupListeners
}
