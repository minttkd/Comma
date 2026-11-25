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
        versionCode = 12
        versionName = "0.12"
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
