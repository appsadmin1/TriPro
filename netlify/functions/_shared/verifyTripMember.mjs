import { getAdminAuth, getAdminFirestore } from "./firebaseAdmin.mjs";

/**
 * Every function in this backend needs the same guarantee: never trust a uid or role
 * sent in a request body — always verify the caller's own identity from their Firebase
 * ID token, then check Firestore for whether *that* uid is actually allowed to act on
 * the trip in question.
 *
 * Returns { uid, trip } on success, or throws an Error with a `status` property set
 * (401 for a bad/missing token, 403 for "you're not a member of this trip", 404 if the
 * trip doesn't exist) that the caller should turn into an HTTP response.
 */
export async function verifyCallerIsTripMember(request, tripId) {
  const authHeader = request.headers.get("authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
  if (!idToken) {
    throw httpError(401, "Missing Authorization: Bearer <idToken> header");
  }

  let uid;
  try {
    const decoded = await getAdminAuth().verifyIdToken(idToken);
    uid = decoded.uid;
  } catch {
    throw httpError(401, "Invalid or expired ID token");
  }

  const tripSnap = await getAdminFirestore().collection("trips").doc(tripId).get();
  if (!tripSnap.exists) {
    throw httpError(404, "Trip not found");
  }
  const trip = { id: tripSnap.id, ...tripSnap.data() };

  if (!trip.members || !(uid in trip.members)) {
    throw httpError(403, "You're not a member of this trip");
  }

  return { uid, trip };
}

/**
 * Lighter-weight check for endpoints that aren't scoped to a specific trip (e.g.
 * flight-lookup.mjs) — just confirms the caller has a valid Firebase ID token, without
 * looking up trip membership. Returns the caller's uid.
 */
export async function verifyCallerIsSignedIn(request) {
  const authHeader = request.headers.get("authorization") || "";
  const idToken = authHeader.startsWith("Bearer ") ? authHeader.slice(7) : null;
  if (!idToken) {
    throw httpError(401, "Missing Authorization: Bearer <idToken> header");
  }
  try {
    const decoded = await getAdminAuth().verifyIdToken(idToken);
    return decoded.uid;
  } catch {
    throw httpError(401, "Invalid or expired ID token");
  }
}

function httpError(status, message) {
  const error = new Error(message);
  error.status = status;
  return error;
}
