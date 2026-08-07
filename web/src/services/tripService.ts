import {
  collection,
  doc,
  query,
  where,
  onSnapshot,
  setDoc,
  addDoc,
  updateDoc,
  deleteDoc,
  getDocs,
  orderBy,
  writeBatch,
  serverTimestamp,
  arrayUnion,
  arrayRemove,
  deleteField
} from "firebase/firestore";
import { db } from "../firebase";
import { Trip, TripDay, ItineraryItem, Role } from "../data/models";
import { format, addDays, parseISO } from "date-fns";

const TRIPS_COLLECTION = "trips";

export const tripService = {
  observeUserTrips: (uid: string, callback: (trips: Trip[]) => void) => {
    const q = query(
      collection(db, TRIPS_COLLECTION),
      where("memberIds", "array-contains", uid)
    );
    return onSnapshot(q, (snapshot) => {
      const trips = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() } as Trip));
      callback(trips);
    });
  },

  observeTrip: (tripId: string, callback: (trip: Trip | null) => void) => {
    return onSnapshot(doc(db, TRIPS_COLLECTION, tripId), (snapshot) => {
      if (snapshot.exists()) {
        callback({ id: snapshot.id, ...snapshot.data() } as Trip);
      } else {
        callback(null);
      }
    });
  },

  createTrip: async (
    name: string,
    destination: string,
    coverImageUrl: string,
    coverImagePublicId: string,
    coverImageResourceType: string,
    startDate: string,
    endDate: string,
    ownerId: string,
    ownerName: string
  ) => {
    const tripRef = doc(collection(db, TRIPS_COLLECTION));
    const tripData = {
      name,
      destination,
      coverImageUrl,
      coverImagePublicId,
      coverImageResourceType,
      startDate,
      endDate,
      ownerId,
      ownerName,
      members: { [ownerId]: Role.OWNER },
      memberIds: [ownerId],
      createdAt: serverTimestamp(),
    };

    const batch = writeBatch(db);
    batch.set(tripRef, tripData);

    let currentDate = parseISO(startDate);
    const lastDate = parseISO(endDate);
    let index = 1;

    while (currentDate <= lastDate) {
      const dateStr = format(currentDate, "yyyy-MM-dd");
      const dayRef = doc(db, TRIPS_COLLECTION, tripRef.id, "days", dateStr);
      batch.set(dayRef, {
        date: dateStr,
        dayIndex: index,
        dayNote: "",
        updatedBy: ownerId,
      });
      currentDate = addDays(currentDate, 1);
      index++;
    }

    await batch.commit();
    return tripRef.id;
  },

  updateTripDetails: async (
    tripId: string,
    name: string,
    destination: string,
    coverImageUrl: string | null,
    coverImagePublicId: string | null,
    coverImageResourceType: string | null,
    startDate: string,
    endDate: string
  ) => {
    const tripRef = doc(db, TRIPS_COLLECTION, tripId);
    const daysSnapshot = await getDocs(collection(db, TRIPS_COLLECTION, tripId, "days"));
    const existingDates = new Set(daysSnapshot.docs.map((d) => d.id));

    const batch = writeBatch(db);
    const updateData: any = {
      name,
      destination,
      startDate,
      endDate,
    };

    if (coverImageUrl) {
      updateData.coverImageUrl = coverImageUrl;
      updateData.coverImagePublicId = coverImagePublicId || "";
      updateData.coverImageResourceType = coverImageResourceType || "";
    }

    batch.update(tripRef, updateData);

    let currentDate = parseISO(startDate);
    const lastDate = parseISO(endDate);
    let index = 1;

    while (currentDate <= lastDate) {
      const dateStr = format(currentDate, "yyyy-MM-dd");
      const dayRef = doc(db, TRIPS_COLLECTION, tripId, "days", dateStr);
      if (existingDates.has(dateStr)) {
        batch.update(dayRef, { dayIndex: index });
      } else {
        batch.set(dayRef, {
          date: dateStr,
          dayIndex: index,
          dayNote: "",
          updatedBy: "",
        });
      }
      currentDate = addDays(currentDate, 1);
      index++;
    }

    await batch.commit();
  },

  deleteTrip: async (tripId: string) => {
    await deleteDoc(doc(db, TRIPS_COLLECTION, tripId));
  },

  observeDays: (tripId: string, callback: (days: TripDay[]) => void) => {
    const q = query(collection(db, TRIPS_COLLECTION, tripId, "days"), orderBy("dayIndex", "asc"));
    return onSnapshot(q, (snapshot) => {
      callback(snapshot.docs.map((doc) => ({ ...doc.data(), date: doc.id } as TripDay)));
    });
  },

  updateDayNote: async (tripId: string, date: string, note: string, updatedBy: string) => {
    await updateDoc(doc(db, TRIPS_COLLECTION, tripId, "days", date), {
      dayNote: note,
      updatedBy,
    });
  },

  observeItems: (tripId: string, date: string, callback: (items: ItineraryItem[]) => void) => {
    const q = collection(db, TRIPS_COLLECTION, tripId, "days", date, "items");
    return onSnapshot(q, (snapshot) => {
      const items = snapshot.docs.map((doc) => ({ id: doc.id, ...doc.data() } as ItineraryItem));
      // Manual sorting as sortMinutes depends on item data logic
      items.sort((a, b) => {
        const aMin = calculateSortMinutes(a);
        const bMin = calculateSortMinutes(b);
        return aMin - bMin;
      });
      callback(items);
    });
  },

  addItem: async (tripId: string, date: string, item: Omit<ItineraryItem, "id">) => {
    const tripRef = doc(db, TRIPS_COLLECTION, tripId);
    const dayRef = doc(tripRef, "days", date);
    const itemRef = doc(collection(dayRef, "items"));
    await setDoc(itemRef, { ...item, id: itemRef.id, updatedBy: item.createdBy });
    return itemRef.id;
  },

  updateItem: async (tripId: string, date: string, itemId: string, item: Partial<ItineraryItem>, updatedBy: string) => {
    const ref = doc(db, TRIPS_COLLECTION, tripId, "days", date, "items", itemId);
    await updateDoc(ref, { ...item, updatedBy });
  },

  deleteItem: async (tripId: string, date: string, itemId: string) => {
    await deleteDoc(doc(db, TRIPS_COLLECTION, tripId, "days", date, "items", itemId));
  },

  setMemberRole: async (tripId: string, uid: string, role: Role) => {
    const tripRef = doc(db, TRIPS_COLLECTION, tripId);
    await updateDoc(tripRef, {
      [`members.${uid}`]: role,
      memberIds: arrayUnion(uid),
    });
  },

  removeMember: async (tripId: string, uid: string) => {
    const tripRef = doc(db, TRIPS_COLLECTION, tripId);
    await updateDoc(tripRef, {
      [`members.${uid}`]: deleteField(),
      memberIds: arrayRemove(uid),
    });
  },

  observeAllItemsForTrip: (tripId: string, dates: string[], callback: (itemsByDate: Record<string, ItineraryItem[]>) => void) => {
    if (dates.length === 0) {
      callback({});
      return () => {};
    }

    const unsubscribes: (() => void)[] = [];
    const itemsByDate: Record<string, ItineraryItem[]> = {};
    let initializedDates = 0;

    dates.forEach((date) => {
      const unsub = tripService.observeItems(tripId, date, (items) => {
        itemsByDate[date] = items;
        if (unsubscribes.length === dates.length) {
          callback({ ...itemsByDate });
        }
      });
      unsubscribes.push(unsub);
    });

    return () => unsubscribes.forEach((u) => u());
  },

  renameAttachment: async (tripId: string, date: string, itemId: string, attachmentId: string, newName: string) => {
    const itemRef = doc(db, TRIPS_COLLECTION, tripId, "days", date, "items", itemId);
    const itemSnapshot = await getDocs(collection(db, TRIPS_COLLECTION, tripId, "days", date, "items"));
    const itemDoc = itemSnapshot.docs.find(d => d.id === itemId);
    if (!itemDoc) return;

    const item = itemDoc.data() as ItineraryItem;
    const updatedAttachments = item.attachments.map(att =>
      att.id === attachmentId ? { ...att, fileName: newName } : att
    );
    await updateDoc(itemRef, { attachments: updatedAttachments });
  },

  inviteByEmail: async (
    tripId: string,
    email: string,
    role: Role,
    invitedBy: string,
    existingUid?: string | null
  ) => {
    if (existingUid) {
      await tripService.setMemberRole(tripId, existingUid, role);
    } else {
      const normalizedEmail = email.trim().toLowerCase();
      const inviteRef = doc(db, TRIPS_COLLECTION, tripId, "pendingInvites", normalizedEmail);
      await setDoc(inviteRef, {
        email: normalizedEmail,
        role,
        invitedBy,
        invitedAt: serverTimestamp(),
      });
    }
  },

  observePendingInvites: (tripId: string, callback: (invites: { email: string, role: string }[]) => void) => {
    const q = collection(db, TRIPS_COLLECTION, tripId, "pendingInvites");
    return onSnapshot(q, (snapshot) => {
      const invites = snapshot.docs.map((doc) => ({
        email: doc.data().email,
        role: doc.data().role || Role.VIEWER,
      }));
      callback(invites);
    });
  },
};

function calculateSortMinutes(item: ItineraryItem): number {
  if (item.timeType === "EXACT" || item.timeType === "RANGE") {
    return item.startTime ? toMinutes(item.startTime) : item.order + 10000;
  }
  if (item.timeType === "PERIOD") {
    switch (item.period) {
      case "MORNING": return 6 * 60;
      case "NOON": return 12 * 60;
      case "AFTERNOON": return 14 * 60;
      case "EVENING": return 18 * 60;
      case "NIGHT": return 21 * 60;
      default: return item.order + 10000;
    }
  }
  return item.order + 10000;
}

function toMinutes(hhmm: string): number {
  const [h, m] = hhmm.split(":").map(Number);
  return (h || 0) * 60 + (m || 0);
}
