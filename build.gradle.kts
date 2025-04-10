import com.nordija.VersionManagerPlugin
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
plugins.apply(VersionManagerPlugin::class)
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
val userName = project.property("gradleUsername") as String
val password = project.property("gradlePassword") as String
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
artifactory {
    setContextUrl("https://aminocom2.jfrog.io/artifactory/")
    publish {
        repository {
            setRepoKey("fokuson-public-release-local")
            setUsername(userName)
            setPassword(password)
            setVersion(pluginVersion)
        }
        defaults {
            publications("mavenJava")
        }
    }
}
// -------------------------------------------------------------------------------------------------
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}
// -------------------------------------------------------------------------------------------------
