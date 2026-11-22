plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "rigatoni534.apps.md3"
    compileSdk = 37

    defaultConfig {
        minSdk = 30
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    // Exposed as api: consumers compile against these types (ColorScheme, Typography, shapes...).
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.core.ktx)
    api(libs.androidx.ui)
    api(libs.androidx.ui.graphics)
    api(libs.androidx.foundation)
    api(libs.androidx.compose.material3)
    api(libs.androidx.ui.text.google.fonts)
    api(libs.smooth.corner.rect.android.compose)
    // HCT / dynamic scheme utilities that back the seed and artwork colour APIs.
    api(libs.material)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
}
