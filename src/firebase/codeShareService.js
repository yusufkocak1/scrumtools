import {
    addDoc,
    collection,
    deleteDoc,
    doc,
    getDoc,
    getDocs,
    onSnapshot,
    query,
    setDoc,
    where
} from "firebase/firestore";
import {db} from "./Firebase.js";
import {createToast} from "mosha-vue-toastify";

const saveCodeShare = async (teamId, codeShare, tag) => {
    try {
        if (!teamId || !tag) throw new Error("teamId ve tag zorunlu");

        // 1. Doküman referansını oluştur (tag'i ID olarak kullan)
        const docRef = doc(db, "teams", teamId, "codeShare", tag);

        // 2. Dokümanı üzerine yaz
        await setDoc(docRef, {
            data: codeShare,
            updatedAt: new Date()
        }, { merge: true }); // merge: mevcut veriyi korur, sadece değişenleri günceller

        createToast("Başarıyla kaydedildi!", { type: 'success', position: 'top-center' });
    } catch (error) {
        console.error("Kayıt hatası:", error);
        createToast("Kayıt başarısız: " + error.message, { type: 'error', position: 'top-center' });
    }
};

const getCodeShare = async (teamId, tag, setterFunc) => {
    try {
        // 1. Aynı doküman referansını kullan
        const docRef = doc(db, "teams", teamId, "codeShare", tag);

        // 2. Tek dokümanı çek
        const docSnap = await getDoc(docRef);

        // 3. State'i güncelle
        if (docSnap.exists()) {
            setterFunc(docSnap.data());
        } else {
            setterFunc(null); // Doküman yoksa null olarak ayarla
        }
    } catch (error) {
        console.error("Veri çekme hatası:", error);
        createToast("Veri alınamadı", { type: 'error', position: 'top-center' });
    }
};
export {saveCodeShare,getCodeShare}