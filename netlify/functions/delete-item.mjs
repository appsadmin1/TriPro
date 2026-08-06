import { getAdminFirestore } from "./_shared/firebaseAdmin.mjs";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";
import { deleteFromCloudinary } from "./_shared/cloudinary.mjs";
import { sendToUsers, filterByPreference } from "./_shared/notifications.mjs";

/**
 * POST /.netlify/functions/delete-item
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { tripId, date, itemId }
 */
export default async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { tripId, date, itemId } = payload;
  if (!tripId || !date || !itemId) {
    return Response.json({ error: "Missing required fields (tripId, date, itemId)" }, { status: 400 });
  }

  try {
    const { uid: callerUid, trip } = await verifyCallerIsTripMember(request, tripId);

    // Check permission: Owner or Editor
    const role = trip.members[callerUid];
    if (role !== "owner" && role !== "editor") {
      return Response.json({ error: "Insufficient permissions to delete item" }, { status: 403 });
    }

    const db = getAdminFirestore();
    const itemRef = db.collection("trips").doc(tripId).collection("days").doc(date).collection("items").doc(itemId);
    const itemSnap = await itemRef.get();

    if (!itemSnap.exists) {
      return Response.json({ error: "Item not found" }, { status: 404 });
    }

    const itemData = itemSnap.data();

    // 1. Delete all attachments from Cloudinary
    if (itemData.attachments && Array.isArray(itemData.attachments)) {
      await Promise.all(itemData.attachments.map(a => deleteFromCloudinary(a.publicId, a.resourceType)));
    }

    // 2. Delete the item from Firestore
    await itemRef.delete();

    // 3. Notify collaborators
    const others = Object.keys(trip.members || {}).filter(uid => uid !== callerUid);
    const recipients = await filterByPreference(others, "itineraryChanges");
    if (recipients.length > 0) {
      await sendToUsers(recipients,
        { title: trip.name, body: `${itemData.title || "An item"} was removed from ${date}` },
        { tripId, date, type: "itinerary_update" }
      );
    }

    return Response.json({ ok: true });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("delete-item function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};
