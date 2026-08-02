import { verifyCallerIsSignedIn } from "./_shared/verifyTripMember.mjs";

/**
 * POST /.netlify/functions/flight-lookup
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { flightNumber, date }  // date: yyyy-MM-dd
 *
 * Proxies AeroDataBox's "flight number" lookup (via RapidAPI) for the Flight edit
 * dialog's "Look up flight" button — this is what keeps the RapidAPI key out of the
 * APK, same reasoning as delete-attachment.mjs and the Cloudinary API Secret. The
 * lookup itself is generic public flight-schedule data, not scoped to one trip, so this
 * only checks that the caller is a signed-in TriPro user (verifyCallerIsSignedIn),
 * rather than trip membership like verifyCallerIsTripMember.
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

  const { flightNumber, date } = payload;
  if (!flightNumber || !date) {
    return Response.json({ error: "Missing flightNumber or date" }, { status: 400 });
  }

  try {
    await verifyCallerIsSignedIn(request);

    const apiKey = process.env.AERODATABOX_RAPIDAPI_KEY;
    if (!apiKey) {
      throw Object.assign(new Error("AERODATABOX_RAPIDAPI_KEY not configured"), { status: 500 });
    }

    const normalizedFlightNumber = flightNumber.replace(/\s+/g, "").toUpperCase();
    const url =
      `https://aerodatabox.p.rapidapi.com/flights/number/${normalizedFlightNumber}/${date}` +
      "?withAircraftImage=false&withLocation=false&withFlightPlan=false&dateLocalRole=Both";

    const aeroResponse = await fetch(url, {
      headers: {
        "x-rapidapi-key": apiKey,
        "x-rapidapi-host": "aerodatabox.p.rapidapi.com",
      },
    });

    if (!aeroResponse.ok) {
      const text = await aeroResponse.text().catch(() => "");
      throw Object.assign(
        new Error(`AeroDataBox lookup failed: ${aeroResponse.status} ${text}`),
        { status: 502 }
      );
    }

    const flights = await aeroResponse.json();
    if (!Array.isArray(flights) || flights.length === 0) {
      return Response.json({ error: "No flight found for that number and date" }, { status: 404 });
    }

    // A designator+date can have multiple legs/codeshares; AeroDataBox's first result is
    // its own best match, which is plenty for an itinerary autofill.
    return Response.json({ flight: flights[0] });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("flight-lookup function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};