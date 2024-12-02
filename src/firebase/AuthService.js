import {app} from "./Firebase.js";
import {getFirestore, collection, getDoc, setDoc, doc, addDoc} from 'firebase/firestore';
import {getAuth, createUserWithEmailAndPassword, signInWithEmailAndPassword, updateProfile,updateEmail,sendPasswordResetEmail,updatePassword} from "firebase/auth";
import {createToast} from "mosha-vue-toastify";

const db = getFirestore(app);

const authService = getAuth();
const signUp = (email, password, name, response) => {
    createUserWithEmailAndPassword(authService, email, password)
        .then((userCredential) => {
            // Signed up
            updateDisplayName(name,(r)=>{
                response(r)
            })
            // ...
        })
        .catch((error) => {
            const errorMessage = error.message;
            createToast('Something went wrong. ' + errorMessage, {type: 'danger', position: 'top-center'})

            // ..
        });
}
const updateDisplayName = (name,response) => {
    updateProfile(authService.currentUser, {displayName: name}).then(r => response("name updated"))
}
const changeEmail = (email,response) => {
    if(authService){
    updateEmail(authService.currentUser, email).then(() => {
        response("email updated")
        // ...
    }).catch((error) => {
        response(error)
    });
    }
}
const login = (email, password, response) => {
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
const logout = () => {
    authService.signOut().then(r => {
        localStorage.removeItem('user')
    })
}

const getUserFromDB = async (email, setterFunc) => {
    if (getAuth().currentUser) {
        setterFunc({name: getAuth().currentUser.displayName, email: getAuth().currentUser.email})
    }
}
const resetPassword = (email) => {
    sendPasswordResetEmail(authService, email)
        .then(() => {
            createToast("Check your email",{type:'success',position:'top-center'})
            // Password reset email sent!
            // ..
        })
        .catch((error) => {
            const errorCode = error.code;
            const errorMessage = error.message;
            createToast("Something went wrong. " + errorMessage, {type: 'danger', position: 'top-center'})
        });}
const  changePassword = (password,response) => {
    updatePassword(authService.currentUser, password)
        .then(() => {
            response("password updated")
        })
        .catch((error) => {
            const errorMessage = error.message;
            createToast("Something went wrong. " + errorMessage, {type: 'danger', position: 'top-center'})
            // ..
        });
}

export {authService, signUp, login, logout, getUserFromDB, updateDisplayName, changeEmail,resetPassword,changePassword}