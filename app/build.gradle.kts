plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.project.solaria_mobile"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.project.solaria_mobile"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    }

    flavorDimensions += "environment"
    productFlavors {
        create("qa") {
            dimension = "environment"
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            resValue("string", "app_name", "Elos QA")
            buildConfigField("String", "ENVIRONMENT", "\"qa\"")
            manifestPlaceholders["authScheme"] = "solaria-qa"
            buildConfigField("String", "AUTH_REDIRECT_URI", "\"solaria-qa://auth/callback\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"https://api-auth-xw6o.onrender.com/\"")
            buildConfigField("String", "CORE_BASE_URL", "\"https://api-core-cqfn.onrender.com/\"")
            buildConfigField("String", "MESSENGER_BASE_URL", "\"https://api-messenger-5g2n.onrender.com/\"")
            buildConfigField("String", "GOOGLE_REGISTRY_BASE_URL", "\"https://google-registry.onrender.com/\"")
        }
        create("prod") {
            dimension = "environment"
            resValue("string", "app_name", "Elos")
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            manifestPlaceholders["authScheme"] = "solaria"
            buildConfigField("String", "AUTH_REDIRECT_URI", "\"solaria://auth/callback\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"https://api-auth.solarianetwork.site/\"")
            buildConfigField("String", "CORE_BASE_URL", "\"https://api-core.solarianetwork.site/\"")
            buildConfigField("String", "MESSENGER_BASE_URL", "\"https://api-messenger.solarianetwork.site/\"")
            buildConfigField("String", "GOOGLE_REGISTRY_BASE_URL", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.lucide.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.graphics.shapes)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.installations)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

if (file("google-services.json").exists() || file("src/qa/google-services.json").exists() || file("src/prod/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
