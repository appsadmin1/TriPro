import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

// local.properties is NOT auto-exposed to Gradle the way gradle.properties is (Android
// Studio only auto-reads sdk.dir from it) — load it ourselves so MAPS_API_KEY and
// WEB_CLIENT_ID can live there instead of in a committed file. See local.properties.example.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun secret(key: String): String =
    (localProperties.getProperty(key) ?: System.getenv(key) ?: "").also {
        if (it.isEmpty()) logger.warn("⚠\uFE0F  $key is not set in local.properties — see local.properties.example")
    }

android {
    namespace = "com.tripro.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tripro.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["MAPS_API_KEY"] = secret("MAPS_API_KEY")
        // Same key, also exposed as a BuildConfig string so Kotlin code can call
        // Places.initialize(...) at runtime (see TriProApplication) — the manifest
        // placeholder above only reaches the Maps SDK's own <meta-data> tag, not code.
        buildConfigField("String", "MAPS_API_KEY", "\"${secret("MAPS_API_KEY")}\"")
        buildConfigField("String", "WEB_CLIENT_ID", "\"${secret("WEB_CLIENT_ID")}\"")
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"${secret("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"${secret("CLOUDINARY_UPLOAD_PRESET")}\"")
        buildConfigField("String", "NETLIFY_FUNCTIONS_BASE_URL", "\"${secret("NETLIFY_FUNCTIONS_BASE_URL")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    debugImplementation(libs.ui.tooling)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)

    // Auth (Credential Manager is the current recommended replacement for
    // the deprecated GoogleSignInClient API)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    // Firebase (Auth + Firestore for realtime sync + Cloud Messaging for push notifications)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)

    // Maps + Places (Places powers the "search on Google Maps" pickers for hotels,
    // airports, and itinerary stops — see util/PlacesAutocomplete.kt. It's a separate
    // artifact/API from the Maps SDK even though they share one Cloud project/key.)
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.places)
    implementation(libs.play.services.location)
    implementation(libs.kotlinx.coroutines.play.services)

    // Weather (Open-Meteo, plain REST call) + image loading
    implementation(libs.okhttp)
    implementation(libs.coil.compose)

    implementation(libs.core.splashscreen)

    // Per-app language support (Settings > System > Languages > App languages on API 33+,
    // with a compatible polyfill below that) — see util/LocaleUtils.kt. Replaces a manual
    // attachBaseContext()/createConfigurationContext() locale override, which correctly
    // flipped Compose's layout direction but did not reliably make stringResource()
    // resolve values-he/ — a known-fragile pattern, especially on Samsung One UI.
    implementation(libs.appcompat)
}
