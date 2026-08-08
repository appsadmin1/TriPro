import {
  collection,
  collectionGroup,
  addDoc,
  query,
  where,
  orderBy,
  onSnapshot,
  serverTimestamp,
} from "firebase/firestore";
import { db } from "../firebase";
import { ActivityEntry, ActivityType } from "../data/models";

export const activityService = {
  observeRecentActivity: (uid: string, callback: (activities: ActivityEntry[]) => void) => {
    // Collection group query to observe all activity across all trips the user belongs to
    const q = query(
      collectionGroup(db, "activity"),
      where("memberIds", "array-contains", uid),
      orderBy("createdAt", "desc")
    );

    return onSnapshot(q, (snapshot) => {
      const activities = snapshot.docs.map((doc) => ({
        id: doc.id,
        ...doc.data(),
      } as ActivityEntry));
      callback(activities);
    });
  },

  logActivity: async (
    tripId: string,
    tripName: string,
    memberIds: string[],
    type: ActivityType,
    message: string,
    actorUid: string,
    actorName: string,
    date?: string
  ) => {
    const activityRef = collection(db, "trips", tripId, "activity");
    await addDoc(activityRef, {
      tripId,
      tripName,
      memberIds,
      type,
      message,
      actorUid,
      actorName,
      date: date || null,
      createdAt: serverTimestamp(),
    });
  },
};
