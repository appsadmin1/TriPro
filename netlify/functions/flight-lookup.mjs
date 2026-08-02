import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";

/**
 * GET /.netlify/functions/flight-lookup?tripId=...&flightNumber=LH441&date=2026-08-01
 * Looks up a flight by number via AeroDataBox (RapidAPI). Field names below match
 * AeroDataBox's "Flights by flight number" endpoint as of writing — double check
 * against your own RapidAPI sandbox response before relying on this, since third-party
 * schemas do shift over time.
 */
export default async (request) => {
  if (request.method !== "GET") return new Response("Method not allowed", { status: 405 });

  const url = new URL(request.url);
  const tripId = url.searchParams.get("tripId");
  const flightNumber = url.searchParams.get("flightNumber")?.trim().toUpperCase();
  const date = url.searchParams.get("date"); // yyyy-MM-dd, optional

  if (!tripId || !flightNumber) {
    return Response.json({ error: "Missing tripId or flightNumber" }, { status: 400 });
  }

  try {
    await verifyCallerIsTripMember(request, tripId); // membership check only, no trip data needed here

    const apiKey = process.env.AERODATABOX_RAPIDAPI_KEY;
    if (!apiKey) throw Object.assign(new Error("AERODATABOX_RAPIDAPI_KEY not configured"), { status: 500 });

    const path = date
      ? `/flights/number/${flightNumber}/${date}`
      : `/flights/number/${flightNumber}`;

    const response = await fetch(`https://aerodatabox.p.rapidapi.com${path}`, {
      headers: {
        "X-RapidAPI-Key": apiKey,
        "X-RapidAPI-Host": "aerodatabox.p.rapidapi.com",
      },
    });

    if (!response.ok) {
      return Response.json({ error: `Flight lookup failed (${response.status})` }, { status: response.status === 404 ? 404 : 502 });
    }

    const results = await response.json();
    const flight = Array.isArray(results) ? results[0] : results;
    if (!flight) return Response.json({ error: "Flight not found" }, { status: 404 });

    return Response.json({
      airline: flight.airline?.name ?? "",
      flightNumber: flight.number ?? flightNumber,
      departureAirportCode: flight.departure?.airport?.iata ?? "",
      arrivalAirportCode: flight.arrival?.airport?.iata ?? "",
      departureAirportLat: flight.departure?.airport?.location?.lat ?? null,
      departureAirportLng: flight.departure?.airport?.location?.lon ?? null,
      arrivalAirportLat: flight.arrival?.airport?.location?.lat ?? null,
      arrivalAirportLng: flight.arrival?.airport?.location?.lon ?? null,
      departureTime: (flight.departure?.scheduledTime?.local ?? "").slice(11, 16),
      arrivalTime: (flight.arrival?.scheduledTime?.local ?? "").slice(11, 16),
    });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("flight-lookup function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};