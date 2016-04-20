package com.nordija

import org.gradle.api.Project
import org.gradle.api.Plugin

class VersionManagerPlugin implements Plugin<Project> {
    Project project;

    void apply(Project target) {
        this.project = target;
        target.task('version', type: VersionManagerTask)
        project.task('showVersion') {
            group = 'Help'
            description = 'Show the project version'
        }
        project.tasks.showVersion << {
            println "Version: " + project.version
        }
    }
}
