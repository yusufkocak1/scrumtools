import {app} from "./Firebase.js";
import { getFirestore, collection, getDoc,setDoc,doc,addDoc } from 'firebase/firestore';
import { getAuth, createUserWithEmailAndPassword,signInWithEmailAndPassword,updateProfile } from "firebase/auth";
import {createToast} from "mosha-vue-toastify";

const db = getFirestore(app);

const authService = getAuth();
const signUp = ( email, password,name)=>{
    createUserWithEmailAndPassword(authService, email, password)
        .then((userCredential) => {
            // Signed up
            updateProfile(authService.currentUser, {displayName: name}).then(r  =>console.log(authService.currentUser.displayName))

            // ...
        })
        .catch((error) => {
            const errorMessage = error.message;
            createToast('Something went wrong. ' + errorMessage,{type:'danger',position:'top-center'})

            // ..
        });
}

const login = (email, password,response)=>{
    signInWithEmailAndPassword(authService, email, password)
        .then((userCredential) => {
            // Signed in
            const user = userCredential.user;
            localStorage.setItem('user', user.email)
            response(true)
        })
        .catch((error) => {
            const errorCode = error.code;
            const errorMessage = error.message;
            response(false)
        });
}
const logout = ()=>{
    authService.signOut().then(r => {
        localStorage.removeItem('user')
    })
}

const getUserFromDB = async (email,setterFunc) => {
    if (getAuth().currentUser) {
        setterFunc({name: getAuth().currentUser.displayName, email: getAuth().currentUser.email})
    }
}


export {authService, signUp, login, logout,getUserFromDB}