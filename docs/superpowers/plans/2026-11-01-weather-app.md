# Weather App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build Iwan's ad-free Android 16 weather app with home-screen widgets, as described in the approved design.

**Architecture:**
- **Structure:** a single `:app` module, split into packages by responsibility.
- **Data:** BMKG, Open-Meteo and WAQI clients produce source models. One pure merger turns them into a `WeatherSnapshot`. A repository adds location, caching and fallbacks.
- **UI:** the Compose UI and the Glance widgets only read `WeatherSnapshot`.
- **Parallel work:** Task 1 fixes every shared type and the build. Tasks 2–6 can then be built in parallel on separate branches, and Task 7 wires them together.

**Tech Stack:**
- **Build:** Kotlin 2.3.20 (AGP 9.3.1 built-in Kotlin), Gradle 9.5.0 on JDK 25 (same as `~/Code/jk-bms-monitor`).
- **UI:** Jetpack Compose (BOM 2026.02.01, Material 3 1.4 expressive APIs), material-icons-extended 1.7.8.
- **Widgets and background work:** Glance 1.1.1, WorkManager 2.10.5.
- **Data:** OkHttp 4.12, kotlinx.serialization 1.11.0, DataStore 1.1.1, play-services-location 21.3.0.
- **Tests:** JUnit 4 with kotlinx-coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-01-weather-app-design.md`. Mockups are in `docs/superpowers/specs/mockups/`. Read both before starting a task.

## Global Constraints

- **Package and versions:** package `com.iwankurnia.weather`, app name "Weather", minSdk 31, targetSdk and compileSdk 37.
- **Privacy:** no ads, analytics, tracking or sign-in. The only network calls go to `api.bmkg.go.id`, `api.open-meteo.com`, `air-quality-api.open-meteo.com` and `api.waqi.info`.
- **Units:** Celsius, km/h, mm, 24-hour clock, dates like "Sunday, 1 November", English UI copy in British spelling.
- **Feels like:** the NOAA heat index computed from BMKG temperature and humidity. If the heat index is below the temperature, show the temperature.
- **Thresholds:**
  - Rain is "likely" when BMKG forecasts rain or thunderstorm, or Open-Meteo's chance of rain is 50% or more.
  - "Very hot" means feels like 38° or more.
  - These thresholds live only in `domain/Thresholds.kt`.
- **Backgrounds:** set by weather and day/night only, never by temperature. Day runs from local sunrise to sunset.
- **BMKG codes to conditions:**

  | Condition | BMKG codes |
  |---|---|
  | Clear | 0 |
  | Partly cloudy | 1, 2 |
  | Cloudy | 3, 4 |
  | Haze | 5, 10, 45 |
  | Rain | 60, 61, 63, 80 |
  | Thunderstorm | 95, 97 |

  Unknown codes become Cloudy.
- **Open-Meteo codes to conditions** (fallback only):

  | Condition | WMO codes |
  |---|---|
  | Clear | 0 |
  | Partly cloudy | 1, 2 |
  | Cloudy | 3 |
  | Haze | 45, 48, or no rain with visibility under 5 km |
  | Rain | 51–67, 80–82 |
  | Thunderstorm | 95–99 |

  Other codes become Cloudy.
- **Fallbacks:**
  - Use Open-Meteo for everything when BMKG fails, returns no steps, or the nearest kelurahan is more than 15 km away.
  - Use Open-Meteo CAMS air quality, marked "modelled", when WAQI fails or has no token.
- **Kelurahan coverage:** the bundled kelurahan list covers Jabodetabek only, about 1,500 kelurahan. Building it means one BMKG request per code, and all of Indonesia would take about a day at the rate limit. Outside the list, the 15 km rule hands over to Open-Meteo.
- **Refresh:** BMKG at most once an hour. Open-Meteo and WAQI on every fetch. Widgets refresh about every 30 minutes.
- **Credits line:** "Updated hh:mm · Weather: BMKG · Open-Meteo.com · Air quality: WAQI". WAQI attribution names the station's originating agency when the feed gives one.
- **Secrets:** the WAQI token is read from `local.properties` key `waqi.token` into `BuildConfig.WAQI_TOKEN`, empty string when missing. `local.properties` is never committed.
- **Motion:** every animation stops when the system "Remove animations" setting is on (`ANIMATOR_DURATION_SCALE == 0`).
- **Widget opacity:** stored per widget, 0–100, default 100. At 40 or lower the text turns white with a soft shadow.

## Review Focus

- **No network and no cache on first launch:** the error screen offers "Try again", with no crash or blank screen. *(Task 6, `repository_firstFetchFails_returnsError`)*
- **BMKG data near the end of its 3-day window**, when the "Now" step is the last one available: the merger still produces "Now" and today's high and low without an index error. *(Task 3, `merge_nowIsLastStep_noCrash`)*
- **WAQI with no token or bad JSON:** air quality falls back to the "modelled" reading and the rest of the snapshot is unaffected. *(Task 2, `waqi_errorStatus_returnsNull`; Task 6, `repository_waqiMissing_usesModelledAir`)*
- **Phone outside Jabodetabek**, for example in Bandung: Open-Meteo is used and the area name still shows. *(Task 2, `nearest_farPoint_returnsNull`; Task 6, `repository_outsideCoverage_usesOpenMeteo`)*
- **Time zone:** BMKG `local_datetime` is Asia/Jakarta. Step times must not shift when the phone's time zone differs. *(Task 2, `bmkg_parsesLocalDatetimeAsJakarta`)*

---

### Task 1: Scaffold, shared contracts and sample data (lead, before any parallel work)

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, `gradle/wrapper/*`, `gradle/gradle-daemon-jvm.properties`, `app/build.gradle.kts`, `app/proguard-rules.pro`
- Create: `app/src/main/AndroidManifest.xml`. It declares permissions `INTERNET`, `ACCESS_COARSE_LOCATION`, `ACCESS_FINE_LOCATION` and `ACCESS_BACKGROUND_LOCATION`, the activity `.MainActivity`, the receivers `.widget.LargeWidgetReceiver` and `.widget.SmallWidgetReceiver`, and the activity `.widget.WidgetConfigActivity`.
- Create: `app/src/main/java/com/iwankurnia/weather/WeatherApplication.kt` and `MainActivity.kt` (placeholder text).
- Create: `app/src/main/java/com/iwankurnia/weather/model/Weather.kt`, for the domain types below.
- Create: `app/src/main/java/com/iwankurnia/weather/data/source/SourceModels.kt`, for the source types below.
- Create: `app/src/main/java/com/iwankurnia/weather/data/Contracts.kt`, for the interfaces below.
- Create: `app/src/main/java/com/iwankurnia/weather/model/SampleData.kt`, holding the Jakarta values from the mockups (`SampleData.dayPartlyCloudy`, `SampleData.nightRain`).
- Create: `app/src/main/res/xml/large_widget_info.xml`, `small_widget_info.xml`, launcher icon (adaptive, plus a monochrome layer for the themed icon).
- Modify: `.gitignore` to add `build/`, `.gradle/`, `local.properties`, `.idea/`, `*.iml` and `.kotlin/`.

**Interfaces (Produces):** every later task uses exactly these names.

```kotlin
// model/Weather.kt
enum class Condition { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, THUNDERSTORM, HAZE }
enum class DataSource { BMKG, OPEN_METEO }
data class Place(val areaName: String, val latitude: Double, val longitude: Double, val kelurahanCode: String?)
data class Now(val temperatureC: Double, val feelsLikeC: Double, val humidityPct: Int, val condition: Condition,
               val windKmh: Double, val windFromDeg: Int, val isDay: Boolean)
data class ForecastStep(val time: java.time.LocalDateTime, val temperatureC: Double, val condition: Condition,
                        val isDay: Boolean, val rainChancePct: Int?)
data class DayForecast(val date: java.time.LocalDate, val lowC: Double, val highC: Double, val condition: Condition,
                       val rainChancePct: Int?, val source: DataSource)
data class AirQuality(val usAqi: Int, val stationName: String?, val attribution: String?, val modelled: Boolean)
data class Details(val uvIndex: Double?, val dewPointC: Double?, val gustKmh: Double?, val rainLast24hMm: Double?,
                   val rainTomorrowMm: Double?, val sunrise: java.time.LocalTime, val sunset: java.time.LocalTime)
data class WeatherSnapshot(val place: Place, val now: Now, val todayHighC: Double, val todayLowC: Double,
                           val steps: List<ForecastStep>, val days: List<DayForecast>, val details: Details,
                           val air: AirQuality?, val summary: String, val source: DataSource,
                           val fetchedAt: java.time.Instant)

// data/source/SourceModels.kt
data class BmkgStep(val local: java.time.LocalDateTime, val temperatureC: Double, val humidityPct: Int,
                    val weatherCode: Int, val windKmh: Double, val windFromDeg: Int, val rainMm: Double)
data class BmkgForecast(val kelurahan: String, val kecamatan: String, val latitude: Double, val longitude: Double,
                        val steps: List<BmkgStep>)
data class OmHour(val time: java.time.LocalDateTime, val temperatureC: Double, val apparentC: Double, val humidityPct: Int,
                  val weatherCode: Int, val rainChancePct: Int, val precipitationMm: Double, val visibilityM: Double?,
                  val uvIndex: Double, val dewPointC: Double, val windKmh: Double, val windFromDeg: Int,
                  val gustKmh: Double, val isDay: Boolean)
data class OmDay(val date: java.time.LocalDate, val lowC: Double, val highC: Double, val weatherCode: Int,
                 val rainChancePct: Int, val sunrise: java.time.LocalTime, val sunset: java.time.LocalTime)
data class OpenMeteoForecast(val hours: List<OmHour>, val days: List<OmDay>, val modelledUsAqi: Int?)
data class Kelurahan(val code: String, val name: String, val kecamatan: String, val latitude: Double, val longitude: Double)

// data/Contracts.kt
interface BmkgClient { suspend fun forecast(adm4: String): BmkgForecast }                   // throws on failure
interface OpenMeteoClient { suspend fun forecast(lat: Double, lon: Double): OpenMeteoForecast } // throws on failure
interface AirClient { suspend fun nearest(lat: Double, lon: Double): AirQuality? }            // null on any failure
interface KelurahanIndex { fun nearest(lat: Double, lon: Double, maxKm: Double = 15.0): Kelurahan? }
data class GeoPoint(val latitude: Double, val longitude: Double)
interface LocationSource { suspend fun current(): GeoPoint? }                                // null if no permission/fix
interface AreaNamer { suspend fun name(lat: Double, lon: Double): String? }
sealed interface LoadResult {
    data class Fresh(val snapshot: WeatherSnapshot) : LoadResult
    data class Stale(val snapshot: WeatherSnapshot, val reason: String) : LoadResult  // offline, shows banner
    data class NoLocation(val permissionDenied: Boolean) : LoadResult
    data class Failed(val message: String) : LoadResult
}
interface WeatherRepository { suspend fun load(forceRefresh: Boolean): LoadResult; suspend fun cached(): WeatherSnapshot? }
```

- [ ] **Step 1:** Write the build files, mirroring the plugin and repository setup of `~/Code/jk-bms-monitor`:
  - Plugins `com.android.application` and `org.jetbrains.kotlin.plugin.compose` (AGP built-in Kotlin), plus `org.jetbrains.kotlin.plugin.serialization`.
  - `buildConfig = true`, and `BuildConfig.WAQI_TOKEN` from `local.properties`.
  - All dependencies for Tasks 2–7 declared now, so no later task edits a build file.
- [ ] **Step 2:** Write the manifest, the contracts and `SampleData` exactly as above. Add a widget info XML for each receiver:
  - Large widget: `targetCellWidth` 4, `targetCellHeight` 2.
  - Small widget: `targetCellWidth` 2, `targetCellHeight` 2.
  - Both: `configure` points to `.widget.WidgetConfigActivity`, `widgetFeatures="reconfigurable|configuration_optional"`, `updatePeriodMillis` 0.
- [ ] **Step 3:** Add `app/src/main/assets/kelurahan.csv` (generated by `tools/build_kelurahan.py`; header `code,kelurahan,kecamatan,kota,lat,lon`), and commit the generator script with a README line on how to re-run it.
- [ ] **Step 4:** Run `./gradlew :app:assembleDebug :app:testDebugUnitTest`. Expected: BUILD SUCCESSFUL.
- [ ] **Step 5:** Commit: `chore: scaffold app, shared contracts and kelurahan list`.

---

### Task 2: Source clients and parsers (parallel, branch `feat/sources`)

**Files:** create in `app/src/main/java/com/iwankurnia/weather/data/source/`:
- `Http.kt`: one shared `OkHttpClient` with 15 s timeouts and `suspend fun getText(url: String): String`.
- `BmkgParser.kt`, `BmkgHttpClient.kt`
- `OpenMeteoParser.kt`, `OpenMeteoHttpClient.kt`
- `WaqiParser.kt`, `WaqiHttpClient.kt`
- `AssetKelurahanIndex.kt`
- `ConditionCodes.kt`: `fun bmkgCondition(code: Int): Condition` and `fun wmoCondition(code: Int, visibilityM: Double?, raining: Boolean): Condition`.
- `location/FusedLocationSource.kt`, `location/GeocoderAreaNamer.kt`

Tests go in `app/src/test/java/com/iwankurnia/weather/data/source/`, with JSON fixtures in `app/src/test/resources/`: real responses captured with curl for Kemayoran `31.71.03.1001`, Open-Meteo forecast and air quality for −6.2297, 106.7997, and a WAQI sample from the API docs.

**Interfaces:**
- Consumes: Task 1 contracts and source models.
- Produces:
  - `BmkgParser.parse(json: String): BmkgForecast`
  - `OpenMeteoParser.parse(forecastJson: String, airJson: String?): OpenMeteoForecast`
  - `WaqiParser.parse(json: String): AirQuality?`
  - `AssetKelurahanIndex(rows: List<Kelurahan>)` and `AssetKelurahanIndex.fromCsv(text: String)`
  - The HTTP clients implementing `BmkgClient`, `OpenMeteoClient` and `AirClient`
  - `FusedLocationSource(context)` and `GeocoderAreaNamer(context)`

- [ ] **Step 1: Write the failing tests:**
  - `bmkg_parsesLocalDatetimeAsJakarta`: the steps are flattened in time order, and `local` equals the `local_datetime` string parsed as `LocalDateTime`, whatever the JVM default time zone (set `TimeZone.setDefault(UTC)` in the test).
  - `bmkg_mapsFields`: `t → temperatureC`, `hu → humidityPct`, `weather → weatherCode`, `ws → windKmh`, `wd_deg → windFromDeg`, `tp → rainMm`, and `lokasi.desa/kecamatan/lat/lon`.
  - `conditionCodes_bmkgTable` and `conditionCodes_wmoTable`: every code from Global Constraints, plus code 999 → CLOUDY, and WMO 3 with visibility 4000 and not raining → HAZE.
  - `openMeteo_parsesHourlyDailyAndAir`: 24+ hours, 10 days, `modelledUsAqi` from `current.us_aqi`, sunrise and sunset as `LocalTime`.
  - `waqi_parsesAqiStationAttribution` (`status:"ok"` → `usAqi`, `city.name`, the first `attributions[].name`, `modelled=false`) and `waqi_errorStatus_returnsNull` (`status:"error"` or malformed JSON → null).
  - `nearest_returnsClosestWithinRadius`: a point 100 m from Kemayoran's centre returns `31.71.03.1001`. `nearest_farPoint_returnsNull`: Bandung, −6.9175, 107.6191, returns null. Use the haversine distance.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*data.source*'`. Expected: FAIL, the classes don't exist yet.
- [ ] **Step 3:** Implement the parsers with kotlinx.serialization `Json { ignoreUnknownKeys = true }`, and the clients:
  - **BMKG URL:** `https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=$code`.
  - **Open-Meteo URL:** `https://api.open-meteo.com/v1/forecast` with `hourly=temperature_2m,apparent_temperature,relative_humidity_2m,weather_code,precipitation_probability,precipitation,visibility,uv_index,dew_point_2m,wind_speed_10m,wind_direction_10m,wind_gusts_10m,is_day`, `daily=temperature_2m_min,temperature_2m_max,weather_code,precipitation_probability_max,sunrise,sunset`, `past_days=1`, `forecast_days=10`, `timezone=Asia/Jakarta`.
  - **Open-Meteo air quality:** `https://air-quality-api.open-meteo.com/v1/air-quality?current=us_aqi`.
  - **WAQI URL:** `https://api.waqi.info/feed/geo:$lat;$lon/?token=${BuildConfig.WAQI_TOKEN}`. The client returns null without calling when the token is blank.
  - **`FusedLocationSource`:** `getCurrentLocation(PRIORITY_BALANCED_POWER_ACCURACY)`, falling back to `lastLocation`. Null when permission is missing.
  - **`GeocoderAreaNamer`:** `subLocality ?: locality`, using the API 33 listener form.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat: BMKG, Open-Meteo and WAQI clients with parsers and kelurahan lookup`.

---

### Task 3: Merger, heat index and summary line (parallel, branch `feat/domain`)

**Files:** create in `app/src/main/java/com/iwankurnia/weather/domain/`:
- `Thresholds.kt` (`RAIN_LIKELY_PCT = 50`, `VERY_HOT_FEELS_C = 38.0`, `SUMMARY_LOOKAHEAD_HOURS = 12`, `BMKG_MAX_KM = 15.0`)
- `HeatIndex.kt`
- `SnapshotMerger.kt`
- `SummaryLine.kt`

Tests go in `app/src/test/java/com/iwankurnia/weather/domain/`, building source models in code (no JSON).

**Interfaces:**
- Consumes: Task 1 source and domain models.
- Produces:
  - `fun feelsLikeC(temperatureC: Double, humidityPct: Int): Double`
  - `object SnapshotMerger { fun merge(place: Place, bmkg: BmkgForecast?, om: OpenMeteoForecast, air: AirQuality?, now: LocalDateTime, fetchedAt: Instant): WeatherSnapshot }`
  - `fun summaryLine(steps: List<ForecastStep>, feelsLike: List<Pair<LocalDateTime, Double>>): String`

- [ ] **Step 1: Write the failing tests:**
  - `heatIndex_noaaReference`: 32 °C and 70% → 40.6 ± 0.5. 33 °C and 72% → 43.0 ± 0.6. 25 °C and 90% → 25.0 (below about 26.7 °C the temperature is returned). It never returns less than the temperature.
  - `merge_bmkgPrimary`:
    - With BMKG steps at 10:00 (30 °C) and 13:00 (33 °C) and "now" at 11:30, `now.temperatureC` = 31.5 (linear interpolation; humidity the same way).
    - `now.condition` comes from the nearest step.
    - `source = BMKG`, and `steps` starts at the step covering now, every 3 hours for 24 hours.
    - `rainChancePct` for each step is Open-Meteo's value at that hour.
  - `merge_daysSplitBySource`: days 1–3 come from BMKG (min/max of that day's steps; condition is the most severe of the 06:00–18:00 steps, ordered THUNDERSTORM > RAIN > HAZE > CLOUDY > PARTLY_CLOUDY > CLEAR) with `source=BMKG`. Days 4–10 come from Open-Meteo. 10 days in total. Every day's `rainChancePct` is Open-Meteo's daily maximum.
  - `merge_details`:
    - `rainTomorrowMm` is the sum of tomorrow's BMKG `rainMm`.
    - `rainLast24hMm` is the sum of Open-Meteo `precipitationMm` over the 24 hours before now.
    - UV, dew point and gust come from the Open-Meteo hour covering now.
  - `merge_fallbackOpenMeteo`: with `bmkg = null`:
    - `source = OPEN_METEO`.
    - Steps are hourly, keeping every third hour so the row looks the same.
    - `feelsLikeC` comes from `apparentC`.
    - The condition comes from `wmoCondition`.
  - `merge_nowIsLastStep_noCrash`: "now" is after the last BMKG step, and the merger uses the last step without throwing.
  - `merge_isDay_usesSunriseSunset`: 05:00 is night and 06:00 is day when sunrise is 05:31.
  - `merge_airFallback`: when `air` is null and `om.modelledUsAqi = 163`, the result is `AirQuality(163, null, "Open-Meteo (CAMS)", modelled = true)`.
  - `summary_rainAndHeat`:
    - "Rain likely from around 16:00. Very hot until then." when the 16:00 step is RAIN and earlier steps have feels like ≥ 38.
    - "Rain likely from around 16:00." when it isn't hot.
    - "Very hot until 16:00." when it's hot with no rain.
    - "Dry and warm for the next few hours." when neither applies.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*domain*'`. Expected: FAIL.
- [ ] **Step 3:** Implement. Heat index is the NOAA Rothfusz regression in °F with the Steadman simple formula first, as on the NWS heat-index page, converted back to °C.
- [ ] **Step 4:** Run the same command. Expected: PASS.
- [ ] **Step 5:** Commit: `feat: merge BMKG and Open-Meteo into a snapshot with heat index and summary line`.

---

### Task 4: Weather backgrounds (parallel, branch `feat/backgrounds`)

**Files:** create in `app/src/main/java/com/iwankurnia/weather/ui/background/`:
- `WeatherBackground.kt`: `@Composable fun WeatherBackground(condition: Condition, isDay: Boolean, modifier: Modifier = Modifier)`
- `Palettes.kt`: gradients and card tints for the 12 combinations.
- `SkyLayers.kt`: sun glow, moon and stars, clouds, fog.
- `RainLayer.kt`: falling rain, splashes, cloud band, lightning.
- `MotionSetting.kt`: `@Composable fun rememberAnimationsEnabled(): Boolean` reading `Settings.Global.ANIMATOR_DURATION_SCALE`.
- Tests: `app/src/test/java/com/iwankurnia/weather/ui/background/RainModelTest.kt`, for the pure drop and bolt simulation in `RainModel.kt`.

**Interfaces:**
- Consumes: `Condition`.
- Produces: `WeatherBackground(...)`, and `fun cardTint(condition: Condition, isDay: Boolean): CardColours`, where `data class CardColours(val surface: Color, val onSurface: Color, val subtle: Color, val primary: Color, val onPrimary: Color, val container: Color, val onContainer: Color)` is used by the main screen. The colour values come from `mockups/backgrounds-day-night.html` and `mockups/rain-storm.html`, with the darker haze-day sky.

- [ ] **Step 1: Write the failing tests in `RainModelTest`:**
  - `drops_recycleAtBottom`: after enough ticks, every drop has a y within [−len, height].
  - `storm_hasMoreDropsAndWind`: the storm model has more drops and a larger slant than rain.
  - `bolt_staysInUpperHalf`: every bolt point has a y ≤ 0.6 × height.
  - `reducedMotion_staticFrame`: `RainModel(animated = false).tick()` changes nothing.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*background*'`. Expected: FAIL.
- [ ] **Step 3: Implement.**
  - `RainModel` is a pure Kotlin simulation: two drop layers (near drops longer and faster), splashes along the bottom 6%, a bolt with branches every 3.5–7.5 s, a flash that decays, and a flicker.
  - `RainLayer` draws it on a `Canvas`, driven by `withFrameNanos`.
  - The sky layers follow the mockups.
  - With animations off, show one still frame and no lightning.
- [ ] **Step 4:** Run the same command. Expected: PASS. Add `@Preview`s for all 12 combinations.
- [ ] **Step 5:** Commit: `feat: animated weather backgrounds for six conditions by day and night`.

---

### Task 5: Main screen UI (parallel, branch `feat/main-ui`)

**Files:** create in `app/src/main/java/com/iwankurnia/weather/ui/`:
- `theme/Theme.kt`, `theme/Type.kt`. Roboto Flex is bundled in `res/font/` (OFL); include its licence in `app/src/main/assets/licences/`.
- `icons/WeatherIcons.kt`: `fun weatherIcon(condition: Condition, isDay: Boolean, showers: Boolean = false): ImageVector` for the 11 icons in `mockups/all-screens.html`, ported from the SVG paths and colours there.
- `main/MainScreen.kt`, `main/Hero.kt`, `main/ForecastRow.kt`, `main/DailyList.kt`, `main/DetailTiles.kt`, `main/CreditsLine.kt`
- `main/StateScreens.kt`: welcome, loading (Material 3 `LoadingIndicator`), location refused, service error, offline banner.
- `main/MainViewModel.kt`, `main/MainUiState.kt`
- `main/Format.kt`: temperatures, times, dates, AQI labels.

Tests go in `app/src/test/java/com/iwankurnia/weather/ui/main/`, covering `FormatTest` and `MainViewModelTest`.

**Interfaces:**
- Consumes:
  - `WeatherRepository` and `LoadResult` from Task 1.
  - `SampleData` for previews.
  - `WeatherBackground` and `cardTint` from Task 4. Until Task 4 is merged, use a local stub with the same signature in `ui/main/BackgroundStub.kt`, and delete the stub at integration.
- Produces:
  - `@Composable fun WeatherRoute(viewModel: MainViewModel, onRequestLocation: () -> Unit, onOpenSettings: () -> Unit)`
  - `class MainViewModel(repo: WeatherRepository) : ViewModel` exposing `val state: StateFlow<MainUiState>` and `fun refresh(force: Boolean)`
  - `sealed interface MainUiState { Loading; Welcome; LocationDenied; Error(message); Ready(snapshot, staleSince: Instant?) }`

- [ ] **Step 1: Write the failing tests:**
  - `format_temperature`: 33.4 → "33°", −0.4 → "0°".
  - `format_date`: 2026-11-01 → "Sunday, 1 November".
  - `format_aqiLabel`: 0–50 → "Good", 51–100 → "Moderate", 101–150 → "Unhealthy for sensitive groups", 151–200 → "Unhealthy", 201–300 → "Very unhealthy", 301+ → "Hazardous".
  - `format_uvLabel`: 0–2 Low, 3–5 Moderate, 6–7 High, 8–10 Very high, 11+ Extreme.
  - `viewModel_mapsLoadResults`: Fresh → Ready(staleSince = null), Stale → Ready(staleSince = fetchedAt), NoLocation(true) → LocationDenied, Failed → Error. Use a fake repository and `StandardTestDispatcher`.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*ui.main*'`. Expected: FAIL.
- [ ] **Step 3: Implement the screens to match `mockups/app-screen.html` and `mockups/all-screens.html`:**
  - **Top bar:** location chip and refresh button (no settings).
  - **Hero:** temperature, condition and high/low, with "feels like" in `MaterialShapes.Cookie9Sided`.
  - **Forecast card:** summary, then pill steps, with "Now" highlighted.
  - **10-day list:** a grouped list with outer corners of 24 dp and inner corners of 6 dp.
  - **Detail tiles:** UV in `MaterialShapes.Sunny`, air quality in `MaterialShapes.Clover4Leaf` labelled "modelled" when `modelled`, humidity fill, wind dial, rainfall, and the sunrise/sunset arc.
  - **Credits line:** at the bottom.
  - **Behaviour:** pull to refresh. Cards use `cardTint`.
- [ ] **Step 4:** Run the same command. Expected: PASS. Add `@Preview`s with `SampleData.dayPartlyCloudy` and `SampleData.nightRain`.
- [ ] **Step 5:** Commit: `feat: main screen, states and view model`.

---

### Task 6: Repository, cache and widgets (parallel, branch `feat/widget-repo`)

**Files:**
- Create in `app/src/main/java/com/iwankurnia/weather/data/`:
  - `DefaultWeatherRepository.kt`
  - `SnapshotCache.kt`: a JSON file in `filesDir` via kotlinx.serialization, with `@Serializable` mirror DTOs kept in this file.
  - `BmkgThrottle.kt`: skip BMKG and reuse the cached BMKG forecast when it was fetched less than 60 minutes ago.
- Create in `app/src/main/java/com/iwankurnia/weather/widget/`:
  - `LargeWidget.kt`, `SmallWidget.kt`, `LargeWidgetReceiver.kt`, `SmallWidgetReceiver.kt`
  - `WidgetPrefs.kt`: per-widget opacity in DataStore under key `opacity_<appWidgetId>`.
  - `WidgetConfigActivity.kt`: a background-location explanation sheet, then the opacity slider with presets 0/30/60/100 and a live preview, then Done.
  - `WidgetUpdateWorker.kt`: unique periodic work `"widget-refresh"`, every 30 minutes, requiring a network connection.
- Tests: `app/src/test/java/com/iwankurnia/weather/data/DefaultWeatherRepositoryTest.kt` (fakes for every Task 1 interface) and `widget/WidgetStyleTest.kt`.

**Interfaces:**
- Consumes:
  - All Task 1 interfaces.
  - `SnapshotMerger.merge` from Task 3. Until it's merged, inject the merge as a function parameter `merge: (Place, BmkgForecast?, OpenMeteoForecast, AirQuality?, LocalDateTime, Instant) -> WeatherSnapshot`. Integration then passes `SnapshotMerger::merge`.
- Produces:
  - `DefaultWeatherRepository(location, areaNamer, kelurahan, bmkg, openMeteo, air, cache, merge, clock: java.time.Clock)`
  - `fun widgetTextStyle(opacityPct: Int): WidgetTextStyle` (white with a shadow at 40 or lower)
  - `object WidgetUpdater { fun schedule(context: Context); suspend fun updateAll(context: Context) }`

- [ ] **Step 1: Write the failing tests:**
  - `repository_bmkgInsideCoverage`: the nearest kelurahan is found, BMKG is called with its code, and the result is `Fresh` with `source BMKG`.
  - `repository_outsideCoverage_usesOpenMeteo`: the index returns null, BMKG is never called, and `merge` receives `bmkg = null`.
  - `repository_bmkgFails_fallsBack`: when the BMKG client throws, `merge` receives null.
  - `repository_waqiMissing_usesModelledAir`: when the air client returns null, `merge` receives `air = null`.
  - `repository_bmkgThrottled`: a second load within 60 minutes doesn't call BMKG and reuses the cached forecast.
  - `repository_offlineWithCache_returnsStale`: Open-Meteo throws and a cache exists, so the result is `Stale`.
  - `repository_firstFetchFails_returnsError`: Open-Meteo throws and there's no cache, so the result is `Failed`.
  - `repository_noPermission_returnsNoLocation`: when the location is null and no cache exists, the result is `NoLocation(true)`.
  - `widgetStyle_thresholds`: 41 → normal, 40 → white with shadow, 0 → white with shadow.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest --tests '*data.DefaultWeatherRepository*' --tests '*widget*'`. Expected: FAIL.
- [ ] **Step 3: Implement the repository.** The cache is written after every successful merge. `areaName` comes from `AreaNamer`, falling back to the kelurahan name, then to "Your location".
- [ ] **Step 4: Implement the widgets to match `mockups/widget.html`:**
  - **Colours:** `GlanceTheme` with dynamic colours.
  - **Large widget:** the top row has the area on the left and "Partly cloudy · H34° L25°" on the right. Below that, time and date on the left (`TextClock` via `AndroidRemoteViews`, or a minute-tick update), and on the right the temperature with icon plus a "Feels 39°" pill.
  - **Small widget:** area, temperature, "feels like" pill and condition.
  - **Opacity:** the background colour's alpha comes from the widget's opacity setting.
  - **Tap:** opens `MainActivity`.
  - **Background location refused:** when `ACCESS_BACKGROUND_LOCATION` isn't granted, the worker uses the cached place, and both widgets show a small "Location may be out of date" line in place of the area line.
- [ ] **Step 5:** Run the same test command plus `./gradlew :app:assembleDebug`. Expected: PASS and BUILD SUCCESSFUL.
- [ ] **Step 6:** Commit: `feat: repository with cache and fallbacks, widgets with opacity and background refresh`.

---

### Task 7: Integration (lead, after Tasks 2–6 merge)

**Files:**
- Modify: `MainActivity.kt` and `WeatherApplication.kt`. Use manual dependency wiring through an `AppGraph` object in `di/AppGraph.kt`, which builds the clients, index, repository and view model factory.
- Delete: `ui/main/BackgroundStub.kt`.
- Wire: `SnapshotMerger::merge` into the repository.
- Permissions: request coarse and fine location from the welcome screen. "Open settings" opens the app details settings.
- Schedule: call `WidgetUpdater.schedule` when the first widget is added and on app start if any widget exists.

- [ ] **Step 1:** Merge `feat/sources`, `feat/domain`, `feat/backgrounds`, `feat/main-ui` and `feat/widget-repo` into `main`, resolving conflicts in favour of the Task 1 contracts.
- [ ] **Step 2:** Run `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug`. Expected: every test passes (report the counts), the build succeeds, and lint shows no errors.
- [ ] **Step 3:** Install on an Android 16 emulator, grant location, and set the emulator's location to Kebayoran Baru (−6.2297, 106.7997). Expected:
  - The main screen shows BMKG data with credits.
  - Pull to refresh works.
  - Adding the large widget shows the explanation sheet, then the opacity sheet, then the widget.
- [ ] **Step 4:** Run an independent review of the whole branch with a fresh reviewer, then apply its findings.
- [ ] **Step 5:** Commit: `feat: wire data, UI and widgets together`. Push to `origin main`.
