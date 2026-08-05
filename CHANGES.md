# TriPro updates — how to apply

Every file in this archive keeps its original path from the repo root, so you can copy
it straight over the matching file in your project (or diff first if you've made local
changes since these were pasted into the chat). Nothing here was auto-applied to your
actual repo — this chat only had these files as pasted text, not real project files on
disk, so a fresh copy was the only way to hand them back to you.

## 1. Fonts + visual refresh
- `app/src/main/res/font/plus_jakarta_sans.ttf`, `work_sans.ttf`, `jetbrains_mono.ttf` —
  the real TriPro webfonts (pulled from the OFL google/fonts repo), replacing the
  `FontFamily.Default` placeholder. Each is a variable font, so one file per family
  covers every weight DESIGN.md calls for.
- `ui/theme/Type.kt` — wires those files in via `FontVariation` (real SemiBold/Bold/
  Medium instances, not synthetic/faux bold), and adds a `titleSmall` style used by the
  time-column fix below.
- `ui/components/ItineraryItemRow.kt` — the itinerary time column was 64dp wide at
  20sp, so "Afternoon"/"Evening"/etc. didn't fit on one line and got force-wrapped
  letter-by-letter (the "vertical text" bug). Widened to 80dp and given a dedicated
  smaller style for period labels only (exact times like "09:00" keep the bigger style).
- `ui/daydetail/AddEditItemSheet.kt` — the 5 "time of day" chips (Morning/Noon/
  Afternoon/Evening/Night) had no scroll container, so they got squeezed to fit and
  wrapped the same way. Wrapped in `horizontalScroll`, matching the item-type row above it.
- Added subtle card elevation (`ui/components/TripCard.kt`, `ItineraryItemRow.kt`,
  `WeatherCard.kt`, and the Hotel/Flight/DayNote cards in `ui/daydetail/DayDetailScreen.kt`)
  — these were all sitting at 0dp elevation, which read as flat.

## 2. Layout mirroring to the right ("start date on the right, end date on the left")
- `ui/theme/Theme.kt` — this was RTL layout mirroring, not a positioning bug in any one
  screen. Compose automatically flips layout direction (Row order, Arrangement.Start,
  TextAlign.Start, ...) to match an RTL device locale (Hebrew, Arabic, ...) *regardless*
  of the `android:supportsRtl` manifest flag — that flag only affects the old View
  system. TriPro has no RTL variant (DESIGN.md, icon directions, and the "Start
  date | End date" button order are all LTR-only, and there's no non-English string
  anywhere), so `TriProTheme` now wraps its content in
  `CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr)`. This
  fixes it everywhere at once rather than patching each screen's Row order individually.

## 3. Cover photo: device picker instead of a URL field
- `ui/triplist/CreateTripScreen.kt` — the "Cover image URL" text field is gone, replaced
  by a tappable photo box using Android's built-in Photo Picker
  (`ActivityResultContracts.PickVisualMedia`), which needs no storage permission on any
  API level.
- `ui/triplist/CreateTripViewModel.kt` — the picked photo is a `content://` Uri, which
  only resolves on the phone that picked it, so `createTrip` now uploads it through the
  same `CloudinaryRepository` every other attachment in the app already uses, and stores
  the resulting hosted URL — so every collaborator's device can actually load it.

## 4. Interactive location + time pickers
- `util/PlacesAutocomplete.kt` (new) — `rememberPlacePicker(...)` opens Google's
  full-screen Place Autocomplete search (search box + results, no map/pin-dropping
  needed) and returns name/address/lat-lng/place ID.
- `ui/daydetail/DayDetailScreen.kt`:
  - **Hotel dialog** — "Search for hotel on Google Maps" (filtered to `lodging`) fills
    name/address/lat/lng. This also fixes a pre-existing gap: the dialog never wrote
    `lat`/`lng` before, so the hotel pin on the day map never actually appeared.
  - **Flight dialog** — "Find departure/arrival airport" (filtered to `airport`) fills
    lat/lng for both ends (also previously never written). The 3-letter codes stay
    manual text fields since Places doesn't return IATA codes.
  - Check-in/check-out (and departure/arrival flight times) now open the same simple
    time-input dialog described below instead of plain text fields.
- `ui/daydetail/AddEditItemSheet.kt` — "Search on Google Maps" fills an itinerary item's
  place name/address and drops the pin; the existing tap-to-adjust map stays underneath
  for fine-tuning.
- `ui/components/SimpleTimePickerDialog.kt` (new) — wraps Material3's `TimeInput`
  (two number boxes + AM/PM) instead of `TimePicker`'s analog clock-face dial. Used for
  exact/range itinerary times and hotel check-in/check-out.
- **Setup required:** these all need the **Places API** enabled for the same Google
  Cloud key as `MAPS_API_KEY` (Maps SDK and Places API are separate APIs on one key).
  Also added: `app/build.gradle.kts` now exposes `MAPS_API_KEY` as a `BuildConfig`
  string (previously it only reached the manifest's `<meta-data>` tag) so
  `TriProApplication` can call `Places.initialize(...)` at startup; the Places SDK
  dependency was added to `gradle/libs.versions.toml` (double-check the pinned version
  against the current release before your first build — left a comment there).

## 5. Custom item description + a new "Show" type
- `data/model/ItineraryItem.kt` — added `ItemType.SHOW` (concerts/plays/movies) and a
  `customLabel: String` field, populated only when `type == CUSTOM`.
- `ui/daydetail/AddEditItemSheet.kt` — shows a "What is this?" text box the moment
  `Custom` is selected as the type.
- `ui/components/ItineraryItemRow.kt` — displays that label under the title for custom
  items, and maps `SHOW` to a theater icon.
- No Firestore migration needed — `customLabel` defaults to `""` for every existing
  document.

## Not changed
Everything else pasted into the conversation (netlify functions, firestore.rules,
NavGraph, AppContainer, the other ViewModels/screens, etc.) is untouched and isn't
included here — only the files above differ from what you shared.
