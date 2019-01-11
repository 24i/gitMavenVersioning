package com.nordija

import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project
import static org.junit.Assert.*

class VersionManagerTaskTest {
    @Test
    public void canAddTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('testTask', type: VersionManagerTask)
        assertTrue(task instanceof VersionManagerTask)
    }
}
