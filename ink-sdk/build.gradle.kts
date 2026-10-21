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
    sourceSets.getByName("main").resources.srcDir(layout.buildDirectory.dir("licenseResources"))
    publishing { singleVariant("release") { withSourcesJar() } }
}
dependencies { testImplementation("junit:junit:4.13.2") }

val licenseResources by tasks.registering(Sync::class) {
    from(rootProject.file("LICENSE"), rootProject.file("NOTICE"))
    into(layout.buildDirectory.dir("licenseResources/META-INF/hanvon-ink-sdk"))
}
tasks.named("preBuild") { dependsOn(licenseResources) }
tasks.withType<Jar>().configureEach {
    if (name == "sourceReleaseJar") {
        from(rootProject.file("LICENSE"), rootProject.file("NOTICE"))
    }
}

group = "dev.hanvon.ink"
version = "0.2.1"
afterEvaluate {
    publishing {
        publications { create<MavenPublication>("release") {
            from(components["release"])
            artifactId = "ink-sdk"
            pom {
                name.set("Hanvon Ink SDK")
                description.set("Experimental native ink preview and retained-stroke support for compatible Hanvon firmware")
                url.set("https://github.com/jdkruzr/hanvon-ink-sdk")
                licenses { license {
                    name.set("Apache License, Version 2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    distribution.set("repo")
                } }
                scm {
                    url.set("https://github.com/jdkruzr/hanvon-ink-sdk")
                    connection.set("scm:git:https://github.com/jdkruzr/hanvon-ink-sdk.git")
                    developerConnection.set("scm:git:ssh://git@github.com/jdkruzr/hanvon-ink-sdk.git")
                    tag.set("v0.2.1")
                }
            }
        } }
        repositories { maven { name = "localArtifacts"; url = uri(rootProject.layout.projectDirectory.dir("artifacts/maven")) } }
    }
}
