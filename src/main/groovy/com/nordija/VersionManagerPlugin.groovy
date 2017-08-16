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
        project.tasks.showVersion  {
            doLast {
                println "Version (project.version): " + project.version
                println "Branch (System.properties.gitBranch): " + System.properties.gitBranch;
                println "Parent Branch (System.properties.gitParentBranch): " + System.properties.gitParentBranch;
                println "Global commit count (System.properties.gitCommitCount): " + System.properties.gitCommitCount;
                println "Highest tag hash (System.properties.gitHighestTagHash): " + System.properties.gitHighestTagHash;
                println "Highest tag (System.properties.gitHighestTag): " + System.properties.gitHighestTag;
                println "Highest tag count (System.properties.gitHighestTagCount): " + System.properties.gitHighestTagCount;
                println "Current commit short hash (System.properties.gitCurrentShortCommitHash): " + System.properties.gitCurrentShortCommitHash;
                println "Current commit hash (System.properties.gitCurrentCommitHash): " + System.properties.gitCurrentCommitHash;
                println "Derived values based on above information: "
                println "Use in maven version and gradle version (System.properties.mavenVersion): " + System.properties.mavenVersion;
                println "Use in app version (System.properties.appVersion): " + System.properties.appVersion;
                println "Use as part of artifact name (System.properties.gitDescribe): " + System.properties.gitDescribe;
                println "Use as part of artifact name (System.properties.gitAppDescribe): " + System.properties.gitAppDescribe;
                println "Use as versionGitNumber (System.properties.gitPaddedVersionCount): " + System.properties.gitPaddedVersionCount;
                println "Use as part of artifact name (System.properties.versionSnapshot): " + System.properties.versionSnapshot;
            }
        }
        project.tasks.findVersion  {
            doLast {
                project.tasks.version.execute();
                println "Version: " + project.version
            }
        }

    }
}
