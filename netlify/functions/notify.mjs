import { sendToUsers } from "./_shared/notifications.mjs";
import { verifyCallerIsTripMember } from "./_shared/verifyTripMember.mjs";

/**
 * POST /.netlify/functions/notify
 * Headers: Authorization: Bearer <Firebase ID token>
 * Body: { type: "trip_invite" | "itinerary_update" | "day_update", tripId, ...fields }
 *
 * Replaces the Firestore-triggered Cloud Functions from the earlier version of this
 * backend. Since Netlify Functions aren't Firestore-event-triggered, the Android app
 * calls this explicitly right after each relevant write succeeds — see
 * PushNotificationRepository.kt and its call sites in CollaboratorsViewModel /
 * DayDetailViewModel.
 *
 * Trade-off worth knowing: because this is client-triggered rather than
 * database-triggered, a notification will silently not fire if the app's HTTP call
 * fails (e.g. the phone loses signal right after saving) — unlike a Firestore trigger,
 * which fires from the database write itself regardless of what happens to the client
 * afterward. Acceptable for a "nice to have" notification; worth knowing if you're
 * depending on this for something more critical.
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

  const { type, tripId } = payload;
  if (!type || !tripId) {
    return Response.json({ error: "Missing type or tripId" }, { status: 400 });
  }

  try {
    const { uid: callerUid, trip } = await verifyCallerIsTripMember(request, tripId);

    switch (type) {
      case "trip_invite": {
        const { invitedUid } = payload;
        if (!invitedUid || !(invitedUid in (trip.members || {}))) {
          return Response.json({ error: "invitedUid is not a member of this trip" }, { status: 400 });
        }
        await sendToUsers(
          [invitedUid],
          { title: "You're invited!", body: `You've been added to "${trip.name}"` },
          { tripId, type: "trip_invite" }
        );
        break;
      }

      case "itinerary_update": {
        const { date, itemTitle, action } = payload;
        if (!date) return Response.json({ error: "Missing date" }, { status: 400 });
        const recipients = Object.keys(trip.members || {}).filter((uid) => uid !== callerUid);
        await sendToUsers(
          recipients,
          { title: trip.name, body: `${itemTitle || "An item"} was ${action || "updated"} on ${date}` },
          { tripId, date, type: "itinerary_update" }
        );
        break;
      }

      case "day_update": {
        const { date, what } = payload;
        if (!date) return Response.json({ error: "Missing date" }, { status: 400 });
        const recipients = Object.keys(trip.members || {}).filter((uid) => uid !== callerUid);
        await sendToUsers(
          recipients,
          { title: trip.name, body: `${what || "Trip info"} was updated for ${date}` },
          { tripId, date, type: "day_update" }
        );
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
