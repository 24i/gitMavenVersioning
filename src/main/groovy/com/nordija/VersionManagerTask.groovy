package com.nordija

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

class VersionManagerTask extends DefaultTask {
    String greeting = 'hello from GreetingTask'
    String branch = "";
    String closestHighestTagHash;
    String closestTag;
    String closestTagCount;
    String currentShortCommitHash;
    String currentCommitHash;
    String mavenVersion;
    String gitDescribe;
    boolean snapshot = true;

    @TaskAction
    def findGitVersions() {
        findBranch()
        findClosestTagHash()
        findGitClosestTag();
        findCurrentCommitHash();
        findCurrentCommitShortHash();
        findCountFromClosestTagHash();
        findMavenVersion();
        findGitDescribeVersion();
        setVersions();
    }

    void setVersions() {
        System.setProperty("gitBranch",branch);
        System.setProperty("gitHighestTagHash",closestHighestTagHash);
        System.setProperty("gitHighestTag",closestTag);
        System.setProperty("gitHighestTagCount",closestTagCount);
        System.setProperty("gitCurrentShortCommitHash",currentShortCommitHash);
        System.setProperty("gitCurrentCommitHash",currentCommitHash);
        System.setProperty("mavenVersion",mavenVersion);
        System.setProperty("gitDescribe",gitDescribe);
        System.setProperty("versionSnapshot",''+snapshot);
        if (mavenVersion != null) {
            getProject().version = mavenVersion
        }


    }

    void findCurrentCommitShortHash() {
        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()
        try {
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'rev-parse', '--short','HEAD'
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            currentShortCommitHash =  stdout.toString().trim()
        }
        catch (ignored) {
            currentShortCommitHash = "0";
        }
    }

    void findCurrentCommitHash() {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'rev-parse', 'HEAD'
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            currentCommitHash = stdout.toString().trim()
        }
        catch (ignored) {
            currentCommitHash = "NoHashFound";
        }
    }


    void findBranch() {
        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()
        try {
            ExecResult result = this.project.exec({
                it.executable = 'git'
                it.args = ['rev-parse', '--abbrev-ref', 'HEAD']
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            branch = stdout.toString().trim();
        } catch (ignored) {
            branch = "error";
        }
    }

    void findClosestTagHash() {
        try {
            def stderr = new ByteArrayOutputStream()
            def stdout = new ByteArrayOutputStream()
            if (branch.equals('master')) {
                ExecResult result = this.project.exec({
                    it.commandLine 'git','rev-list', '--tags', '--max-count=10';
                    it.standardOutput = stdout;
                    it.errorOutput = stderr;
                });
                def outputString = stdout.toString().trim();
                def hashes;
                if (outputString.contains('\n')) {
                    hashes = outputString.split('\n');
                } else {
                    hashes = [outputString];
                }
                def closestHighestTag = '0.0.0';
                def localClosestHighestTag = '0';
                for (String item : hashes) {
                    def foundTag = getClosestTagForHash(item);
                    if (compareVersions(foundTag, closestHighestTag) > 0) {
                        closestHighestTag = getClosestTagForHash(item);
                        localClosestHighestTag = item;
                    }
                }
                this.closestHighestTagHash = localClosestHighestTag;
            } else {
                ExecResult result = this.project.exec({
                    it.commandLine 'git','rev-list', '--tags', '--max-count=1'
                    it.standardOutput = stdout
                    it.errorOutput = stderr;
                });
                this.closestHighestTagHash = stdout.toString().trim()
            }
        }
        catch (ignored) {
            this.closestHighestTagHash = "0";
        }
    }

    void findGitClosestTag () {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'describe', '--tags', closestHighestTagHash
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            closestTag = stdout.toString().trim()
        }
        catch (ignored) {
            closestTag = "0.0.0";
        }

    }

    void findCountFromClosestTagHash()  {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'rev-list', closestHighestTagHash+'..', '--count'
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            closestTagCount = stdout.toString().trim()
        }
        catch (ignored) {
            closestTagCount =  "0";
        }
    }

    void findMavenVersion() {
        def closestTag = closestTag;
        def gitBranch = branch;
        def versionSplit = /([0-9]+).([0-9]+).([0-9]+).*/;
        def matcher = ( closestTag =~ versionSplit );
        if (closestHighestTagHash.equals(currentCommitHash)) {
            mavenVersion = closestTag;
            snapshot = false;
        } else {
            def major = matcher[0][1];
            def minor = matcher[0][2];
            def bugfix = matcher[0][3];

            if (gitBranch.equals("master")) {
                minor = minor.toLong() + 1;
                bugfix = "0-SNAPSHOT";
            } else if (gitBranch.startsWith("bugfix")) {
                bugfix = (bugfix.toLong() + 1) + "-SNAPSHOT";
            } else {
                def startIdx = 0;
                def endIdx = 10;
                if (gitBranch == null) {
                    gitBranch = "UNKNOWN";
                } else if (gitBranch.length() >= 11) {
                    if (gitBranch.startsWith("SPRINT-")) {
                        startIdx = 6;
                    }
                    gitBranch = gitBranch.substring(startIdx, endIdx);
                } else {
                    gitBranch = gitBranch.substring(startIdx, gitBranch.length());
                }
                minor = minor.toLong() + 1;
                bugfix = "0-" + gitBranch + "-SNAPSHOT";
            }
            mavenVersion = (major +
                    "." +
                    minor +
                    "." +
                    bugfix);
        }
    }

    void findGitDescribeVersion() {
        if (currentCommitHash.equals(closestHighestTagHash)) {
            gitDescribe = closestTag;
        } else {
            if (branch.equals('master')) {
                gitDescribe = getMavenVersion()+ '-'+ currentShortCommitHash;
            } else {
                gitDescribe = getMavenVersion()+ '-' + closestTagCount +'-'+ currentShortCommitHash;
            }
        }

    }


    String getClosestTagForHash( hash ) {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'describe', '--tags', hash;
                it.standardOutput = stdout;
                it.errorOutput = stderr
            });
            return stdout.toString().trim();
        } catch (ignored) {
            return "0.0.0";
        }
    }

    boolean isProjectDirty() {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'status';
                it.standardOutput = stdout;
                it.errorOutput = stderr
            });
            return !stdout.toString().trim().contains('nothing to commit, working directory clean');
        } catch (ignored) {
            return false;
        }
    }

    int compareVersions(String v1, String v2) {

        if (v1.length() > 0  && v2.length() == 0) return -1;
        if (v1.length() == 0 && v2.length() == 0) return 0;
        if (v1.length() == 0 && v2.length() < 0) return 1;

        int pos1 = v1.indexOf('.');
        int pos2 = v2.indexOf('.');

        Integer num1 = (pos1 > 0 ? Integer.valueOf(v1.substring(0, pos1)) : 0);
        Integer num2 = (pos2 > 0 ? Integer.valueOf(v2.substring(0, pos2)) : 0);

        if (num1 != num2) return num1.compareTo(num2);

        String tail1 = (pos1 > 0 ? v1.substring(pos1 + 1, v1.length()) : "");
        String tail2 = (pos2 > 0 ? v2.substring(pos2 + 1, v2.length()) : "");

        return compareVersions(tail1, tail2);
    }
}
