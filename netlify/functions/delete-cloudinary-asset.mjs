import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";
import { deleteFromCloudinary } from "./_shared/cloudinary.mjs";

/**
 * POST /.netlify/functions/delete-cloudinary-asset
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { tripId, publicId, resourceType }
 *
 * Generic cleanup for Cloudinary assets (like trip cover images) after verifying
 * the caller is a member of the trip.
 */
export default async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { tripId, publicId, resourceType } = payload;
  if (!tripId || !publicId) {
    return Response.json({ error: "Missing tripId or publicId" }, { status: 400 });
  }

  try {
    await verifyCallerIsTripMember(request, tripId);
    const result = await deleteFromCloudinary(publicId, resourceType);
    return Response.json({ ok: true, result });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("delete-cloudinary-asset function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};
