# Weather app for Android: design

Date: 2026-11-01
Owner: Iwan
Status: approved by Iwan on 2026-11-01

## Why

Iwan has an Android 16 phone and no weather app on it, because the ones on the Play Store are full of ads. He wants his own clean, ad-free app. It has been very hot in Jakarta lately, so the two things he checks most are the current conditions and the temperature, and above all how hot it *feels*. He also wants a home-screen widget so he doesn't have to open the app.

It is for Iwan's own phone only. It is installed straight from the computer, not published to the Play Store.

## Goals

1. Show the weather for wherever the phone is right now, using its location.
2. Put "feels like" next to the real temperature, never hidden away.
3. Lay the content out like iPhone Weather, but make it look and feel like an Android 16 app (Material 3 Expressive).
4. Make the background show the current weather, with separate day and night versions.
5. Provide a home-screen widget in two sizes that follows the phone's own colour scheme.

## Non-goals (for now)

- Rain radar map.
- Saved cities or a list of locations.
- Severe-weather alerts and notifications.
- Play Store release, multiple users, or other languages.
- Phones older than the one he has. Older Android versions don't need to be supported.

## Anti-goals (would make it a failure even if it works)

- Any ads, tracking, analytics or sign-in.
- Colouring the background by temperature. Iwan tried this in the mockups and rejected it. The background shows the weather and time of day only.
- A frosted-glass iPhone look. The content follows iPhone Weather, but the styling must be Android 16.

## Main screen

Agreed mockup: `mockups/app-screen.html` (day: partly cloudy; night: rain). Full screen set with icons and every state: `mockups/all-screens.html`. The main screen is one long screen that scrolls from top to bottom:

1. **Top bar:** a location chip showing the local area (for example "Kebayoran Baru"), plus a round refresh button. The settings button in the mockup is dropped because there is nothing to set; see "Decided by Claude".
2. **Summary:** a large current temperature, the condition ("Partly cloudy"), and today's high and low. Next to them, "Feels like 39°" sits in a filled, scalloped "cookie" shape in the primary colour.
3. **Forecast card:** a one-line summary (for example "Rain likely from around 16:00. Very hot until then."), then a row of pill-shaped items in BMKG's 3-hour steps (Now, 16:00, 19:00, 22:00, …) showing the time, temperature, an icon and chance of rain. "Now" is highlighted. The row scrolls sideways across the next 24 hours.
4. **10-day forecast:** a grouped list in the Android 16 Settings style (one rounded group with small gaps between rows). Each row shows the day, an icon, chance of rain, the low, a range bar and the high. Days 1–3 come from BMKG and days 4–10 from Open-Meteo; see "Data sources".
5. **Detail tiles**, two per row:
   - UV index, in a sun-shaped badge.
   - Air quality, in a four-lobed badge.
   - Humidity, with a fill level and the dew point.
   - Wind, with a direction dial.
   - Rainfall in the last 24 hours (Open-Meteo), plus the amount expected tomorrow (BMKG).
   - Sunset and sunrise, with an arc showing where the sun is now.

   Visibility appeared in an early mockup and is optional.

**Look:**
- **Responsive text (Iwan's request):** all text scales with the window width against the 411 dp phone the mockups were drawn for. It goes down to 85% on small phones and split screen, and up to 125% on tablets and landscape. It multiplies with the phone's own font-size setting.
- **Typography:** Roboto Flex, with wide, bold numbers.
- **Cards:** solid, rounded (28 dp corners), and tinted to suit the background.
- **Hourly items:** pill-shaped.
- **Badges:** expressive shapes (cookie, sun, clover).
- **Card colours:** light cards on day backgrounds, dark cards on night backgrounds.

## Background: weather and time of day

Agreed mockup: `mockups/backgrounds-day-night.html`. There are six weather types, each with a day and a night version. Each one is a colour gradient with a subtle moving layer on top:

| Weather | Day | Night | Moving layer |
|---|---|---|---|
| Clear | bright blue sky | deep navy | glowing sun / stars and moon |
| Partly cloudy | softer blue | navy | a few drifting clouds (plus sun or moon) |
| Cloudy | grey-blue | dark slate | more, greyer clouds |
| Rain | blue-grey | very dark blue-grey | "falling rain", see below |
| Thunderstorm | dark slate | near black | "falling rain" with wind and lightning, see below |
| Haze / fog | darker beige-grey (white text must stay readable) | dark warm grey | slow drifting mist |

**Rain and thunderstorm: "falling rain" (Iwan's choice, 2026-11-01).** Agreed mockup: `mockups/rain-storm.html`. It replaces the flat diagonal stripes in the earlier mockups. Everything is drawn on a canvas behind the cards:
- **Clouds:** a band of heavy, dark clouds rolls slowly across the top of the screen. It is dimmer at night.
- **Rain:** falls in two layers, distant drops short, thin and slow, near drops longer, thicker and faster. Rain slants slightly with the wind.
- **Splashes:** near drops leave small splash ripples along the bottom edge.
- **Thunderstorm:**
  - More rain, slanting more steeply.
  - Every few seconds, a branching lightning bolt with a soft glow strikes from the top. It flickers once or twice, and a short flash lights the whole screen. The flash is cool-tinted at night.
- **The rain never covers the text.** It stays behind the cards.
- **Reduced motion:** with "Remove animations" on, the background shows a still frame (clouds and a few drops) and lightning is not shown.

- **Day or night:** day runs from local sunrise to sunset.
- **Haze:** included because it is common in Jakarta.
- **Temperature:** never affects the background.
- **Motion:** gentle, and it must respect the phone's "remove animations" setting.

## Widget

Agreed mockup: `mockups/widget.html` (option C, without the hourly row). It takes all its colours from the phone's system colour scheme, which Android derives from the wallpaper, and follows light and dark mode. It does **not** use the weather backgrounds.

- **Large (4×2):** a rounded card. The top line has the area name on the left (as in the small widget) and "Partly cloudy · H34° L25°" on the right. Below that, the time ("12:41") and date ("Sunday, 1 November") are on the left. The current temperature and condition icon are on the right, with a "Feels 39°" pill under them.
- **Small (2×2):** a rounded card with the area name, the temperature, a "Feels 39° ⛅" pill and the condition.
- Tapping either widget opens the app.
- **Responsive widgets (Iwan's request):** both widgets can be resized down to one cell, and they pick a layout from their real size.
  - **Large widget:** one row (time, icon, temperature, "feels like") at one cell tall. No condition text when narrow. Bigger type plus the forecast summary when roomy.
  - **Small widget:** icon and temperature at one cell wide. A single row at one cell tall. Bigger type plus today's high and low when roomy.
  - Cells under 120 dp count as one cell.
- **Background opacity (Iwan's request):** each widget has its own opacity setting, from 0% (text only) to 100% (solid), defaulting to 100%. A small settings sheet with a slider opens when the widget is added. To change it later, long-press the widget and use Android's reconfigure (pencil) option. The mockup shows 100%, 60%, 0% and the sheet.
  - *Claude's call:* at 40% or lower, the text switches to white with a soft shadow so it stays readable on the wallpaper.

## Location (Iwan's decision)

The widget always shows the phone's current area, even when the app hasn't been opened for hours. The app therefore asks for "Allow all the time" location access as well as normal location access.

- **When to ask:** following Android's rules, the app first asks for normal location access when it opens. It asks for "all the time" separately, with a short explanation, when the first widget is added.
- **If "all the time" is refused:** the widget falls back to the last location the app saw. It shows a small "Location may be out of date" hint until the permission is granted.
- **Accuracy:** approximate (city-district level) is enough for weather and is gentler on the battery. *Claude's call.*

## Data sources (Iwan's decision, 2026-11-01)

Chosen to fit Jakarta best: Indonesia's own weather agency first, with free global data filling the gaps and real station readings for air quality.

| What | Source | Notes |
|---|---|---|
| Conditions, temperature, humidity, wind, rain amount, days 1–3 | **BMKG open data** (`api.bmkg.go.id/publik/prakiraan-cuaca?adm4=<kelurahan code>`) | Official forecast for each kelurahan, 3-hour steps, 3 days, refreshed by BMKG twice a day. Free, no key. Limit 60 requests a minute. **BMKG must be credited** wherever its data appears. |
| Chance of rain, UV index, sunrise and sunset, days 4–10, rainfall in the last 24 hours | **Open-Meteo** | Free, no key. Global models only for Indonesia (about 9 km). |
| Air quality (US AQI) | **WAQI / aqicn.org** geo feed (`/feed/geo:<lat>;<lng>/`) | Real readings from the nearest monitoring station. A free token is needed, which Iwan requests himself. Free for personal, non-paid apps. **Credit the World Air Quality Index Project and the originating agency.** |

- **"Now":** BMKG has forecasts, not live readings. "Now" is the BMKG step covering the current time, with temperature and humidity interpolated between the neighbouring steps.
- **"Feels like":** BMKG doesn't provide it, so the app works it out from BMKG's temperature and humidity using the standard heat-index formula (NOAA). That keeps it consistent with the temperature shown, and it suits Jakarta's humid heat. When the heat index is below the temperature, it shows the temperature.
- **Today's high and low:** taken from today's BMKG steps.
- **Finding the kelurahan code:** the app ships a list of kelurahan/desa codes (BMKG's `adm4`, the Kemendagri area code) with their centre points, and picks the nearest one to the phone. Finding a suitable open dataset is a build task.
- **Fallbacks:**
  - If BMKG fails, has no data, or the phone is outside Indonesia (nearest kelurahan more than about 15 km away), Open-Meteo supplies everything. That includes its own "feels like" and its weather codes, where fog codes 45/48, or no rain with visibility under 5 km, count as haze.
  - If WAQI fails or there is no token, air quality comes from Open-Meteo (CAMS model) and the tile says "modelled".
- **Refresh:**
  - BMKG at most once an hour, because it only updates twice a day.
  - Open-Meteo and WAQI on every app open, pull to refresh, and widget refresh (about every 30 minutes).
- **Credits line** at the bottom of the main screen: "Weather: BMKG · Open-Meteo.com · Air quality: WAQI · Updated hh:mm".

**BMKG condition codes to the six backgrounds** (to check against BMKG's code list during the build):

| Background | BMKG codes |
|---|---|
| Clear | 0 Cerah |
| Partly cloudy | 1, 2 Cerah Berawan |
| Cloudy | 3 Berawan, 4 Berawan Tebal |
| Haze / fog | 5 Udara Kabur, 10 Asap, 45 Kabut |
| Rain | 60, 61, 63 Hujan; 80 Hujan Lokal |
| Thunderstorm | 95, 97 Hujan Petir |

## Units and formats

Celsius, km/h, mm, 24-hour clock, dates such as "Sunday, 1 November", in English.

## Decided by Claude (Iwan can veto)

- **Build tools:** Kotlin and Jetpack Compose with Material 3, matching his existing Android project (`~/Code/jk-bms-monitor`). The widget is built with Jetpack Glance.
- **Supported Android versions:** Android 12 (API 31) and newer, because that's where system colours from the wallpaper start. It targets the newest Android version. Requiring Android 16 instead wouldn't save any work, because nothing here needs a fallback for older versions.
- **Weather data:** superseded by Iwan's decision; see "Data sources".
- **Area name:** taken from the phone's own address lookup (Android `Geocoder`) using the current coordinates. If that returns nothing, the app uses BMKG's kelurahan name.
- **WAQI token:** kept in `local.properties`, which is never committed, and read at build time.
- **Freshness:** the app fetches when opened and when you pull to refresh. The widget refreshes on a schedule of roughly every 30 minutes.
- **Offline:** if there is no connection, show the last fetched weather with an "Updated hh:mm" note, never an empty screen.
- **Installation:** directly from Android Studio or `adb`, no Play Store listing.
- **No settings screen.** Units and formats are fixed, so the settings button is dropped. The data credits and the "Updated hh:mm" time go in a small line at the bottom of the main screen (see "Data sources").
- **Forecast summary line:** looks ahead over the next 12 hours (four BMKG steps). The wording rules are:
  - **Rain:** a step counts as rain being "likely" when BMKG forecasts rain or thunderstorm for it, or Open-Meteo's chance of rain at that time is 50% or more. The line names the first such step ("Rain likely from around 16:00").
  - **Heat:** a "feels like" of 38° or more counts as "very hot". The line says how long that lasts ("Very hot until 15:00").
  - **Neither:** the line says something plain, for example "Dry and warm for the next few hours."
  - **Tuning:** the 50% and 38° thresholds sit in one place so they are easy to adjust after a week of use.

## Left to the builder

Package name, architecture inside the app, exact colour values (the mockups are guides, not specs), the icon set, and animation details. Testing approach should follow Iwan's usual workflow.
