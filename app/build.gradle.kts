import java.util.Properties

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

    val mapsProperties = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) file.inputStream().use { load(it) }
    }
    fun mapsKey(environment: String): String = providers.environmentVariable("MAPS_ANDROID_KEY_${environment.uppercase()}").orNull
        ?: mapsProperties.getProperty("maps.android.key.$environment", "")
    fun mapId(environment: String): String = providers.environmentVariable("MAPS_ANDROID_MAP_ID_${environment.uppercase()}").orNull
        ?: mapsProperties.getProperty("maps.android.map.id.$environment", "")

    // Endereços públicos do site de cada ambiente; variável de ambiente ou local.properties sobrescrevem.
    val defaultWebBase = mapOf("qa" to "https://web-app-ldi0.onrender.com", "prod" to "https://solarianetwork.site")
    fun webBase(environment: String): String = providers.environmentVariable("WEB_BASE_URL_${environment.uppercase()}").orNull
        ?: mapsProperties.getProperty("web.base.url.$environment", defaultWebBase[environment].orEmpty())

    flavorDimensions += "environment"
    productFlavors {
        create("qa") {
            dimension = "environment"
            manifestPlaceholders["mapsApiKey"] = mapsKey(name)
            buildConfigField("boolean", "MAPS_CONFIGURED", mapsKey(name).isNotBlank().toString())
            buildConfigField("String", "MAP_ID", "\"${mapId(name)}\"")
            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            resValue("string", "app_name", "Elos")
            buildConfigField("String", "ENVIRONMENT", "\"qa\"")
            manifestPlaceholders["authScheme"] = "solaria-qa"
            buildConfigField("String", "AUTH_REDIRECT_URI", "\"solaria-qa://auth/callback\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"https://api-auth-xw6o.onrender.com/\"")
            buildConfigField("String", "CORE_BASE_URL", "\"https://api-core-cqfn.onrender.com/\"")
            buildConfigField("String", "MESSENGER_BASE_URL", "\"https://api-messenger-5g2n.onrender.com/\"")
            buildConfigField("String", "AI_ASSISTANT_BASE_URL", "\"https://ai-assistant-5ljd.onrender.com/\"")
            buildConfigField("String", "NOTIFY_BASE_URL", "\"https://api-notify-i637.onrender.com/\"")
            buildConfigField("String", "GOOGLE_REGISTRY_BASE_URL", "\"https://google-registry.onrender.com/\"")
            buildConfigField("String", "WEB_BASE_URL", "\"${webBase(name)}\"")
        }
        create("prod") {
            dimension = "environment"
            manifestPlaceholders["mapsApiKey"] = mapsKey(name)
            buildConfigField("boolean", "MAPS_CONFIGURED", mapsKey(name).isNotBlank().toString())
            buildConfigField("String", "MAP_ID", "\"${mapId(name)}\"")
            resValue("string", "app_name", "Elos")
            buildConfigField("String", "ENVIRONMENT", "\"prod\"")
            manifestPlaceholders["authScheme"] = "solaria"
            buildConfigField("String", "AUTH_REDIRECT_URI", "\"solaria://auth/callback\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"https://auth.solarianetwork.site/\"")
            buildConfigField("String", "CORE_BASE_URL", "\"https://core.solarianetwork.site/\"")
            buildConfigField("String", "MESSENGER_BASE_URL", "\"https://messenger.solarianetwork.site/\"")
            buildConfigField("String", "AI_ASSISTANT_BASE_URL", "\"https://assistant.solarianetwork.site/\"")
            buildConfigField("String", "NOTIFY_BASE_URL", "\"https://notify.solarianetwork.site/\"")
            buildConfigField("String", "GOOGLE_REGISTRY_BASE_URL", "\"\"")
            buildConfigField("String", "WEB_BASE_URL", "\"${webBase(name)}\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.google.maps.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.haze)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.krossbow.stomp.core)
    implementation(libs.markdown.renderer.m3)
    implementation(libs.krossbow.websocket.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.graphics.shapes)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.installations)
    implementation(libs.firebase.messaging)
    implementation(libs.kotlinx.coroutines.play.services)
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
