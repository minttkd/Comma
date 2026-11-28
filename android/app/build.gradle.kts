plugins {
    id("com.android.application")
}

android {
    namespace = "com.shimpyo.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.shimpyo.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 25
        versionName = "0.25"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
