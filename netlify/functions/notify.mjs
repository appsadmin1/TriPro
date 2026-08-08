import { sendToUsers, filterByPreference } from "./_shared/notifications.mjs";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";

export default async (request) => {
  if (request.method !== "POST") return new Response("Method not allowed", { status: 405 });

  let payload;
  try {
    payload = await request.json();
  } catch {
    return Response.json({ error: "Invalid JSON body" }, { status: 400 });
  }

  const { type, tripId } = payload;
  if (!type || !tripId) return Response.json({ error: "Missing type or tripId" }, { status: 400 });

  try {
    const { uid: callerUid, trip } = await verifyCallerIsTripMember(request, tripId);

    switch (type) {
      case "trip_invite": {
        const { invitedUid } = payload;
        if (!invitedUid || !(invitedUid in (trip.members || {}))) {
          return Response.json({ error: "invitedUid is not a member of this trip" }, { status: 400 });
        }
        const recipients = await filterByPreference([invitedUid], "tripInvites");
        await sendToUsers(recipients, { title: "You're invited!", body: `You've been added to "${trip.name}"` }, { tripId, type: "trip_invite" });
        break;
      }
      case "itinerary_update": {
        const { date, itemTitle, action, actorName } = payload;
        if (!date) return Response.json({ error: "Missing date" }, { status: 400 });
        const others = Object.keys(trip.members || {}).filter((uid) => uid !== callerUid);
        const recipients = await filterByPreference(others, "itineraryChanges");
        await sendToUsers(recipients, { title: trip.name, body: `${actorName || "A traveler"} ${action || "updated"} "${itemTitle || "an item"}" on ${date}` }, { tripId, date, type: "itinerary_update" });
        break;
      }
      case "day_update": {
        const { date, what, actorName } = payload;
        if (!date) return Response.json({ error: "Missing date" }, { status: 400 });
        const others = Object.keys(trip.members || {}).filter((uid) => uid !== callerUid);
        const recipients = await filterByPreference(others, "dayInfoChanges");
        await sendToUsers(recipients, { title: trip.name, body: `${actorName || "A traveler"} updated ${what || "trip info"} for ${date}` }, { tripId, date, type: "day_update" });
        break;
      }
      case "trip_update": {
        const { what, actorName } = payload;
        const others = Object.keys(trip.members || {}).filter((uid) => uid !== callerUid);
        const recipients = await filterByPreference(others, "dayInfoChanges");
        await sendToUsers(recipients, { title: trip.name, body: `${actorName || "A traveler"} updated ${what || "trip details"}` }, { tripId, type: "trip_update" });
        break;
      }
      default:
        return Response.json({ error: `Unknown type: ${type}` }, { status: 400 });
    }
    return Response.json({ ok: true });
  } catch (error) {
    const status = error.status || 500;
    if (status === 500) console.error("notify function error:", error);
    return Response.json({ error: error.message || "Internal error" }, { status });
  }
};