import {
  collection,
  doc,
  query,
  where,
  getDocs,
  setDoc,
  documentId,
  onSnapshot,
  updateDoc,
} from "firebase/firestore";
import { db } from "../firebase";
import { UserProfile, NotificationPreferences, MarkerColorKey } from "../data/models";

const USERS_COLLECTION = "users";

export const userService = {
  getProfiles: async (uids: string[]): Promise<Record<string, UserProfile>> => {
    if (uids.length === 0) return {};

    const result: Record<string, UserProfile> = {};
    const distinctUids = Array.from(new Set(uids));

    // Firestore whereIn has a limit of 30 elements
    const chunks = [];
    for (let i = 0; i < distinctUids.length; i += 30) {
      chunks.push(distinctUids.slice(i, i + 30));
    }

    for (const chunk of chunks) {
      const q = query(collection(db, USERS_COLLECTION), where(documentId(), "in", chunk));
      const snapshot = await getDocs(q);
      snapshot.forEach((doc) => {
        result[doc.id] = { uid: doc.id, ...doc.data() } as UserProfile;
      });
    }

    return result;
  },

  updateUserProfile: async (uid: string, displayName: string, photoUrl: string) => {
    await setDoc(
      doc(db, USERS_COLLECTION, uid),
      { displayName, photoUrl },
      { merge: true }
    );
  },

  observeNotificationPreferences: (uid: string, callback: (prefs: NotificationPreferences) => void) => {
    return onSnapshot(doc(db, USERS_COLLECTION, uid, "preferences", "notifications"), (snapshot) => {
      if (snapshot.exists()) {
        callback(snapshot.data() as NotificationPreferences);
      } else {
        callback({ tripInvites: true, itineraryChanges: true, dayInfoChanges: true });
      }
    }, (error) => {
      console.error("Error observing notification preferences:", error);
      callback({ tripInvites: true, itineraryChanges: true, dayInfoChanges: true });
    });
  },

  updateNotificationPreferences: async (uid: string, prefs: NotificationPreferences) => {
    await setDoc(doc(db, USERS_COLLECTION, uid, "preferences", "notifications"), prefs);
  },

  observeActivityColors: (uid: string, callback: (colors: Record<string, string>) => void) => {
    return onSnapshot(doc(db, USERS_COLLECTION, uid, "preferences", "activityColors"), (snapshot) => {
      if (snapshot.exists()) {
        callback(snapshot.data().colors || {});
      } else {
        callback({});
      }
    }, (error) => {
      console.error("Error observing activity colors:", error);
      callback({});
    });
  },

  updateActivityColor: async (uid: string, key: MarkerColorKey, hex: string) => {
    const ref = doc(db, USERS_COLLECTION, uid, "preferences", "activityColors");
    await setDoc(ref, {
      colors: { [key]: hex }
    }, { merge: true });
  },
};
