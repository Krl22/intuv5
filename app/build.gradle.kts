import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.intu.taxi"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.intu.taxi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    ndkVersion = "27.0.12077973"

    // Inject Mapbox token from local.properties into resources for v11
    val localProps = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    val mapboxPublicToken = localProps.getProperty("MAPBOX_PUBLIC_TOKEN") ?: ""
    val functionsRegion = localProps.getProperty("FUNCTIONS_REGION") ?: "us-central1"
    val functionsEmuHost = localProps.getProperty("FUNCTIONS_EMULATOR_HOST") ?: "10.0.2.2"
    val functionsEmuPort = localProps.getProperty("FUNCTIONS_EMULATOR_PORT") ?: "5001"
    val functionsEmuEnabled = (localProps.getProperty("FUNCTIONS_EMULATOR_ENABLED") ?: "false").lowercase() == "true"
    defaultConfig {
        resValue("string", "mapbox_access_token", mapboxPublicToken)
        resValue("string", "functions_region", functionsRegion)
        resValue("string", "functions_emulator_host", functionsEmuHost)
        resValue("string", "functions_emulator_port", functionsEmuPort)
        resValue("string", "functions_emulator_enabled", functionsEmuEnabled.toString())
        buildConfigField("boolean", "USE_FUNCTIONS_EMULATOR", functionsEmuEnabled.toString())
        buildConfigField("String", "APP_VERSION_TAG", "\"1.10\"")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.mapbox.maps.android)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation(libs.coil.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
