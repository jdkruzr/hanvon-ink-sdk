plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "dev.hanvon.ink.demo"
    compileSdk = 35
    defaultConfig {
        applicationId = "dev.hanvon.ink.demo"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "0.2.0-alpha05"
        testInstrumentationRunner = "dev.hanvon.ink.demo.InkLabInstrumentation"
    }
    buildTypes { release { isMinifyEnabled = false } }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation(project(":ink-sdk")) }
