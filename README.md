# TriPro

A collaborative trip-planning Android app: create a vacation, invite people with
view/edit permissions, and plan it together in real time — day-by-day schedules, hotel
and flight info, attachments you can preview in-app, weather, a map of everything
you've planned, and push notifications when something changes.

Visual design is a direct port of the **Horizon Ethos** design system (`DESIGN.md` /
the four `code.html` mockups) onto Jetpack Compose + Material 3 — see
[`docs/DESIGN_MAPPING.md`](docs/DESIGN_MAPPING.md) for exactly how each token maps.

**The Firebase project for this app never needs to leave the free Spark plan** — see
Architecture below for how attachments and push notifications are handled without it.

---

## What's implemented

- **Google Sign-In** via Credential Manager (the current, non-deprecated API)
- **Trips list** — upcoming / past, with a "days away" countdown
- **Trip overview** — hero image, quick stats, collaborator avatars, day-by-day list
- **Day detail** — hotel ("base camp") card, flight card, weather, map of the day's
  pins, and a schedule of itinerary items
- **Itinerary items** with three ways to specify timing (exact hour, a range, or a
  time-of-day like "Morning"/"Evening"), a location + map pin, a free-text note/alert
  (e.g. "18+ only", "closes early at 18:00 today"), and file attachments
- **Attachments on Cloudinary**, viewable in-app — zoomable images, page-by-page PDF
  rendering (Android's built-in `PdfRenderer`, no third-party library), and an "Open
  with…" fallback for anything else — plus a one-tap download to the phone's Downloads
  folder, and real server-side deletion when you remove one
- **Real-time collaborative sync** — every screen uses Firestore snapshot listeners, so
  a change on one device appears on every other device immediately, no refresh needed
- **Push notifications** — a small Netlify Functions backend notifies the rest of a
  trip's collaborators when someone's invited, or when the itinerary/hotel/flight/notes
  change; tapping a notification deep-links straight to the relevant trip or day
- **Collaborators screen** — invite by email, assign Editor/Read-only, remove members;
  inviting someone who hasn't signed up yet queues automatically and resolves the first
  time they do
- **Weather** — free, keyless forecast; shows a "check back closer to the date" message
  for days beyond the 16-day forecast horizon instead of guessing
- **Google Maps** — pins for hotel + every itinerary item with coordinates

## What's *not* implemented (see [Next steps](#next-steps))

- Recursive delete of a trip's subcollections when the trip itself is deleted
- Places Autocomplete for location search (items use a tap-to-pin map instead)
- Automated tests, including for `firestore.rules`

---

## Architecture

| Concern | Choice | Why |
|---|---|---|
| Auth | **Firebase Authentication** + Credential Manager (Google Sign-In) | Free at any scale (Spark plan); Credential Manager is Google's current recommended API — `GoogleSignInClient` is deprecated. |
| Database | **Cloud Firestore** | Realtime listeners map directly onto "collaborative sync" with almost no extra code, plus built-in offline persistence. Free (Spark) tier: **1 GiB storage, 50K reads / 20K writes / 20K deletes per day**. |
| File storage | **Cloudinary** | See the comparison below. |
| Weather | **Open-Meteo** (`api.open-meteo.com`) | Free, keyless, no signup, non-commercial use up to 10,000 calls/day. 16-day forecast + up to 92 days of history. |
| Maps | **Google Maps SDK for Android** (Maps Compose) | Mobile-native Dynamic Maps usage is currently unmetered for standard mobile app usage, though you still need a billing-enabled Cloud project and API key. Verify current terms at mapsplatform.google.com/pricing before shipping — Google has changed this more than once. |
| Collaborator invites | Firestore `pendingInvites` sub-collection + client-side reconciliation on login | Lets "invite someone who doesn't have an account yet" work without a backend call. |
| Push notifications + attachment cleanup | **Netlify Functions** (not Firebase Cloud Functions) | Keeps the Firebase project on Spark — see below. |

### Cloudinary — and why it's a better fit here than Uploadcare was

Good instinct switching to this: checked the current numbers directly against
Cloudinary's own pricing/docs pages, and for this app it's a genuinely better fit than
Uploadcare, not just a lateral move —

- **No credit card at all**, for anything — Cloudinary's own signup docs confirm this
  explicitly. Uploadcare required one for KYC the moment you upload a non-image file
  (which is most of what this app needs — PDF tickets and confirmations).
- **No overage billing.** The Free plan runs on a 25-credits/month pool (1 credit = 1GB
  storage, 1GB bandwidth, or 1,000 transformations — your choice how to split it) on a
  rolling 30-day window. Cross the limit and Cloudinary warns you and eventually pauses
  the account; it does not silently bill a card, because there's no card on file to bill.
- **PDFs are a first-class, well-documented use case** — Cloudinary can store them,
  rasterize individual pages, and serve them, with no add-on or KYC step. There's one
  real setup gotcha, though (see Setup step 1 below): free accounts have PDF/ZIP
  *delivery* switched off by default for security reasons, so you have to flip one
  toggle in Console Settings or your uploaded PDFs will 403 when the app tries to view them.

Same security constraint as before, just a different vendor: uploads use an **unsigned
upload preset** (safe to embed in the app, by design — Cloudinary's own client-side
uploading docs recommend exactly this pattern), while *deleting* a file requires the
API Secret, which must never ship in an APK. `CloudinaryRepository` only uploads;
`netlify/functions/delete-attachment.mjs` handles real deletion server-side when you
remove an attachment in the app.

### Why Netlify Functions instead of Firebase Cloud Functions

The previous version of this backend used Firebase Cloud Functions, which **requires
the Blaze (pay-as-you-go) plan to deploy at all**, regardless of usage — that's a hard
requirement from Firebase, not a workaround. If you'd rather never put a card on a
Firebase project (a completely reasonable position — "probably won't be charged" is a
different thing than "can't be charged"), Netlify Functions do the identical job for
$0 with no billing plan upgrade anywhere:

- Firestore and Firebase Auth are usable via the **Admin SDK from any Node process**,
  not just Cloud Functions — Blaze is specifically required to *host compute on
  Firebase itself*, not to access Firestore/Auth from an external service. A Netlify
  Function with a service account key does the exact same reads/writes/token
  verification a Cloud Function would, just hosted elsewhere.
- Netlify's own Functions free tier: 125,000 requests/month and 100 hours of run time
  — enormous headroom for a personal/small-group trip app.

**The trade-off, honestly:** Cloud Functions are *Firestore-triggered* — they fire from
the database write itself, no matter what happens to the client afterward. Netlify
Functions are *HTTP-triggered*, so the Android app has to explicitly call the
`/notify` endpoint right after each relevant write succeeds (see the call sites in
`CollaboratorsViewModel` and `DayDetailViewModel`). In practice this means: if the
phone loses its connection in the split second between the Firestore write succeeding
and the notification HTTP call going out, that one notification silently doesn't fire.
The underlying data is never at risk (Firestore already has it), just that specific
push. Acceptable for a "nice to have" notification; worth knowing.

### Data model

```
users/{uid}                                  { email, displayName, photoUrl,
                                                fcmTokens: [token, ...] }

trips/{tripId}                               { name, destination, coverImageUrl,
                                                startDate, endDate, ownerId,
                                                members: {uid: "owner"|"editor"|"viewer"},
                                                memberIds: [uid, ...] }
trips/{tripId}/days/{yyyy-MM-dd}             { dayIndex, hotel, flight, dayNote,
                                                updatedBy }
trips/{tripId}/days/{date}/items/{itemId}    { title, type, timeType, startTime,
                                                endTime, period, locationName, address,
                                                lat, lng, note, attachments[], order,
                                                createdBy, updatedBy }
trips/{tripId}/pendingInvites/{email}        { role, invitedBy, invitedAt }
```

`members` is the single source of truth for permissions, enforced in
`firestore.rules`. `updatedBy` on days/items lets the notify endpoint exclude the
person who just made a change from their own "this changed" notification.

---

## Setup

You'll need: Android Studio (current stable), a Google account, a free Cloudinary
account, a free Netlify account, and ~25 minutes. **No credit card, anywhere, for any
of this.**

### 1. Set up Cloudinary

1. Create a free account at cloudinary.com (no card required).
2. Dashboard home page → copy your **Cloud name**.
3. Settings (gear icon) → **Upload** → **Upload presets** → **Add upload preset**.
   Set **Signing Mode** to **Unsigned**, save, and note the preset's name.
4. **Important:** Settings → **Security** → find **"PDF and ZIP files delivery"** →
   enable it. Free accounts block delivery of these formats by default (an
   anti-malware measure) — skip this and every PDF attachment will upload fine but fail
   to load when someone tries to view or download it.
5. Settings → **API Keys** → note your **API Key** and **API Secret** (the secret is
   only used later, server-side, in Netlify's environment variables — never in the app).

### 2. Create the Firebase project

1. Firebase console (console.firebase.google.com) → **Add project**. Stay on the
   **Spark (free) plan** — nothing in this app needs Blaze.
2. **Build → Authentication → Sign-in method** → enable **Google**.
3. **Build → Firestore Database → Create database** (production mode — the rules in
   this repo replace the default-deny ruleset).
4. **Project settings → General → Add app → Android**. Package name `com.tripro.app`.
   Download **`google-services.json`** → place at `app/google-services.json`
   (gitignored — every developer/environment provides their own).
5. **Project settings → Service accounts → Generate new private key.** Downloads a JSON
   file — you'll copy three fields out of it in step 4 below. Keep this file itself out
   of git; you only need values from it, not the file.
6. Install the Firebase CLI and deploy the Firestore rules:
   ```
   npm install -g firebase-tools
   firebase login
   firebase use --add        # pick the project you just created
   firebase deploy --only firestore:rules,firestore:indexes
   ```

### 3. Deploy the Netlify Functions backend

1. Create a free Netlify account, then a new site from the `netlify/` folder of this
   repo (Netlify CLI: `npm install -g netlify-cli && netlify init`, or connect the repo
   via the Netlify dashboard and set the base directory to the repo root — `netlify.toml`
   already points at `netlify/functions`).
2. Site settings → **Environment variables** → add:
   - `FIREBASE_PROJECT_ID` — from the service account JSON's `project_id`
   - `FIREBASE_CLIENT_EMAIL` — from the JSON's `client_email`
   - `FIREBASE_PRIVATE_KEY` — from the JSON's `private_key` (paste it as-is, including
     the `-----BEGIN PRIVATE KEY-----`/`-----END PRIVATE KEY-----` lines)
   - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` — from
     Cloudinary Setup step 1 above
3. Deploy: `netlify deploy --prod` (or push to the connected Git branch).
4. Note your site's URL, e.g. `https://tripro-notifications.netlify.app` — you'll need
   it in step 5.

### 4. Get your Web Client ID (for Google Sign-In)

**Firebase console → Authentication → Sign-in method → Google → Web SDK
configuration** — copy the **Web client ID** (`....apps.googleusercontent.com`). This
is *not* the Android OAuth client ID; Credential Manager's `GetSignInWithGoogleOption`
specifically needs the Web one.

### 5. Get a Maps API key

1. In Google Cloud Console (same project Firebase created), enable the **Maps SDK for
   Android** API.
2. **APIs & Services → Credentials → Create credentials → API key.**
3. Restrict it to *Android apps* / *Maps SDK for Android* once you have a release SHA-1
   fingerprint (optional locally, recommended before shipping).

### 6. Wire up local.properties

Copy `local.properties.example`'s contents into your own `local.properties` (Android
Studio creates this automatically; it's already gitignored):

```properties
MAPS_API_KEY=AIza...your key
WEB_CLIENT_ID=123...-abc.apps.googleusercontent.com
CLOUDINARY_CLOUD_NAME=your_cloud_name
CLOUDINARY_UPLOAD_PRESET=your_unsigned_preset_name
NETLIFY_FUNCTIONS_BASE_URL=https://your-site.netlify.app
```

### 7. Open and run

Open the project root in Android Studio, let it sync (accept any AGP/Kotlin/library
version bumps it offers), generate a launcher icon via **Image Asset Studio** if you
want a real one (a placeholder vector icon is included so it builds without it), and run.

The first time you sign in, the app requests notification permission (API 33+) —
accept it to receive push notifications.

---

## Project structure

```
app/src/main/java/com/tripro/app/
  data/model/          Trip, TripDay, ItineraryItem, Attachment, Role, DailyWeather, ...
  data/repository/     AuthRepository, TripRepository, UserRepository,
                        CloudinaryRepository, WeatherRepository, PushNotificationRepository
  data/remote/         OpenMeteoClient
  notifications/       TriProMessagingService (FCM), NotificationHelper (channel + deep link)
  ui/theme/            Color.kt, Type.kt, Shape.kt, Spacing.kt, Theme.kt — the
                        Horizon Ethos design system as Compose/M3
  ui/auth/             Login screen + AuthViewModel
  ui/triplist/         Trips list, create-trip flow
  ui/tripoverview/     Trip overview (hero, stats, day list)
  ui/daydetail/        Day detail screen, add/edit item sheet
  ui/collaborators/    Invite / permissions management
  ui/components/       Shared pieces: TripCard, ItineraryItemRow, WeatherCard,
                        DayMapPreview, AvatarStack, AttachmentViewerDialog
  navigation/          NavGraph, route definitions, PendingDeepLink (notification taps)
  util/                DateUtils, WeatherCodeMapper, FileDownloader, PdfPageRenderer
  AppContainer.kt      Manual DI — see note below
netlify/functions/     Push notifications + Cloudinary cleanup backend (Node.js, ESM)
  _shared/             Firebase Admin init, ID-token+membership verification, FCM sending
  notify.mjs           POST endpoint: trip invites, itinerary changes, day info changes
  delete-attachment.mjs  POST endpoint: deletes a Cloudinary asset by public_id
firestore.rules        Security rules (read this — it's commented with the reasoning)
firebase.json / firestore.indexes.json
netlify.toml
```

**Dependency injection:** this uses a small hand-written `AppContainer` instead of
Hilt/Koin, specifically so there's zero annotation-processing configuration to get
wrong on your very first build.

---

## Next steps

1. **Recursive delete on trip deletion.** `TripRepository.deleteTrip` only deletes the
   trip document — Firestore doesn't cascade-delete subcollections client-side.
2. **Places Autocomplete** for location search instead of tap-to-pin, if the extra
   Places API billing surface is acceptable for your use case.
3. **Notification preferences.** Right now every trip change notifies every other
   member with no way to opt out of specific trips or mute a noisy one.
4. **Retry/queue for the notify call.** Since notifications are client-triggered (see
   Architecture), a failed HTTP call currently just gets swallowed. A lightweight retry
   (e.g. WorkManager) would close that gap if it matters for your use case.
5. **Tests.** Nothing here has automated coverage yet; `firestore.rules` in particular
   is exactly the kind of thing you want covered by the Firestore emulator's
   rules-unit-testing library before depending on it for real user data.
