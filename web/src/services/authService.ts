import {
  onAuthStateChanged,
  signInWithPopup,
  GoogleAuthProvider,
  signOut as firebaseSignOut,
  User,
  updateProfile
} from "firebase/auth";
import { auth } from "../firebase";

export const authService = {
  subscribeToAuthChanges: (callback: (user: User | null) => void) => {
    return onAuthStateChanged(auth, callback);
  },

  updateUserProfile: async (displayName: string, photoURL: string) => {
    const user = auth.currentUser;
    if (user) {
      await updateProfile(user, { displayName, photoURL });
    }
  },

  signInWithGoogle: async () => {
    const provider = new GoogleAuthProvider();
    try {
      const result = await signInWithPopup(auth, provider);
      return result.user;
    } catch (error) {
      console.error("Error signing in with Google", error);
      throw error;
    }
  },

  signOut: async () => {
    try {
      await firebaseSignOut(auth);
    } catch (error) {
      console.error("Error signing out", error);
      throw error;
    }
  },

  getCurrentUser: () => auth.currentUser,
};
