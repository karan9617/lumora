plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.noteaiapp.keyboardai"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.noteaiapp.keyboardai"
        minSdk = 24
        targetSdk = 35
        versionCode = 17
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,DEPENDENCIES}"
        }
        jniLibs {
            useLegacyPackaging = false  // Important for 16KB support
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.3.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.6.0")
    implementation("androidx.navigation:navigation-ui:2.6.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.activity:activity:1.9.2")
    implementation("com.prolificinteractive:material-calendarview:1.4.3")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-camera2:1.5.1") // Or the latest version
    implementation("androidx.camera:camera-lifecycle:1.5.1")
    implementation("androidx.camera:camera-view:1.5.1")
    implementation("com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava")
    // google drives
    implementation("com.google.apis:google-api-services-drive:v3-rev136-1.25.0")
    implementation("com.google.android.gms:play-services-drive:17.0.0")
// Note: This is a legacy API, but used here for simplicity. The modern approach uses the REST API with an HTTP client.
    implementation("com.google.api-client:google-api-client-android:1.23.0")
    implementation("com.github.bumptech.glide:glide:4.14.2")
    annotationProcessor("com.github.bumptech.glide:compiler:4.14.2")
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation(platform("com.google.firebase:firebase-bom:34.2.0"))
    implementation("com.google.firebase:firebase-analytics")

    // 1. Add the dependency for Firebase Authentication
    implementation("com.google.firebase:firebase-auth")

    // 2. Add the dependency for Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.1.0")
    implementation("com.google.firebase:firebase-firestore")

    // 2. For Cloud Storage (to store images)
    implementation("com.google.firebase:firebase-storage")
}