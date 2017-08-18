package com.nordija

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecResult

class VersionManagerTask extends DefaultTask {
    String branch = "";
    String parentBranch = "";
    String closestHighestTagHash;
    String closestTag;
    String closestTagCount;
    String commitCount;
    String currentShortCommitHash;
    String currentCommitHash;
    String mavenVersion;
    String appVersion;
    String gitDescribe;
    String gitAppDescribe;
    String gitPaddedVersionCount;
    boolean snapshot = true;

    @TaskAction
    def findGitVersions() {
//        fetch()
        findBranch()
        findCurrentCommitHash()
        findParentBranch()
        findCurrentCommitShortHash()
        findClosestTagHash()
        findGitClosestTag()
        findCountFromClosestTagHash()
        findCommitCount()
        findMavenVersion()
        findGitDescribeVersion()
        findGitAppDescribeVersion()
        setVersions()
    }

    private void fetch() {
        try {
            def stderr = new ByteArrayOutputStream()
            def stdout = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'fetch'
                it.standardOutput = stdout
                it.errorOutput = stderr;
            })
        } catch (Exception e) {
        }
    }

    private void findParentBranch() {
        if (!branch.equals('master') && !branch.startsWith("bugfix_")) {
            def foundHash = parentBranchCommitHash()
            if (foundHash == null || foundHash.isEmpty()) {
                foundHash = currentCommitHash
            }
            if (foundHash != null && !foundHash.isEmpty()) {
                def hashes = foundHash.split(' ')
                def parentBranchFound = ''
                for (String hash : hashes) {
                    String foundBranch = findLowestBranchForHash(hash)
                    if (foundBranch != null && !foundBranch.isEmpty() && !parentBranchFound.equals(foundBranch)) {
                        if (foundBranch.startsWith('bugfix')) {
                            parentBranchFound = findLowestBranch(foundBranch,parentBranchFound)
                        } else if (!parentBranchFound.startsWith('bugfix') && foundBranch.equals('master')) {
                            parentBranchFound = foundBranch
                        }
                    }
                }
                parentBranch = parentBranchFound
            }
        }
    }

    private String findLowestBranch(String branchA, String branchB) {
        if (branchA.equals('')) {
            return branchB
        }
        if (branchB.equals('')) {
            return branchA
        }
        if (branchA.startsWith('bugfix') && branchB.startsWith('bugfix')) {
            int compare = branchA.compareTo(branchB)
            if (comare < 0 || compare == 0) {
                return branchA
            }
            else {
                return branchB
            }
        } else if (branchA.startsWith('bugfix')) {
            return branchA
        } else if (branchB.startsWith('bugfix')) {
            return branchB
        }
        if (branchA.equals('master')) {
            return branchA
        }
        if (branchB.equals('master')) {
            return branchB
        }
        return ''
    }

    private String findLowestBranchForHash(String hash) {
        def stderr = new ByteArrayOutputStream()
        def stdout = new ByteArrayOutputStream()

        ExecResult result = this.project.exec({
            it.commandLine 'git', 'branch', '-r', '--contains', hash
            it.standardOutput = stdout
            it.errorOutput = stderr;
        })
        def outputString = stdout.toString().trim()
        def branches = outputString;
        if (outputString.contains('\n')) {
            branches = outputString.split('\n');
            def branchFound = ''
            for (final String item : branches) {
                if (item.startsWith("*")) {
                    branchFound = item.substring(1);
                } else {
                    branchFound = item
                }
                branchFound = branchFound.trim().replaceAll('origin\\/','')
                if (branchFound.startsWith('bugfix_') || branchFound.equals('master')) {
                    return branchFound
                }
            }
        } else {
            if (branches.startsWith('*')) {
                return branches.replaceAll('origin/','').substring(1).trim()
            }
            return outputString.replaceAll('origin/','').trim()
        }
        return '';
    }

    private String parentBranchCommitHash() {
        def stderr = new ByteArrayOutputStream()
        def stdout = new ByteArrayOutputStream()

        ExecResult result = this.project.exec({
            it.commandLine 'git', 'log', branch, '--not', 'origin/master', '--pretty=format:%P'
            it.standardOutput = stdout
            it.errorOutput = stderr;
        });
        def outputString = stdout.toString().trim();
        def hashes;
        def version = '';
        if (outputString.contains('\n')) {
            hashes = outputString.split('\n');
            for (String item : hashes) {
                version = item;
            }
        } else {
            version = outputString
        }
        return version;
    }


    void findCommitCount() {
        def stdout = new ByteArrayOutputStream()
        def stderr = new ByteArrayOutputStream()
        try {
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'rev-list', '--all','--count'
                it.standardOutput = stdout
                it.errorOutput = stderr
            });
            commitCount =  stdout.toString().trim()
        }
        catch (ignored) {
            commitCount = "0";
        }
        logger.debug("Found commit count: " + commitCount)
    }

    void setVersions() {
        System.setProperty("gitBranch",branch);
        System.setProperty("gitParentBranch",parentBranch);
        System.setProperty("gitHighestTagHash",closestHighestTagHash);
        System.setProperty("gitHighestTag",closestTag);
        System.setProperty("gitHighestTagCount",closestTagCount);
        System.setProperty("gitCommitCount",commitCount);
        System.setProperty("gitCurrentShortCommitHash",currentShortCommitHash);
        System.setProperty("gitCurrentCommitHash",currentCommitHash);
        System.setProperty("mavenVersion",mavenVersion);
        System.setProperty("appVersion",appVersion);
        System.setProperty("gitDescribe",gitDescribe);
        System.setProperty("gitAppDescribe",gitAppDescribe);
        if (gitPaddedVersionCount != null) {
            System.setProperty("gitPaddedVersionCount", gitPaddedVersionCount);
        }

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
        logger.debug("Found currentShortCommitHash: " + currentShortCommitHash)
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
        logger.debug("Found currentCommitHash: " + currentCommitHash)
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
        parentBranch = branch
        logger.debug("Found branch: " + branch)

    }

    void findClosestTagHash() {
        try {
            def stderr = new ByteArrayOutputStream()
            def stdout = new ByteArrayOutputStream()
            def branchToFindTag = branch
            if (!parentBranch.equals(branch)) {
                branchToFindTag = parentBranch
            }
            if (branchToFindTag.equals('master')) {
                def tag = findGitHighestTag()
                ExecResult result = this.project.exec({
                    it.commandLine 'git','log', '-1', '--format=format:%H', tag;
                    it.standardOutput = stdout;
                    it.errorOutput = stderr;
                });
                this.closestHighestTagHash = stdout.toString().trim();
                this.closestTag = tag
            } else {
                if (branchToFindTag.startsWith("bugfix_")) {
                    String version = highestVersionNumber(branchToFindTag)
                    ExecResult result = this.project.exec({
                        it.commandLine 'git', 'rev-list', '-n', '1', version
                        it.standardOutput = stdout
                        it.errorOutput = stderr;
                    });
                    this.closestHighestTagHash = stdout.toString().trim()
                } else if (branchToFindTag.equals("HEAD")) {
                    closestHighestTagHash = currentCommitHash;
                    findGitClosestTag();
                    if (closestTag.contains('-')) {
                        def tag = closestTag.substring(0,closestTag.indexOf('-'))
                        ExecResult result = this.project.exec({
                            it.commandLine 'git', 'rev-list', '-n', '1', tag
                            it.standardOutput = stdout
                            it.errorOutput = stderr;
                        });
                        this.closestHighestTagHash = stdout.toString().trim();
                    }
                } else {
                    def tag = findGitHighestTag()
                    ExecResult result = this.project.exec({
                        it.commandLine 'git','log', '-1', '--format=format:%H', tag;
                        it.standardOutput = stdout;
                        it.errorOutput = stderr;
                    });
                    this.closestHighestTagHash = stdout.toString().trim();
                    this.closestTag = tag
                }
            }
        }
        catch (ignored) {
            this.closestHighestTagHash = "0";
        }
        logger.debug("Found closestHighestTagHash: " + closestHighestTagHash)

    }

    private String highestVersionNumber(String branch) {
        def stderr = new ByteArrayOutputStream()
        def stdout = new ByteArrayOutputStream()

        def extractedVersion = branch.replaceAll("bugfix_", "").replaceAll("_", ".");
        ExecResult result = this.project.exec({
            it.commandLine 'git', 'tag', '-l', extractedVersion + '*', '--sort=v:refname'
            it.standardOutput = stdout
            it.errorOutput = stderr;
        });
        def outputString = stdout.toString().trim();
        def hashes;
        def version = '0.0.0';
        if (outputString.contains('\n')) {
            hashes = outputString.split('\n');
            for (String item : hashes) {
                version = item;
            }
        } else {
            version = outputString
        }
        return version;
    }

    String findGitHighestTag () {
        try {
            def stderr = new ByteArrayOutputStream()
            def stdout = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git','tag', '-l', '--sort=v:refname';
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
            def closestTag = '';
            for (String item : hashes) {
                logger.info(item)
                if (item.matches("[0-9.]*")) {
                    closestTag = item
                }

            }
            return closestTag
        }
        catch (ignored) {
            return "0.0.0";
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
        logger.debug("Found ClosestTag: " + closestTag)
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
        logger.debug("Found tagCount: " + closestTagCount)

    }

    void findMavenVersion() {
        def closestTag = closestTag;
        def gitBranch = branch;
        def versionSplit = /([0-9]+).([0-9]+).([0-9]+).*/;
        def matcher = ( closestTag =~ versionSplit );
        if (closestHighestTagHash.equals(currentCommitHash)) {
            mavenVersion = closestTag;
            def major = matcher[0][1];
            def minor = matcher[0][2];
            def bugfix = matcher[0][3];
            gitPaddedVersionCount = major +
                    String.format("%02d", minor.toLong()) +
                    String.format("%02d", bugfix.toLong()) +
                    String.format("%04d", commitCount.toLong());
            snapshot = false;
            if (isProjectDirty()) {
                mavenVersion += '-dirty'
            }
        } else {
            def major = matcher[0][1];
            def minor = matcher[0][2];
            def bugfix = matcher[0][3];
            def branchToFindVersion = branch
            if (!parentBranch.equals(branch)) {
                branchToFindVersion = parentBranch
            }
            if (gitBranch.equals("master")) {
                minor = minor.toLong() + 1;
                bugfix = "0-SNAPSHOT";
            } else if (gitBranch.startsWith("bugfix")) {
                bugfix = (bugfix.toLong() + 1) + "-SNAPSHOT";
            } else {
                def startIdx = 0;
                def endIdx = 11;
                if (gitBranch == null) {
                    gitBranch = "-UNKNOWN";
                } else if (gitBranch.length() >= endIdx) {
                    if (gitBranch.startsWith("SPRINT-")) {
                        startIdx = 7;
                    }
                    gitBranch = "-" + gitBranch.substring(startIdx, endIdx);
                } else if (gitBranch.equals("HEAD")) {
                    gitBranch = "";
                } else {
                    gitBranch = "-" + gitBranch.substring(startIdx, gitBranch.length());
                }
                if (!parentBranch.equals(branch) && parentBranch.startsWith('bugfix')) {
                    bugfix = (bugfix.toLong() + 1) +gitBranch + "-SNAPSHOT"
                } else {
                    minor = minor.toLong() + 1;
                    bugfix = "0" + gitBranch + "-SNAPSHOT";
                }
            }
            def bugfixExtracted = '0';
            if (bugfix.indexOf('-')) {
                bugfixExtracted = bugfix.substring(0,bugfix.indexOf('-'));
            } else if (bugfix.isNumber()) {
                bugfixExtracted = bugfix;
            }

            gitPaddedVersionCount = major +
                    String.format("%02d", minor.toLong()) +
                    String.format("%02d", bugfixExtracted.toLong()) +
                    String.format("%04d", commitCount.toLong());
            mavenVersion = (major +
                    "." +
                    minor +
                    "." +
                    bugfix);
            if (isProjectDirty()) {
                mavenVersion += '-dirty'
            }
        }
        logger.debug("Found mavenVersion: " + mavenVersion)
        logger.debug("Found gitPaddedVersionCount: " + gitPaddedVersionCount)
    }

    void findGitDescribeVersion() {
        if (currentCommitHash.equals(closestHighestTagHash)) {
            gitDescribe = closestTag;
        } else {
            if (branch.equals('master')) {
                gitDescribe = getMavenVersion()+ '-'+ currentShortCommitHash;
            } else {
                gitDescribe = getMavenVersion()+ '-' + commitCount +'-'+ currentShortCommitHash;
            }
        }
        logger.debug("found gitDescribe: " + gitDescribe)
    }

    void findGitAppDescribeVersion() {
        if (currentCommitHash.equals(closestHighestTagHash)) {
            gitAppDescribe = closestTag;
        } else {
            if (branch.equals('master')) {
                gitAppDescribe = gitDescribe.replaceAll('-SNAPSHOT','');
            } else {
                gitAppDescribe = gitDescribe.replaceAll('-SNAPSHOT','');
            }
        }
        appVersion = mavenVersion.replaceAll('-SNAPSHOT','');
        logger.debug("found gitAppDescribe: " + gitAppDescribe)
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
            def returnValue = stdout.toString().trim();
            logger.debug('ClosestTagForHash: ' + hash + ' tag: ' + returnValue)
            return returnValue;
        } catch (ignored) {
            return "0.0.0";
        }
    }

    boolean isProjectDirty() {
        try {
            def stdout = new ByteArrayOutputStream()
            def stderr = new ByteArrayOutputStream()
            ExecResult result = this.project.exec({
                it.commandLine 'git', 'status', '--porcelain';
                it.standardOutput = stdout;
                it.errorOutput = stderr
            });
            def resultValue = stdout.toString().trim();
            if (resultValue.length() > 2) {
                return resultValue.split("\n").size() > 0
            } else {
                return false;
            }
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
