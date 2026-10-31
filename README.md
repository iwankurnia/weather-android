# Weather

A personal, ad-free weather app for Android 16, built with Kotlin, Jetpack Compose and Jetpack Glance.

- Design: [docs/superpowers/specs/2026-11-01-weather-app-design.md](docs/superpowers/specs/2026-11-01-weather-app-design.md)
- Every screen and icon: open [docs/superpowers/specs/mockups/all-screens.html](docs/superpowers/specs/mockups/all-screens.html) in a browser.

Weather data by [BMKG](https://data.bmkg.go.id/) and [Open-Meteo.com](https://open-meteo.com/). Air quality by the [World Air Quality Index Project](https://aqicn.org/).

## Building

Open in Android Studio, or run `./gradlew :app:installDebug` with a phone connected.

- **APK:** run `tools/build-apk.sh`. Every run bumps the build number in `version.properties` (the APK's versionCode) and commits the bump. It then runs the tests, builds, checks the APK's version and copies it to `dist/weather-<version>.apk`.

- **Air quality token:** request a free token at <https://aqicn.org/data-platform/token/> and add `waqi.token=<your token>` to `local.properties`, which git ignores. Without it, air quality falls back to Open-Meteo's modelled reading.
- **Kelurahan list:** `app/src/main/assets/kelurahan.csv` lists the Jabodetabek kelurahan with BMKG's own coordinates, so the app can pick the nearest BMKG forecast. To rebuild it, download `db/wilayah.sql` from [cahyadsn/wilayah](https://github.com/cahyadsn/wilayah) (MIT), then run `python3 tools/build_kelurahan.py wilayah.sql app/src/main/assets/kelurahan.csv`. It takes about 30 minutes, because BMKG allows 60 requests a minute.
