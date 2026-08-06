import { getAdminFirestore } from "./_shared/firebaseAdmin.mjs";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";
import { deleteFromCloudinary } from "./_shared/cloudinary.mjs";

/**
 * POST /.netlify/functions/delete-trip
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { tripId }
 *
 * Deletes a trip, all its subcollections (recursively), and all associated
 * Cloudinary attachments.
 */
export default async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { tripId } = payload;
  if (!tripId) {
    return Response.json({ error: "Missing required field (tripId)" }, { status: 400 });
  }

  try {
    const { uid: callerUid, trip } = await verifyCallerIsTripMember(request, tripId);

    // Check permission: ONLY Owner can delete a trip
    if (trip.ownerId !== callerUid && trip.members[callerUid] !== "owner") {
      return Response.json({ error: "Only the trip owner can delete it" }, { status: 403 });
    }

    const db = getAdminFirestore();

    // 1. Find and delete all attachments from Cloudinary
    const attachments = [];

    // Add trip cover image if it exists
    if (trip.coverImagePublicId) {
      attachments.push({
        publicId: trip.coverImagePublicId,
        resourceType: trip.coverImageResourceType || "image"
      });
    }

    // We iterate through days -> items to find all attachments.
    const daysSnap = await db.collection("trips").doc(tripId).collection("days").get();

    for (const dayDoc of daysSnap.docs) {
      const itemsSnap = await dayDoc.ref.collection("items").get();
      for (const itemDoc of itemsSnap.docs) {
        const itemData = itemDoc.data();
        if (itemData.attachments && Array.isArray(itemData.attachments)) {
          attachments.push(...itemData.attachments);
        }
      }
    }

    if (attachments.length > 0) {
      // Parallel deletion
      await Promise.all(attachments.map(a => deleteFromCloudinary(a.publicId, a.resourceType)));
    }

    // 2. Recursive delete trip from Firestore
    // Note: recursiveDelete is available in firebase-admin SDK
    const tripRef = db.collection("trips").doc(tripId);
    await db.recursiveDelete(tripRef);

    return Response.json({ ok: true });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("delete-trip function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};
