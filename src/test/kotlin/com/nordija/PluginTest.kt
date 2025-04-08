package com.nordija

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class PluginTest {

    companion object {
        private const val TASK_NAME = "version"
        private const val PLUGIN_ID_GIT_VERSION = "com.24i.gitVersion"
        private const val PLUGIN_ID_GIT_MAVEN_VERSION = "com.nordija.versionManager"
    }

    @Test
    fun testGitVersion() {
        val project: Project = ProjectBuilder.builder().build()
        project.plugins.apply(PLUGIN_ID_GIT_VERSION)
        val hasTask = project.tasks.findByName(TASK_NAME) != null
        assert(hasTask, { "No task $TASK_NAME found." })
    }

    @Test
    fun testGitMavenVersion() {
        val project = ProjectBuilder.builder().build()
        project.getPlugins().apply(PLUGIN_ID_GIT_MAVEN_VERSION)
        val hasTask = project.tasks.findByName(TASK_NAME) != null
        assert(hasTask, { "No task $TASK_NAME found." })
    }
}