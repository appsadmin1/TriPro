import { FieldValue } from "firebase-admin/firestore";
import { getAdminFirestore, getAdminMessaging } from "./firebaseAdmin.mjs";

async function tokenOwnerPairs(uids) {
  if (uids.length === 0) return [];
  const db = getAdminFirestore();
  const refs = uids.map((uid) => db.collection("users").doc(uid));
  const snaps = await db.getAll(...refs);
  const pairs = [];
  for (const snap of snaps) {
    const tokens = snap.data()?.fcmTokens || [];
    for (const token of tokens) pairs.push({ uid: snap.id, token });
  }
  return pairs;
}

/** Drops uids who've turned this notification type off in Settings. Missing the field
 *  entirely defaults to enabled, matching the app's own default. */
export async function filterByPreference(uids, prefKey) {
  if (uids.length === 0) return [];
  const db = getAdminFirestore();
  const refs = uids.map((uid) => db.collection("users").doc(uid));
  const snaps = await db.getAll(...refs);
  return snaps
    .filter((snap) => {
      const prefs = snap.data()?.notificationPrefs;
      if (!prefs || !(prefKey in prefs)) return true;
      return prefs[prefKey] !== false;
    })
    .map((snap) => snap.id);
}

export async function sendToUsers(uids, notification, data) {
  const pairs = await tokenOwnerPairs(uids);
  if (pairs.length === 0) return;

  const response = await getAdminMessaging().sendEachForMulticast({
    tokens: pairs.map((p) => p.token),
    notification,
    data,
    android: { priority: "high" },
  });

  const staleTokensByUid = {};
  response.responses.forEach((result, i) => {
    if (result.success) return;
    const code = result.error?.code;
    if (code === "messaging/registration-token-not-registered" || code === "messaging/invalid-registration-token") {
      const { uid, token } = pairs[i];
      (staleTokensByUid[uid] ||= []).push(token);
    }
  });

  const db = getAdminFirestore();
  await Promise.all(
    Object.entries(staleTokensByUid).map(([uid, tokens]) =>
      db.collection("users").doc(uid).update({ fcmTokens: FieldValue.arrayRemove(...tokens) })
    )
  );
}