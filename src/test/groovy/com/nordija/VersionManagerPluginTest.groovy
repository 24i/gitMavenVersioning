package com.nordija

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class VersionManagerPluginTest {
    @Test
    public void greeterPluginAddsGreetingTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.nordija.versionManager'

        assertTrue(project.tasks.version instanceof VersionManagerTask)
    }
}
