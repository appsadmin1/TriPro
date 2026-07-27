import { FieldValue } from "firebase-admin/firestore";
import { getAdminFirestore, getAdminMessaging } from "./firebaseAdmin.mjs";

/** Flattens {uid: [token, ...]} for every uid in [uids] into [{uid, token}, ...] pairs. */
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

/**
 * Sends the same notification+data payload to every device belonging to [uids] and
 * cleans up any tokens FCM reports as dead so they stop being tried on every future
 * send. Uses sendEachForMulticast — the current (non-deprecated) batch-send method as
 * of firebase-admin v12+. If you're reading this much later, double-check the Admin SDK
 * release notes before assuming that's still true; Firebase has iterated on this API
 * more than once.
 */
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
    if (
      code === "messaging/registration-token-not-registered" ||
      code === "messaging/invalid-registration-token"
    ) {
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
