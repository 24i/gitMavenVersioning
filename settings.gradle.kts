pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://aminocom2.jfrog.io/artifactory/FokusOnCentral") {
            val gradleUsername: String by settings
            val gradlePassword: String by settings
            credentials {
                username = gradleUsername
                password = gradlePassword
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://aminocom2.jfrog.io/artifactory/FokusOnCentral") {
            val gradleUsername: String by settings
            val gradlePassword: String by settings
            credentials {
                username = gradleUsername
                password = gradlePassword
            }
        }
    }
}

rootProject.name = "gitMavenVersioning"
