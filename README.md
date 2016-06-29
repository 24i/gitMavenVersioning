# Build of this plugin
This is how to build the plugin and install it in your local repository

    gradlew clean install

To build from the build server and install it in artifactory

    gradlew clean artifactoryPublish

# Usage of the plugin
Include this in the **build.gradle** file

Example of buildscript addition:

    buildscript {
        repositories {
            mavenLocal()
            maven { url "https://ssl.nordija.com/artifactory/NordijaCentral"
                credentials {
                    username = gradleUsername
                    password = gradlePassword
                }
            }
            mavenCentral()
            maven { url "http://download.java.net/maven/2/" }
            maven { url "http://repo.maven.apache.org/maven2" }
        }
        dependencies {
            classpath group: 'com.nordija', name: 'gitMavenVersioning', version: '1.0.0'
        }
    }

Example of usage:

    apply plugin: 'com.nordija.versionManager'
    afterEvaluate {
        tasks.findVersion.execute()
    }

Two tasks can be called:
*  showVersion
*  findVersion

**showVersion** will display information about the values generated by the plugin.

**findVersion** will store all the information in the System.properties. all properties can be seen by showVersion