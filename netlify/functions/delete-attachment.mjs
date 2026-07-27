import crypto from "node:crypto";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";

/**
 * POST /.netlify/functions/delete-attachment
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { tripId, publicId, resourceType }
 *
 * The Android app already detaches the attachment from Firestore itself (instant, no
 * round-trip needed for that part) — this function's only job is deleting the actual
 * file from Cloudinary, which requires signing the request with the API Secret. That
 * secret lives only in this Netlify environment's variables, never in the app or the repo.
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

  const { tripId, publicId, resourceType } = payload;
  if (!tripId || !publicId) {
    return Response.json({ error: "Missing tripId or publicId" }, { status: 400 });
  }

  try {
    // Membership check only — anyone on the trip may clean up an attachment they can
    // already see. If you want to restrict this to editors/owner only, that role is
    // available at `trip.members[uid]` from verifyCallerIsTripMember's return value.
    await verifyCallerIsTripMember(request, tripId);

    const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
    const apiKey = process.env.CLOUDINARY_API_KEY;
    const apiSecret = process.env.CLOUDINARY_API_SECRET;
    if (!cloudName || !apiKey || !apiSecret) {
      throw Object.assign(new Error("Cloudinary server credentials not configured"), { status: 500 });
    }

    const timestamp = Math.floor(Date.now() / 1000);
    // Cloudinary's signing rule: sort every signable param alphabetically, join as
    // "key=value&key=value", append the API secret directly (no separator), then hash.
    // resource_type/cloud_name/api_key are deliberately excluded from the signature.
    const toSign = `public_id=${publicId}&timestamp=${timestamp}${apiSecret}`;
    const signature = crypto.createHash("sha1").update(toSign).digest("hex");

    const body = new URLSearchParams({
      public_id: publicId,
      timestamp: String(timestamp),
      api_key: apiKey,
      signature,
    });

    const destroyUrl = `https://api.cloudinary.com/v1_1/${cloudName}/${resourceType || "image"}/destroy`;
    const cloudinaryResponse = await fetch(destroyUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
    });
    const result = await cloudinaryResponse.json();

    // "not found" means the goal state (file doesn't exist) is already true — treat as success.
    if (result.result !== "ok" && result.result !== "not found") {
      throw Object.assign(new Error(`Cloudinary destroy failed: ${JSON.stringify(result)}`), { status: 502 });
    }

    return Response.json({ ok: true });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("delete-attachment function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};
