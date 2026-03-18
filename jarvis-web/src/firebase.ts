import { initializeApp } from "firebase/app";
import { getAuth, GoogleAuthProvider } from "firebase/auth";
import { getFirestore } from "firebase/firestore";
import { getAnalytics } from "firebase/analytics";

// Your web app's Firebase configuration
const firebaseConfig = {
  apiKey: "AIzaSyBhf9-9878LvChIMMf_wPqcqxNYmwe4Ego",
  authDomain: "jarvis-official-support.firebaseapp.com",
  projectId: "translateanywhere-7614e",
  storageBucket: "translateanywhere-7614e.firebasestorage.app",
  messagingSenderId: "600499890236",
  appId: "1:600499890236:web:1b6d5e04cc090354adbb39",
  measurementId: "G-GQK692QVN7"
};

const app = initializeApp(firebaseConfig);
export const auth = getAuth(app);
export const googleProvider = new GoogleAuthProvider();
export const db = getFirestore(app);
export const analytics = getAnalytics(app);

export default app;
