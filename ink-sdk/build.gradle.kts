plugins { id("com.android.library"); id("org.jetbrains.kotlin.android"); id("maven-publish") }
android {
    namespace = "dev.hanvon.ink"
    compileSdk = 35
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    publishing { singleVariant("release") { withSourcesJar() } }
}
dependencies { testImplementation("junit:junit:4.13.2") }

group = "dev.hanvon.ink"
version = "0.2.0"
afterEvaluate {
    publishing {
        publications { create<MavenPublication>("release") {
            from(components["release"])
            artifactId = "ink-sdk"
        } }
        repositories { maven { name = "localArtifacts"; url = uri(rootProject.layout.projectDirectory.dir("artifacts/maven")) } }
    }
}
