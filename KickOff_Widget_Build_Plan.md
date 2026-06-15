# KickOff — Android Home-Screen Widget Build Plan
**For: Antigravity (agentic build)**
**Deliverable: ONE Android app whose entire purpose is a single home-screen widget showing the next upcoming match with a live countdown, in the "KickOff World Cup" visual style.**

---

## 0. Hard Constraints (read first)

- **No companion app screens.** Ignore every other mockup in the uploaded zip (`group_standings`, `live_match`, `notifications`, `match_detail`, `component_library`, `home_screen`). They are NOT in scope.
- The **only** required UI surface is the **widget itself**, built from `ios_widget_medium/code.html`.
- One tiny optional Activity is allowed *only* if needed for: (a) entering the API-Football key, (b) requesting the Android 12+ exact-alarm permission, (c) showing "Add the widget to your home screen" instructions on first launch. If the agent prefers, the API key can instead be hardcoded as a `BuildConfig` field from `local.properties` (user already has a key, this is a personal-use app, not a Play Store release) — **pick whichever is simpler and document the choice**, but do not build any additional screens beyond this.
- Target: Android 13+ devices, `minSdk = 26`, `targetSdk` = latest stable.

---

## 1. Reference Assets (already provided)

| Asset | Use |
|---|---|
| `ios_widget_medium/code.html` (+ `screen.png`) | Pixel-accurate visual reference for the widget card |
| `kickoff_world_cup/DESIGN.md` | Source of truth for colors, type, spacing, shape/elevation language |
| `kickoff-widget-configuration.json` | Example of the data shape the widget renders (teams, countdown, venue, group) |
| API-Football v3 docs (api-sports.io) | Data source |

---

## 2. Tech Stack

- Kotlin
- **Jetpack Glance** (`androidx.glance:glance-appwidget`) for the widget UI — modern Compose-style API, replaces hand-written RemoteViews
- **WorkManager** (`androidx.work:work-runtime-ktx`) — periodic + one-off refresh scheduling
- **Retrofit + kotlinx.serialization** (or Moshi) for the API-Football v3 REST calls
- **DataStore (Preferences)** for caching the last-fetched fixture + timestamps
- Plain `HttpURLConnection`/OkHttp `BitmapFactory` for downloading the two team logo images (Glance widgets need `Bitmap`/`ImageProvider`, not remote URLs, so logos must be fetched & decoded in the worker)

### Gradle dependencies to add
```kotlin
implementation("androidx.glance:glance-appwidget:1.1.1")
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.datastore:datastore-preferences:1.1.1")
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
implementation("com.squareup.okhttp3:okhttp:4.12.0")
```

---

## 3. Design Tokens → Android Resources

Port these directly from `DESIGN.md` into `res/values/colors.xml`. Only the subset actually used by the widget card matters:

| Token | Hex | Used for |
|---|---|---|
| `primary` | `#865300` | countdown number text, accents |
| `primary-container` | `#f39c12` | widget background fill (under botanical art) |
| `on-primary` | `#ffffff` | team code labels (ECU/SEN) |
| `primary-fixed-dim` | `#ffb961` | 2px outer border, team-circle border |
| `on-primary-fixed` | `#2b1700` | hard "drop-block" shadow color |
| `surface-container-low` | `#fbf3e4` | countdown pill background |
| `secondary` | `#006d38` | bottom info strip background |
| `secondary-fixed` | `#96f7b0` | "Estadio Azteca" label / stadium icon color |
| `secondary-fixed-dim` | `#7bda96` | group badge background |
| `on-secondary-fixed-variant` | `#005228` | group badge text |
| `surface` | `#fff8ef` | fallback/empty-state background |

### Fonts (`res/font/`)
Download and bundle as `.ttf`:
- **Bricolage Grotesque** (ExtraBold 800) → countdown number (`02:45` style)
- **Hanken Grotesk** (Bold 700 / Medium 500) → "TO KICKOFF" label, team codes
- **JetBrains Mono** (SemiBold 600) → venue + group badge text (`label-data` style)

Define `FontFamily` objects in a `Typography.kt` and reuse them in the Glance `TextStyle`s.

### Shapes
- Card corner radius: **8dp** (rounded), 2dp border, color `primary-fixed-dim`
- Team logo circles: 56dp diameter, 2dp border `primary-fixed-dim`
- Countdown pill: rounded rect, 8dp radius, 2dp border `primary-fixed-dim`, fill `surface-container-low`
- Group badge (bottom-right): small rounded rect, 1dp border `secondary`, fill `secondary-fixed-dim`

### Adaptations for things Glance/RemoteViews CANNOT do
Glance has no CSS — three effects from the HTML need to be approximated:

1. **Noise/grain texture + gradient dimmer over the botanical photo** → **pre-bake into one static PNG asset** (see §4). Do NOT try to recreate the SVG `feTurbulence` filter at runtime.
2. **`rotate(-2deg)` on the countdown pill** → **drop the rotation**. Keep the pill straight. This is a minor, acceptable visual simplification for a widget.
3. **Hard 4px "drop-block" shadow** (`box-shadow: 4px 4px 0px 0px rgba(43,23,0,1)`) → implement as a **layered Box**: a solid-color rectangle (color `on-primary-fixed`, same corner radius) drawn first, then the real card drawn on top offset by ~4dp via `GlanceModifier.padding(end = 4.dp, bottom = 4.dp)` on the back layer (or `top/start` on the front layer — whichever is easier in Glance's Box stacking). Apply this same two-layer pattern to: the outer card, the countdown pill, and the group badge.

---

## 4. Asset Prep Task (one-time, do this early)

Generate a single PNG: **`widget_card_background.png`** (and `.9.png`/density variants if time allows), representing the card's botanical photo **with the noise overlay and the primary-color gradient dimmer already baked in**, rounded corners + border, but **without** the team circles, countdown pill, or bottom strip (those are drawn by Glance on top).

Recipe (Python + Pillow, run once during build):
1. Download the botanical background image referenced in `ios_widget_medium/code.html` (`botanical-bg` URL).
2. Resize/crop to the card's aspect ratio (348:120, i.e. ignore the bottom 40px strip — that's drawn separately as a flat `secondary` color block).
3. Overlay a vertical linear gradient from `primary` @ 80% opacity (top) → `primary` @ 40% opacity (bottom) using `multiply`/`overlay` blend.
4. Generate random fractal noise (numpy) at ~15% opacity, blend with `multiply`.
5. Round the top corners to 8dp-equivalent px, add a 2px outer stroke in `primary-fixed-dim`.
6. Export at `xhdpi`/`xxhdpi` sizes into `res/drawable-*/widget_card_background.png`.

The flat bottom strip (`secondary` color, 2px top border `primary-fixed-dim`) can just be an XML shape drawable — no image needed.

---

## 5. Data Layer

### 5.1 Config constants (`Config.kt`)
```kotlin
object Config {
    const val API_BASE_URL = "https://v3.football.api-sports.io/"
    const val LEAGUE_ID = 1     // "World Cup" — verified via /leagues?id=1&season=2026
    const val SEASON = 2026
    // To follow a different competition later, find the id via:
    // GET /leagues?search=<name>  and update LEAGUE_ID/SEASON here only.
}
```

### 5.2 Retrofit service (`ApiFootballService.kt`)
```kotlin
interface ApiFootballService {

    @GET("fixtures")
    suspend fun getNextFixture(
        @Header("x-apisports-key") apiKey: String,
        @Query("league") league: Int = Config.LEAGUE_ID,
        @Query("season") season: Int = Config.SEASON,
        @Query("next") next: Int = 1
    ): FixtureResponse

    // Optional: used to resolve "GROUP A"/"GROUP B" labels during the group stage
    @GET("standings")
    suspend fun getStandings(
        @Header("x-apisports-key") apiKey: String,
        @Query("league") league: Int = Config.LEAGUE_ID,
        @Query("season") season: Int = Config.SEASON
    ): StandingsResponse
}
```

### 5.3 Minimal response models (only the fields the widget needs)
```kotlin
@Serializable
data class FixtureResponse(val response: List<FixtureItem>)

@Serializable
data class FixtureItem(
    val fixture: FixtureInfo,
    val league: LeagueInfo,
    val teams: Teams,
    val goals: Goals
)

@Serializable
data class FixtureInfo(
    val id: Long,
    val date: String,       // ISO-8601 with offset, e.g. "2026-06-15T18:00:00+00:00"
    val status: StatusInfo,
    val venue: VenueInfo
)

@Serializable data class StatusInfo(val short: String, val elapsed: Int? = null) // "NS","1H","HT","FT" etc.
@Serializable data class VenueInfo(val name: String?, val city: String?)
@Serializable data class LeagueInfo(val round: String)
@Serializable data class Teams(val home: TeamInfo, val away: TeamInfo)
@Serializable data class TeamInfo(val id: Int, val name: String, val code: String? = null, val logo: String)
@Serializable data class Goals(val home: Int?, val away: Int?)

@Serializable
data class StandingsResponse(val response: List<StandingsLeague>)
@Serializable data class StandingsLeague(val league: StandingsLeagueDetail)
@Serializable data class StandingsLeagueDetail(val standings: List<List<StandingRow>>)
@Serializable data class StandingRow(val team: TeamInfo, val group: String)
```

### 5.4 Repository (`FixtureRepository.kt`)
- `suspend fun fetchNextMatchCard(): MatchCardState`
  1. Call `getNextFixture()`.
  2. If empty response → return `MatchCardState.NoUpcomingMatch`.
  3. Compute `kickoffEpochMillis` by parsing `fixture.date` (ISO-8601 → `Instant`).
  4. Derive team codes: use `team.code` if non-null, else first 3 letters of `team.name` uppercased.
  5. Derive the bottom-right badge text:
     - Call `getStandings()` once per day (cache result), build a `teamId -> "GROUP X"` map from `standings[].team.id` / `.group`.
     - If the home team's id is in the map → badge = that group string (uppercase).
     - Else → badge = `league.round` (uppercase), e.g. `"ROUND OF 16"`.
  6. Download `teams.home.logo` and `teams.away.logo`, decode to `Bitmap`, **clip to a circle in code** (Canvas + `Paint` with `PorterDuff.Mode.SRC_IN`, since Glance's corner-radius support on `Image` is inconsistent across API levels — clipping in code is the robust path).
  7. Return a fully-populated `MatchCardState.Match(...)`.

### 5.5 Cached UI state model (`MatchCardState.kt`)
```kotlin
sealed interface MatchCardState {
    data object Loading : MatchCardState
    data object NoUpcomingMatch : MatchCardState
    data class Match(
        val homeCode: String, val awayCode: String,
        val homeLogoPath: String, val awayLogoPath: String, // cached file paths
        val kickoffEpochMillis: Long,
        val venueName: String,
        val badgeText: String,
        val status: String,          // "NS" | "1H" | "2H" | "HT" | "FT" | ...
        val homeGoals: Int?, val awayGoals: Int?,
        val lastUpdatedEpochMillis: Long
    ) : MatchCardState
}
```
Serialize this to JSON and store under one DataStore `Preferences` key (simplest approach — avoids Proto DataStore setup). Cached logo bitmaps go to `context.filesDir/widget_cache/home.png` and `away.png`.

---

## 6. Refresh & Countdown Strategy

Widgets can't run a real-time per-second clock, so the countdown is **recomputed on each refresh** and displayed as **"Hh Mm"** (or **"Mm"** once under an hour) — close in spirit to the mockup's `02:45`, but realistically hours:minutes given refresh granularity.

### 6.1 `FixtureSyncWorker` (CoroutineWorker)
- Calls `FixtureRepository.fetchNextMatchCard()`, writes result to DataStore + bitmap cache, then calls `KickoffWidget().updateAll(context)`.
- On failure (network error / rate limit), **leave the existing cached state untouched** so the widget keeps showing the last-known match rather than going blank; just update `lastUpdatedEpochMillis`-based "stale" flag if you want a small visual cue (optional, see §8 empty/error states).

### 6.2 Scheduling (in `KickoffWidgetReceiver.onEnabled` / `onUpdate`)
- Enqueue a **`PeriodicWorkRequest`** every **30 minutes** (`ExistingPeriodicWorkPolicy.KEEP`) — this is the steady-state refresh. 30-min cadence = 48 calls/day, well inside the free-plan **100 requests/day** quota (standings call adds ~1/day, image downloads don't count toward quota).
- After every successful sync, **also enqueue a `OneTimeWorkRequest`** with a calculated delay so the countdown ticks down more visibly as kickoff approaches:
  - If kickoff is **> 3 hours away** → next tick in 30 min (i.e. let the periodic worker handle it, no extra one-off needed).
  - If kickoff is **within 3 hours** → one-off tick every **15 min**.
  - If kickoff is **within 30 min** → one-off tick every **5 min**.
  - If match is **live** (`status` in `1H/HT/2H/ET`) → one-off tick every **5–10 min** to refresh the live score/elapsed time.
- This avoids needing exact alarms / `SCHEDULE_EXACT_ALARM` permission entirely — WorkManager's flexible scheduling is sufficient for a "minutes" level countdown.
- `onDisabled` → cancel both the periodic and any pending one-off work.

### 6.3 Cancel/reset on widget removal
`KickoffWidgetReceiver.onDisabled(context)` → `WorkManager.getInstance(context).cancelUniqueWork("fixture_sync")`.

---

## 7. Glance Widget UI (`KickoffWidget.kt`)

Structure mirrors `ios_widget_medium/code.html` 1:1:

```
Box (fills widget, background = widget_card_background drawable, 8dp corner radius via drawable)
 ├─ Row (height ~120dp, padding 16dp horiz, top 8dp)  — main content area
 │   ├─ Column (width 80dp, centered) — HOME
 │   │    ├─ Image(homeLogoBitmap) 56dp circle, 2dp border
 │   │    └─ Text(homeCode) — Hanken Grotesk Bold, on-primary
 │   ├─ Column (weight = 1f, centered) — COUNTDOWN
 │   │    ├─ Box (drop-block layered: shadow rect + pill)
 │   │    │    └─ Text(countdownLabel) — Bricolage Grotesque ExtraBold, primary, ~28sp
 │   │    └─ Text("TO KICKOFF") — Hanken Grotesk Bold, small caps, surface-container-low
 │   └─ Column (width 80dp, centered) — AWAY
 │        ├─ Image(awayLogoBitmap) 56dp circle, 2dp border
 │        └─ Text(awayCode)
 └─ Box (height 40dp, fillMaxWidth, background = secondary, border-top 2dp primary-fixed-dim)
      └─ Row (space-between, padding 16dp horiz)
          ├─ Row: stadium icon + Text(venueName) — JetBrains Mono, secondary-fixed
          └─ Box (drop-block badge) → Text(badgeText) — JetBrains Mono Bold, on-secondary-fixed-variant
```

### State branching at the top of `provideGlance`
```kotlin
when (val state = currentState<MatchCardState>()) {
    is MatchCardState.Loading -> LoadingCard()
    is MatchCardState.NoUpcomingMatch -> EmptyCard("No upcoming matches")
    is MatchCardState.Match -> MatchCard(state)
}
```

### Countdown / status text logic (inside `MatchCard`)
```kotlin
val now = System.currentTimeMillis()
val remaining = state.kickoffEpochMillis - now
val centerLabel = when {
    state.status in setOf("1H","2H","HT","ET","P","LIVE") ->
        "${state.homeGoals}-${state.awayGoals}"   // live score instead of countdown
    state.status == "FT" -> "FT"
    remaining <= 0 -> "LIVE"
    remaining < 60 * 60_000 -> "${remaining / 60_000}m"
    else -> "${remaining / 3_600_000}h ${(remaining % 3_600_000) / 60_000}m"
}
val captionLabel = when (state.status) {
    "FT" -> "FULL TIME"
    "1H","2H","HT","ET","P","LIVE" -> "LIVE"
    else -> "TO KICKOFF"
}
```

### `LoadingCard()` / `EmptyCard()`
Simple centered text on the `surface` background ("Loading next match…" / "No upcoming matches"), same outer card shape, no botanical art needed (use a flat `surface-container` fill).

---

## 8. Widget Provider, Manifest, Sizing

### `res/xml/kickoff_widget_info.xml`
```xml
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="110dp"
    android:targetCellWidth="4"
    android:targetCellHeight="2"
    android:resizeMode="horizontal"
    android:widgetCategory="home_screen"
    android:previewImage="@drawable/widget_preview"
    android:updatePeriodMillis="1800000"
    android:description="@string/widget_description" />
```
(`updatePeriodMillis` set to 30 min as a *fallback* in case WorkManager is killed by the OS; the real refresh logic lives in WorkManager.)

### `AndroidManifest.xml`
```xml
<uses-permission android:name="android.permission.INTERNET" />

<receiver android:name=".KickoffWidgetReceiver"
    android:exported="false"
    android:label="@string/widget_label">
    <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
    </intent-filter>
    <meta-data android:name="android.appwidget.provider"
        android:resource="@xml/kickoff_widget_info" />
</receiver>
```

### `KickoffWidgetReceiver.kt`
```kotlin
class KickoffWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KickoffWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        FixtureSyncScheduler.start(context) // enqueues periodic work + immediate one-off
    }

    override fun onDisabled(context: Context) {
        FixtureSyncScheduler.stop(context)
        super.onDisabled(context)
    }
}
```

---

## 9. Optional Minimal Setup Activity

If the agent decides not to hardcode the API key via `BuildConfig`/`local.properties`:

- One screen, one `TextField` for the API key, one "Save" button → writes to DataStore.
- On save, immediately trigger `FixtureSyncWorker` once (so the widget populates without waiting 30 min) and show a short instruction text: *"Now long-press your home screen → Widgets → KickOff to add it."*
- No navigation, no other screens, no bottom nav, nothing from `component_library`.

If hardcoding is chosen instead: put the key in `local.properties` (`apiFootballKey=...`), expose via `buildConfigField` in `build.gradle.kts`, and the app needs **zero** activities (Android allows an app with only a widget receiver and no launcher activity).

---

## 10. State / Edge-Case Matrix

| Condition | Widget shows |
|---|---|
| Normal, match `NS` (not started), > 1h away | Team circles + codes, countdown `"Xh Ym"`, "TO KICKOFF", venue + group/round badge |
| < 1h away | Countdown `"Xm"` |
| Match live (`1H`/`2H`/`HT`/`ET`) | Center shows live score `"H-A"`, caption `"LIVE"` |
| Match finished (`FT`) | Center shows final score, caption `"FULL TIME"`; widget keeps showing this until the *next* `/fixtures?next=1` call returns a new fixture (next sync after the match ends) |
| No fixtures returned (off-season / between tournaments) | Flat empty-state card: "No upcoming matches" |
| API error / rate-limited | Keep last cached `Match` state as-is (stale-but-shown); do not crash or blank the widget |
| First install, before first sync completes | `LoadingCard` |

---

## 11. Build Order Checklist (for Antigravity to execute sequentially)

1. Create new Android project, `minSdk 26`, Kotlin, no default Activity (or a single minimal one per §9).
2. Add Gradle dependencies (§2).
3. Add color resources, font files, and `Typography.kt` (§3).
4. Generate `widget_card_background.png` asset per §4 and drop into `res/drawable*`.
5. Build `Config.kt`, serialization models, `ApiFootballService`, `FixtureRepository` (§5).
6. Build `MatchCardState` + DataStore read/write helpers + bitmap circle-cropping util.
7. Build `FixtureSyncWorker` + `FixtureSyncScheduler` (§6).
8. Build `KickoffWidget` Glance composable with all three states + drop-block layering pattern (§7).
9. Add `kickoff_widget_info.xml` + manifest receiver entry (§8).
10. (If applicable) build the minimal setup Activity (§9).
11. Manual test: install on device/emulator, add widget, confirm it renders with the World Cup 2026 (`league=1`, `season=2026`) data, confirm countdown decreases across refresh cycles, confirm bottom badge shows `GROUP X` during group stage or round name during knockout, confirm graceful empty/error states by temporarily using an invalid league id / API key.
