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
3. **Hourly card:** a one-line summary (for example "Rain likely from around 16:00. Very hot until then."), then a row of pill-shaped hourly items showing the time, temperature, an icon and chance of rain. "Now" is highlighted.
4. **10-day forecast:** a grouped list in the Android 16 Settings style (one rounded group with small gaps between rows). Each row shows the day, an icon, chance of rain, the low, a range bar and the high.
5. **Detail tiles**, two per row:
   - UV index, in a sun-shaped badge.
   - Air quality, in a four-lobed badge.
   - Humidity, with a fill level and the dew point.
   - Wind, with a direction dial.
   - Rainfall in the last 24 hours, plus the amount expected tomorrow.
   - Sunset and sunrise, with an arc showing where the sun is now.

   Visibility appeared in an early mockup and is optional.

**Look:**
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
| Rain | blue-grey | very dark blue-grey | falling rain |
| Thunderstorm | dark slate | near black | rain plus an occasional lightning flash |
| Haze / fog | pale beige-grey | dark warm grey | slow drifting mist |

- **Day or night:** day runs from local sunrise to sunset.
- **Haze:** included because it is common in Jakarta.
- **Temperature:** never affects the background.
- **Motion:** gentle, and it must respect the phone's "remove animations" setting.

## Widget

Agreed mockup: `mockups/widget.html` (option C, without the hourly row). It takes all its colours from the phone's system colour scheme, which Android derives from the wallpaper, and follows light and dark mode. It does **not** use the weather backgrounds.

- **Large (4×2):** a rounded card. The top line has the area name on the left (as in the small widget) and "Partly cloudy · H34° L25°" on the right. Below that, the time ("12:41") and date ("Sunday, 1 November") are on the left. The current temperature and condition icon are on the right, with a "Feels 39°" pill under them.
- **Small (2×2):** a rounded card with the area name, the temperature, a "Feels 39° ⛅" pill and the condition.
- Tapping either widget opens the app.
- **Background opacity (Iwan's request):** each widget has its own opacity setting, from 0% (text only) to 100% (solid), defaulting to 100%. A small settings sheet with a slider opens when the widget is added. To change it later, long-press the widget and use Android's reconfigure (pencil) option. The mockup shows 100%, 60%, 0% and the sheet.
  - *Claude's call:* at 40% or lower, the text switches to white with a soft shadow so it stays readable on the wallpaper.

## Location (Iwan's decision)

The widget always shows the phone's current area, even when the app hasn't been opened for hours. The app therefore asks for "Allow all the time" location access as well as normal location access.

- **When to ask:** following Android's rules, the app first asks for normal location access when it opens. It asks for "all the time" separately, with a short explanation, when the first widget is added.
- **If "all the time" is refused:** the widget falls back to the last location the app saw. It shows a small "Location may be out of date" hint until the permission is granted.
- **Accuracy:** approximate (city-district level) is enough for weather and is gentler on the battery. *Claude's call.*

## Units and formats

Celsius, km/h, mm, 24-hour clock, dates such as "Sunday, 1 November", in English.

## Decided by Claude (Iwan can veto)

- **Build tools:** Kotlin and Jetpack Compose with Material 3, matching his existing Android project (`~/Code/jk-bms-monitor`). The widget is built with Jetpack Glance.
- **Supported Android versions:** Android 12 (API 31) and newer, because that's where system colours from the wallpaper start. It targets the newest Android version. Requiring Android 16 instead wouldn't save any work, because nothing here needs a fallback for older versions.
- **Weather data:** Open-Meteo. It is free, needs no API key or account, and provides the "feels like" temperature, weather codes, day/night, hourly and 16-day forecasts, UV, sunrise and sunset, and air quality (US AQI scale, which matches the "156 Unhealthy" style in the mockup).
- **Mapping weather codes to the six backgrounds:** Open-Meteo has no "haze" code. Fog codes 45 and 48 show as haze/fog. Haze is also shown when it is not raining and visibility is under 5 km.
- **Area name:** taken from the phone's own address lookup (Android `Geocoder`) using the current coordinates.
- **Freshness:** the app fetches when opened and when you pull to refresh. The widget refreshes on a schedule of roughly every 30 minutes.
- **Offline:** if there is no connection, show the last fetched weather with an "Updated hh:mm" note, never an empty screen.
- **Installation:** directly from Android Studio or `adb`, no Play Store listing.
- **No settings screen.** Units and formats are fixed, so the settings button is dropped. Open-Meteo's required credit ("Weather data by Open-Meteo.com") and the "Updated hh:mm" time go in a small line at the bottom of the main screen.
- **Hourly summary line:** looks ahead over the next 12 hours. The wording rules are:
  - **Rain:** a chance of 50% or more counts as rain being "likely". The line names the first such hour ("Rain likely from around 16:00").
  - **Heat:** a "feels like" of 38° or more counts as "very hot". The line says how long that lasts ("Very hot until 15:00").
  - **Neither:** the line says something plain, for example "Dry and warm for the next few hours."
  - **Tuning:** the 50% and 38° thresholds sit in one place so they are easy to adjust after a week of use.
## Left to the builder

Package name, architecture inside the app, exact colour values (the mockups are guides, not specs), the icon set, and animation details. Testing approach should follow Iwan's usual workflow.
