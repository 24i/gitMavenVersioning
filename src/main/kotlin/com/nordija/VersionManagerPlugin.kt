package com.nordija

import com.github.javaparser.utils.Log
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class VersionManagerPlugin : Plugin<Project> {
    companion object {
        const val TASK_GROUP = "24i"
        const val TASK_VERSION_NAME = "version"
        const val TASK_VERSION_DESCRIPTION = "Versioning task"
        const val TASK_SHOW_VERSION_NAME = "showVersion"
        const val TASK_SHOW_VERSION_DESCRIPTION = "Show the project version"
        const val TASK_FIND_VERSION_NAME = "findVersion"
        const val TASK_FIND_VERSION_DESCRIPTION = "Find the version from git system"
        const val TASK_PRINT_VERSION_NAME = "printVersion"
        const val TASK_PRINT_VERSION_DESCRIPTION = "Print the version from git system"

        private val Project.isCI : Boolean
            get() = if (hasProperty("CI")) parseBoolean(properties["CI"].toString())
            else false

        fun parseBoolean(
            input: String,
            default: Boolean = false
        ): Boolean = runCatching {
            java.lang.Boolean.parseBoolean(input)
        }.onFailure { e ->
            Log.error("E: ${e.message}")
        }.getOrDefault(default)
    }

    override fun apply(target: Project) {
        with(target) {

            // create main task
            val mainTask = tasks.register(
                TASK_VERSION_NAME,
                VersionManagerTask::class.java
            ).apply {
                group = TASK_GROUP
                description = TASK_VERSION_DESCRIPTION
                configure {
                    isCI = target.isCI
                }
            }

            // all tasks in project depends on main task
            allprojects.forEach { proj: Project ->
                if (proj.name != "buildSrc") {
                    proj.tasks.forEach { task: Task ->
                        val isMainTask = (task == mainTask) || task.name
                            .replace(":", "")
                            .contentEquals(TASK_VERSION_NAME)
                        if (!isMainTask) {
                            logger.debug("Task ${task.name} depends on ${mainTask.name}.")
                            task.dependsOn(mainTask)
                        }
                    }
                }
            }

            // create show version task
            tasks.register(TASK_SHOW_VERSION_NAME) {
                group = TASK_GROUP
                description = TASK_SHOW_VERSION_DESCRIPTION
                doLast {
                    println("Version (project.version): " + project.version)
                    println("Branch (System.properties.gitBranch): " + System.getProperty("gitBranch"))
                    println(
                        "Parent Branch (System.properties.gitParentBranch): " + System.getProperty(
                            "gitParentBranch"
                        )
                    )
                    println(
                        "Highest tag hash (System.properties.gitHighestTagHash): " + System.getProperty(
                            "gitHighestTagHash"
                        )
                    )
                    println("Highest tag (System.properties.gitHighestTag): " + System.getProperty("gitHighestTag"))
                    println(
                        "Highest tag count (System.properties.gitHighestTagCount): " + System.getProperty(
                            "gitHighestTagCount"
                        )
                    )
                    println(
                        "Current commit short hash (System.properties.gitCurrentShortCommitHash): " + System.getProperty(
                            "gitCurrentShortCommitHash"
                        )
                    )
                    println(
                        "Current commit hash (System.properties.gitCurrentCommitHash): " + System.getProperty(
                            "gitCurrentCommitHash"
                        )
                    )
                    println("Derived values based on above information: ")
                    println(
                        "Use in maven version and gradle version (System.properties.mavenVersion): " + System.getProperty(
                            "mavenVersion"
                        )
                    )
                    println(
                        "Use in app version (System.properties.appVersion): " + System.getProperty(
                            "appVersion"
                        )
                    )
                    println(
                        "Use as part of artifact name (System.properties.gitDescribe): " + System.getProperty(
                            "gitDescribe"
                        )
                    )
                    println(
                        "Use as part of artifact name (System.properties.gitAppDescribe): " + System.getProperty(
                            "gitAppDescribe"
                        )
                    )
                    println(
                        "Use as versionGitNumber (System.properties.gitPaddedVersionCount): " + System.getProperty(
                            "gitPaddedVersionCount"
                        )
                    )
                    println(
                        "Use as part of artifact name (System.properties.versionSnapshot): " + System.getProperty(
                            "versionSnapshot"
                        )
                    )
                }
            }

            // create find version task
            tasks.register(TASK_FIND_VERSION_NAME) {
                group = TASK_GROUP
                description = TASK_FIND_VERSION_DESCRIPTION
                // todo : not implemented in previous source
            }

            // create print version task
            tasks.register(TASK_PRINT_VERSION_NAME) {
                group = TASK_GROUP
                description = TASK_PRINT_VERSION_DESCRIPTION
                doLast {
                    println("Version (project.version): " + project.version)
                }
            }
        }
    }
}