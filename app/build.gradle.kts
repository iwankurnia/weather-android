import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The WAQI token stays out of git: put `waqi.token=...` in local.properties.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val waqiToken = (localProps.getProperty("waqi.token") ?: "").trim()

// Version lives in version.properties; tools/build-apk.sh bumps buildNumber before every build.
val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties().apply {
    if (versionPropsFile.exists()) versionPropsFile.inputStream().use { load(it) }
}
val buildNumber = (versionProps.getProperty("buildNumber") ?: "1").trim().toInt()
val marketingVersion = (versionProps.getProperty("marketingVersion") ?: "1.0").trim()

android {
    namespace = "com.iwankurnia.weather"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.iwankurnia.weather"
        minSdk = 31
        targetSdk = 37
        versionCode = buildNumber
        versionName = "$marketingVersion.$buildNumber"
        buildConfigField("String", "WAQI_TOKEN", "\"$waqiToken\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // play-services pulls in fragment 1.1.0, too old for the activity-result APIs.
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.okhttp)
    implementation(libs.play.services.location)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

/**
 * Increments buildNumber in version.properties. Run it as its own Gradle invocation before
 * assembling: versionCode is read at configuration time. tools/build-apk.sh does both.
 */
tasks.register("bumpBuildNumber") {
    // Plain values only: the configuration cache can't serialise references into the script.
    val propsFile = versionPropsFile
    val current = buildNumber
    val marketing = marketingVersion
    doLast {
        val next = current + 1
        propsFile.writeText(
            "# The app's version, the single source of truth.\n" +
                "# marketingVersion: edited by hand when a release earns it.\n" +
                "# buildNumber: bumped on EVERY build by tools/build-apk.sh (via :app:bumpBuildNumber); it is the\n" +
                "#   APK's versionCode, and versionName is \"<marketingVersion>.<buildNumber>\".\n" +
                "marketingVersion=$marketing\n" +
                "buildNumber=$next\n",
        )
        println("build number -> $next (version $marketing.$next)")
    }
}
