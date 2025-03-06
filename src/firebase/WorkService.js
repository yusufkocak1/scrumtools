import {addDoc, collection, doc, onSnapshot, updateDoc} from "firebase/firestore";
import {db} from "./Firebase.js";


const addTask = async (teamId, newTask) => {
    await addDoc(collection(db, "teams", teamId, "tasks"), newTask);
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

export {addTask, addSprint,addTaskToSprint,listenTasks,listenSprints,updateSprintStatus}