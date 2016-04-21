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
        project.task('findVersion') {
            group = 'task'
            description = 'Find the version from git system'
        }
        project.tasks.showVersion << {
            println "Version: " + project.version
        }
        project.tasks.findVersion << {
            project.tasks.version.execute();
            println "Version: " + project.version
        }

    }
}
