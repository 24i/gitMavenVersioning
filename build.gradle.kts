import com.nordija.VersionManagerPlugin
// -------------------------------------------------------------------------------------------------
plugins.apply(VersionManagerPlugin::class)
// -------------------------------------------------------------------------------------------------
buildscript {
    dependencies {
        classpath(libs.git.maven.versioning)
        classpath(libs.jfrog.buildinfo)
    }
}
// -------------------------------------------------------------------------------------------------
plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    alias(libs.plugins.jfrog.artifactory)
}
kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}
dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(gradleKotlinDsl())
    testImplementation(libs.junit)
}
// -------------------------------------------------------------------------------------------------
fun getVersionFromTag(): String = runCatching {
    val props = System.getProperties()
    props["gitHighestTag"].toString()
}.getOrNull() ?: "0.0.0"
// -------------------------------------------------------------------------------------------------
val pluginGroup = "com.nordija"
val pluginVersion = getVersionFromTag()
val pluginName = "versionManager"
val pluginDescription = "Versioning plugin 24i"
// -------------------------------------------------------------------------------------------------
val pluginId = "$pluginGroup.$pluginName"
val pluginClass = "com.nordija.VersionManagerPlugin"
val gradleUserName = project.property("gradleUsername") as String
val gradlePassword = project.property("gradlePassword") as String
// -------------------------------------------------------------------------------------------------
group = pluginGroup
version = pluginVersion
// -------------------------------------------------------------------------------------------------
gradlePlugin {
    plugins {
        register(pluginName) {
            id = pluginId
            implementationClass = pluginClass
            displayName = pluginName
            description = pluginDescription
        }
    }
}
// -------------------------------------------------------------------------------------------------
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.jar.get()) {
                classifier = "sources"
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
artifactory {
    setContextUrl("https://aminocom2.jfrog.io/artifactory/")
    publish {
        repository {
            repoKey = "fokuson-public-release-local"
            username = gradleUserName
            password = gradlePassword
            setVersion(pluginVersion)
        }
        defaults {
            publications("mavenJava")
        }
    }
}
// -------------------------------------------------------------------------------------------------