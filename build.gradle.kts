// -------------------------------------------------------------------------------------------------
plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    id("com.jfrog.artifactory") version ("4.33.1")
}
kotlin {
    jvmToolchain(17)
}
dependencies {
    implementation(gradleApi())
    implementation(localGroovy())
    implementation(gradleKotlinDsl())
    testImplementation("junit:junit:4.11")
}
// -------------------------------------------------------------------------------------------------
val pluginGroup = "com.nordija"
val pluginVersion = "2.2.3"
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
            setMavenCompatible(true)
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
