import { FieldValue } from "firebase-admin/firestore";
import { getAdminFirestore } from "./_shared/firebaseAdmin.mjs";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";
import { deleteFromCloudinary } from "./_shared/cloudinary.mjs";

/**
 * POST /.netlify/functions/delete-attachment
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { tripId, date, itemId, attachmentId }
 *
 * This function handles both deleting the actual file from Cloudinary and removing
 * the reference from the Firestore itinerary item.
 */
export default async (request) => {
  if (request.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  let payload;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { tripId, date, itemId, attachmentId } = payload;
  if (!tripId || !date || !itemId || !attachmentId) {
    return Response.json({ error: "Missing required fields (tripId, date, itemId, attachmentId)" }, { status: 400 });
  }

  try {
    await verifyCallerIsTripMember(request, tripId);

    const db = getAdminFirestore();
    const dayRef = db.collection("trips").doc(tripId).collection("days").doc(date);
    const itemRef = dayRef.collection("items").doc(itemId);

    const itemSnap = await itemRef.get();
    if (!itemSnap.exists) return Response.json({ error: "Itinerary item not found" }, { status: 404 });
    const itemData = itemSnap.data();

    const attachment = (itemData.attachments || []).find(a => a.id === attachmentId);
    if (!attachment) return Response.json({ ok: true, message: "Attachment already removed" });

    // 1. Delete from Cloudinary
    await deleteFromCloudinary(attachment.publicId, attachment.resourceType);

    // 2. Remove from Firestore array
    await itemRef.update({
      attachments: FieldValue.arrayRemove(attachment)
    });

    return Response.json({ ok: true });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("delete-attachment function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};
