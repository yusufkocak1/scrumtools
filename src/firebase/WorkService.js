import {addDoc, collection, doc, onSnapshot, updateDoc, deleteDoc, getDoc, getDocs, query, where} from "firebase/firestore";
import {db} from "./Firebase.js";

const generateTaskId = async (teamId) => {
    try {
        // Takım bilgisini al
        const teamDoc = await getDoc(doc(db, "teams", teamId));
        if (!teamDoc.exists()) {
            throw new Error("Team not found");
        }

        const teamData = teamDoc.data();
        const teamCode = teamData.teamCode || "TEAM";

        // Takımın mevcut task sayısını al
        const tasksSnapshot = await getDocs(collection(db, "teams", teamId, "tasks"));
        const taskCount = tasksSnapshot.size;

        // Yeni task ID'sini oluştur: TEAMCODE-NUMBER
        const taskNumber = taskCount + 1;
        return `${teamCode}-${taskNumber}`;
    } catch (error) {
        console.error("Error generating task ID:", error);
        // Fallback olarak timestamp kullan
        return `TASK-${Date.now()}`;
    }
}

const addTask = async (teamId, newTask) => {
    // Otomatik task ID oluştur
    const taskId = await generateTaskId(teamId);

    // Task verisine custom ID ekle
    const taskWithId = {
        ...newTask,
        customId: taskId,
        createdAt: new Date().toISOString()
    };

    await addDoc(collection(db, "teams", teamId, "tasks"), taskWithId);
}
const addSprint = async (teamId, newSprint) => {
    await addDoc(collection(db, "teams", teamId, "sprints"), newSprint);
}

const addTaskToSprint = async (teamId, taskId, sprintId) => {
    await updateDoc(doc(db, "teams", teamId, "tasks", taskId), {sprintId: sprintId});
}

const listenTasks = async (teamId, setterFunc) => {
    onSnapshot(collection(db, "teams", teamId, "tasks"), (snapshot) => {
        let tasks = snapshot.docs.map((doc) => ({
            id: doc.id,
            ...doc.data(),
        }));
        console.log()
        setterFunc(tasks);
    });
}
const listenSprints = async (teamId, setterFunc) => {
    onSnapshot(collection(db, "teams", teamId, "sprints"), (snapshot) => {
        let sprints = snapshot.docs.map((doc) => ({
            id: doc.id,
            ...doc.data(),
        }));
        setterFunc(sprints);
    });
}
const updateSprintStatus = async (teamId, sprintId, status) => {
    await updateDoc(doc(db, "teams", teamId, "sprints", sprintId), {status: status});
}

const updateTask = async (teamId, taskId, taskData) => {
    await updateDoc(doc(db, "teams", teamId, "tasks", taskId), taskData);
}

const updateTaskStatus = async (teamId, taskId, status) => {
    await updateDoc(doc(db, "teams", teamId, "tasks", taskId), {status: status});
}

const deleteTask = async (teamId, taskId) => {
    await deleteDoc(doc(db, "teams", teamId, "tasks", taskId));
}

const getTaskByCustomId = async (teamId, customId) => {
    try {
        const q = query(
            collection(db, "teams", teamId, "tasks"),
            where("customId", "==", customId)
        );
        const querySnapshot = await getDocs(q);

        if (!querySnapshot.empty) {
            const doc = querySnapshot.docs[0];
            return { id: doc.id, ...doc.data() };
        } else {
            return null;
        }
    } catch (error) {
        console.error("Error getting task by customId:", error);
        return null;
    }
}

const getTask = async (teamId, taskId) => {
    const docRef = doc(db, "teams", teamId, "tasks", taskId);
    const docSnap = await getDoc(docRef);
    if (docSnap.exists()) {
        return { id: docSnap.id, ...docSnap.data() };
    } else {
        return null;
    }
}

const findTaskInUserTeams = async (customId) => {
    try {
        const userEmail = localStorage.getItem("user");
        if (!userEmail) {
            throw new Error("User not authenticated");
        }

        // Kullanıcının takımlarını al
        const teamsQuery = query(
            collection(db, "teams"),
            where("memberEmails", "array-contains", userEmail)
        );
        const teamsSnapshot = await getDocs(teamsQuery);

        // Her takımda task ara
        for (const teamDoc of teamsSnapshot.docs) {
            const teamId = teamDoc.id;
            const teamData = teamDoc.data();

            // Bu takımda customId ile task ara
            const tasksQuery = query(
                collection(db, "teams", teamId, "tasks"),
                where("customId", "==", customId)
            );
            const tasksSnapshot = await getDocs(tasksQuery);

            if (!tasksSnapshot.empty) {
                const taskDoc = tasksSnapshot.docs[0];
                return {
                    task: { id: taskDoc.id, ...taskDoc.data() },
                    teamId: teamId,
                    teamData: teamData
                };
            }
        }

        return null; // Task bulunamadı
    } catch (error) {
        console.error("Error finding task in user teams:", error);
        return null;
    }
}

export {addTask, addSprint,addTaskToSprint,listenTasks,listenSprints,updateSprintStatus,updateTask,updateTaskStatus,deleteTask,getTask,getTaskByCustomId,findTaskInUserTeams}
