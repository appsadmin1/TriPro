import crypto from "node:crypto";

/**
 * Deletes an asset from Cloudinary using the API Secret (server-side only).
 */
export async function deleteFromCloudinary(publicId, resourceType) {
  const cloudName = process.env.CLOUDINARY_CLOUD_NAME;
  const apiKey = process.env.CLOUDINARY_API_KEY;
  const apiSecret = process.env.CLOUDINARY_API_SECRET;

  if (!cloudName || !apiKey || !apiSecret) {
    console.warn("Cloudinary credentials not configured, skipping file deletion.");
    return { result: "skipped" };
  }

  const maskedKey = apiKey.length > 4 ? `...${apiKey.slice(-4)}` : apiKey;
  console.log(`Using Cloudinary API Key: ${maskedKey}`);
  console.log(`Attempting to delete ${publicId} (type: ${resourceType}) from Cloudinary...`);

  try {
    const timestamp = Math.floor(Date.now() / 1000);
    // alphabetical: public_id, timestamp
    const toSign = `public_id=${publicId}&timestamp=${timestamp}${apiSecret}`;
    const signature = crypto.createHash("sha1").update(toSign).digest("hex");

    const body = new URLSearchParams({
      public_id: publicId,
      timestamp: String(timestamp),
      api_key: apiKey,
      signature,
    });

    const destroyUrl = `https://api.cloudinary.com/v1_1/${cloudName}/${resourceType || "image"}/destroy`;
    console.log(`POST to ${destroyUrl}`);

    const response = await fetch(destroyUrl, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body,
    });

    const result = await response.json();
    console.log("Cloudinary response:", result);
    return result;
  } catch (error) {
    console.error(`Error deleting ${publicId} from Cloudinary:`, error);
    return { result: "error", error: error.message };
  }
}
