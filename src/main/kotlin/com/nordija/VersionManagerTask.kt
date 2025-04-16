package com.nordija

import com.github.javaparser.utils.Log
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Main plugin task class
 *
 * Note: cache error can not be traced in this version and it is probably due another
 * task depending on, and or base task itself, disabled yet
 */
@Suppress("unused")
@DisableCachingByDefault
open class VersionManagerTask : DefaultTask() {
    private var branch: String = ""
    private var parentBranch: String = ""
    private var closestHighestTagHash: String? = null
    private var closestTag: String? = null
    private var closestTagCount: String? = null
    private var commitCount: String? = null
    private var currentShortCommitHash: String? = null
    private var currentCommitHash: String? = null
    private var mavenVersion: String? = null
    private var appVersion: String? = null
    private var gitDescribe: String? = null
    private var gitAppDescribe: String? = null
    private var gitPaddedVersionCount: String? = null
    private var snapshot: Boolean = true

    private val isCI: Boolean
        get() = if (hasProperty("CI")) {
            parseBoolean(project.properties["CI"].toString())
        } else {
            false
        }

    init {
        findGitVersions()
        setVersions()
    }

    private fun execGitCommand(
        vararg commands: Any
    ): String? = runCatching {
        project.providers.exec {
            commandLine(*commands)
            isIgnoreExitValue = true
        }.standardOutput?.asText?.getOrElse("")?.trim { it <= ' ' }
    }.onFailure { e ->
        if (logger.isDebugEnabled) {
            logger.debug("E: Git command: ${commands.contentToString()}")
            logger.debug("E: ${e.message}")
        }
    }.getOrNull()

    @TaskAction
    fun findGitVersions() {
        findBranch()
        findCurrentCommitHash()
        if (branch != "HEAD") {
            findParentBranch()
        }
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

    private fun findParentBranch() {
        if (branch.isNotEmpty() && branch != "main" && !branch.startsWith("bugfix_")) {
            var foundHash = parentBranchCommitHash()
            if (foundHash.isNullOrEmpty()) {
                foundHash = currentCommitHash
            }
            if (!foundHash.isNullOrEmpty()) {
                val hashes = foundHash.split(' ')
                var parentBranchFound = ""
                for (hash in hashes) {
                    val foundBranch = findLowestBranchForHash(hash)
                    if (foundBranch.isNotEmpty() && parentBranchFound != foundBranch) {
                        if (foundBranch.startsWith("bugfix")) {
                            parentBranchFound = findLowestBranch(foundBranch, parentBranchFound)
                        } else if (!parentBranchFound.startsWith("bugfix") && foundBranch == "main") {
                            parentBranchFound = foundBranch
                        }
                    }
                }
                parentBranch = parentBranchFound
            }
        }
    }

    private fun findLowestBranch(branchA: String, branchB: String): String {
        if (branchA.isEmpty()) return branchB
        if (branchB.isEmpty()) return branchA
        if (branchA.startsWith("bugfix") && branchB.startsWith("bugfix")) {
            val compare = branchA.compareTo(branchB)
            return if (compare < 0 || compare == 0) branchA else branchB
        } else if (branchA.startsWith("bugfix")) {
            return branchA
        } else if (branchB.startsWith("bugfix")) {
            return branchB
        }
        if (branchA == "main") return branchA
        if (branchB == "main") return branchB
        return ""
    }

    private fun findLowestBranchForHash(hash: String): String {
        val outputString = if (isCI) execGitCommand("git", "branch", "--contains", hash)
        else execGitCommand("git", "branch", "-r", "--contains", hash)
        if (outputString.isNullOrEmpty()) return ""
        return if (outputString.contains('\n')) {
            val branchList = outputString.split('\n')
            var branchFound: String
            for (item in branchList) {
                branchFound = if (item.startsWith("*")) {
                    item.substring(1)
                } else {
                    item
                }
                branchFound = branchFound.trim().replace("origin/", "")
                if (branchFound.startsWith("bugfix_") || branchFound == "main") {
                    return branchFound
                }
            }
            ""
        } else {
            if (outputString.startsWith("*")) {
                outputString.replace("origin/", "").substring(1).trim()
            } else {
                outputString.replace("origin/", "").trim()
            }
        }
    }

    private fun parentBranchCommitHash(): String? {
        val outputString = if (isCI)
            execGitCommand("git", "log", branch, "--not", "main", "--pretty=format:%P")
        else
            execGitCommand("git", "log", branch, "--not", "origin/main", "--pretty=format:%P")
        return if (outputString?.contains('\n') == true) {
            outputString.split('\n').lastOrNull()
        } else {
            outputString
        }
    }

    private fun findCommitCount() {
        commitCount = execGitCommand("git", "rev-list", "--all", "--count")
        if (commitCount.isNullOrEmpty()) {
            commitCount = "0"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found commit count: $commitCount")
        }
    }

    private fun setVersions() {
        System.setProperty("gitBranch", branch)
        if (parentBranch.isEmpty()) parentBranch = "N/A"
        System.setProperty("gitParentBranch", parentBranch)
        System.setProperty("gitHighestTagHash", closestHighestTagHash ?: "")
        System.setProperty("gitHighestTag", closestTag ?: "")
        System.setProperty("gitHighestTagCount", closestTagCount ?: "")
        System.setProperty("gitCommitCount", commitCount ?: "")
        System.setProperty("gitCurrentShortCommitHash", currentShortCommitHash ?: "")
        System.setProperty("gitCurrentCommitHash", currentCommitHash ?: "")
        System.setProperty("mavenVersion", mavenVersion ?: "")
        System.setProperty("appVersion", appVersion ?: "")
        System.setProperty("gitDescribe", gitDescribe ?: "")
        System.setProperty("gitAppDescribe", gitAppDescribe ?: "")
        if (gitPaddedVersionCount != null) {
            System.setProperty("gitPaddedVersionCount", gitPaddedVersionCount ?: "")
        }
        System.setProperty("versionSnapshot", snapshot.toString())
        mavenVersion?.let { mv -> project.version = mv }
    }

    private fun findCurrentCommitShortHash() {
        currentShortCommitHash = execGitCommand("git", "rev-parse", "--short", "HEAD")
        if (currentShortCommitHash.isNullOrEmpty()) {
            currentShortCommitHash = "0"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found currentShortCommitHash: $currentShortCommitHash")
        }
    }

    private fun findCurrentCommitHash() {
        currentCommitHash = execGitCommand("git", "rev-parse", "HEAD")
        if (currentCommitHash.isNullOrEmpty()) {
            currentCommitHash = "NoHashFound"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found currentCommitHash: $currentCommitHash")
        }
    }

    private fun findBranch() {
        branch = execGitCommand("git", "rev-parse", "--abbrev-ref", "HEAD") ?: ""
        branch = branch.replace("[^\\dA-Za-z ]".toRegex(), "_")
        parentBranch = branch
        if (logger.isDebugEnabled) {
            logger.debug("Found branch: $branch")
        }
    }

    private fun findClosestTagHash() {
        var branchToFindTag = branch
        if (parentBranch != branch) {
            branchToFindTag = parentBranch
        }
        if (branchToFindTag == "main") {
            val tag = findGitHighestTag()
            closestHighestTagHash = execGitCommand("git", "log", "-1", "--format=format:%H", tag)
            this.closestTag = tag
        } else {
            if (branchToFindTag.startsWith("bugfix_")) {
                val version = highestVersionNumber(branchToFindTag)
                this.closestHighestTagHash = execGitCommand("git", "rev-list", "-n", "1", version)
            } else if (branchToFindTag == "HEAD") {
                closestHighestTagHash = currentCommitHash
                findGitClosestTag()
                val containsMinus = closestTag?.contains('-') ?: false
                val isRC = closestTag?.contains("-RC") ?: false
                val isM = closestTag!!.contains("-M")
                if (containsMinus && !isRC && !isM) {
                    val tag = closestTag?.substring(0, closestTag?.indexOf('-') ?: 0) ?: ""
                    this.closestHighestTagHash = execGitCommand("git", "rev-list", "-n", "1", tag)
                }
            } else {
                val tag = findGitHighestTag()
                this.closestHighestTagHash =
                    execGitCommand("git", "log", "-1", "--format=format:%H", tag)
                this.closestTag = tag
            }
        }
        if (closestHighestTagHash.isNullOrEmpty()) {
            this.closestHighestTagHash = "0"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found closestHighestTagHash: $closestHighestTagHash")
        }
    }

    @Suppress("RegExpDuplicateCharacterInClass")
    private fun highestVersionNumber(branch: String): String {
        val extractedVersion = branch.replace("bugfix_", "")
            .replace("_", ".")
        val outputString =
            execGitCommand("git", "tag", "-l", "$extractedVersion*", "--sort=v:refname") ?: ""
        return if (outputString.contains('\n')) {
            outputString.split('\n').filter {
                it.matches("[0-9|.|a-z|R|C|M|-]*".toRegex())
            }.filter {
                it.isNotEmpty()
            }.fold("0.0.0") { acc, item ->
                if (
                    acc.isEmpty() ||
                    !(item.startsWith(acc) && (item.contains("RC") ||
                            item.contains("M")))
                ) item else acc
            }
        } else outputString
    }

    @Suppress("RegExpDuplicateCharacterInClass")
    private fun findGitHighestTag(): String {
        val outputString = execGitCommand("git", "tag", "-l", "--sort=v:refname") ?: ""
        val hashes = if (outputString.contains('\n')) outputString.split('\n')
        else listOf(outputString)
        return hashes.filter {
            it.matches("[0-9|.|a-z|R|C|M|-]*".toRegex())
        }.fold("") { acc, item ->
            if (
                acc.isEmpty() ||
                !(item.startsWith(acc) && (item.contains("RC") ||
                        item.contains("M")))
            ) item else acc
        }
    }

    private fun findGitClosestTag() {
        closestTag = execGitCommand("git", "describe", "--tags", closestHighestTagHash ?: "")
        if (closestTag.isNullOrEmpty()) closestTag = "0.0.0"
        if (branch.isEmpty()) branch = "main"
        if (branch.startsWith("bugfix_") && closestTag == "0.0.0") {
            val extractedVersion = branch.replace("bugfix_", "")
                .replace("_", ".")
            closestTag = "$extractedVersion.0"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found ClosestTag: $closestTag")
        }
    }

    private fun findCountFromClosestTagHash() {
        closestTagCount = execGitCommand("git", "rev-list", "$closestHighestTagHash..", "--count")
        if (closestTagCount.isNullOrEmpty()) {
            closestTagCount = "0"
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found tagCount: $closestTagCount")
        }
    }

    private fun findMavenVersion() {
        val localClosestTag = closestTag ?: ""
        val localBranch = branch
        val versionSplit = "([0-9]+).([0-9]+).([0-9]+).*".toRegex()
        val matcher = versionSplit.find(localClosestTag)
        if (closestHighestTagHash == currentCommitHash) {
            mavenVersion = localClosestTag
            val major = matcher?.groupValues?.get(1)
            val minor = matcher?.groupValues?.get(2)
            val bugfix = matcher?.groupValues?.get(3)
            gitPaddedVersionCount = major +
                    minor?.toLong()?.to00() +
                    bugfix?.toLong()?.to00() +
                    commitCount?.toLong()?.to00()
            snapshot = false
            if (isProjectDirty()) {
                mavenVersion += "-dirty"
            }
        } else {
            val major = matcher?.groupValues?.get(1)
            val minor = matcher?.groupValues?.get(2)
            var bugfix = matcher?.groupValues?.get(3)
//            var branchToFindVersion = branch
//            if (parentBranch != branch) {
//                branchToFindVersion = parentBranch
//            }
            if (localBranch.isEmpty()) {
                mavenVersion = "0.0.0-SNAPSHOT"
                branch = "unknown"
                return
            }
            if (localBranch == "main") {
                if (localClosestTag.contains("-M")) {
                    bugfix = "0-SNAPSHOT"
                } else {
                    minor?.toLong()?.plus(1).toString()
                    bugfix = "0-SNAPSHOT"
                }
            } else if (localBranch.startsWith("bugfix")) {
                if ((closestHighestTagHash == "0" && bugfix == "0" && closestTagCount == "0") || localClosestTag.contains(
                        "-RC"
                    ) || localClosestTag.contains("-M")
                ) {
                    bugfix += "-SNAPSHOT"
                } else {
                    bugfix = bugfix?.toLong()?.plus(1).toString() + "-SNAPSHOT"
                }
            } else {
                var localGitBranch = localBranch
                var startIdx = 0
                val endIdx = 11
                if (localGitBranch.isEmpty()) {
                    localGitBranch = "-UNKNOWN"
                } else if (localGitBranch.length >= endIdx) {
                    if (localGitBranch.startsWith("SPRINT-")) {
                        startIdx = 7
                    }
                    localGitBranch = "-" + localGitBranch.substring(startIdx, endIdx)
                } else if (localGitBranch == "HEAD") {
                    localGitBranch = ""
                } else {
                    localGitBranch = "-" + localGitBranch.substring(startIdx, localGitBranch.length)
                }
                if (parentBranch != branch && parentBranch.startsWith("bugfix")) {
                    bugfix = bugfix?.toLong()?.plus(1).toString() + localGitBranch + "-SNAPSHOT"
                } else if (localGitBranch.startsWith("-") && localClosestTag.contains("-M")) {
                    bugfix = "0$localGitBranch-SNAPSHOT"
                } else {
                    minor?.toLong()?.plus(1).toString()
                    bugfix = "0$localGitBranch-SNAPSHOT"
                }
            }
            var bugfixExtracted = "0"
            if (bugfix?.contains('-') == true) {
                bugfixExtracted = bugfix.substring(0, bugfix.indexOf('-'))
            } else if (bugfix?.toLongOrNull() != null) {
                bugfixExtracted = bugfix
            }
            gitPaddedVersionCount = major +
                    minor?.toLong()?.to00() +
                    bugfixExtracted.toLongOrNull()?.to00() +
                    commitCount?.toLong()?.to00()
            mavenVersion = "$major.$minor.$bugfix"
            if (isProjectDirty()) {
                mavenVersion += "-dirty"
            }
        }
        if (mavenVersion == localClosestTag && branch == "HEAD") {
            branch = mavenVersion!!
        }
        if (logger.isDebugEnabled) {
            logger.debug("Found mavenVersion: $mavenVersion")
            logger.debug("Found gitPaddedVersionCount: $gitPaddedVersionCount")
        }
    }

    private fun findGitDescribeVersion() {
        gitDescribe = if (currentCommitHash == closestHighestTagHash) {
            closestTag
        } else {
            if (branch == "main") {
                "$mavenVersion-${currentShortCommitHash}"
            } else {
                "$mavenVersion-$commitCount-${currentShortCommitHash}"
            }
        }
        if (logger.isDebugEnabled) {
            logger.debug("found gitDescribe: $gitDescribe")
        }
    }

    private fun findGitAppDescribeVersion() {
        gitAppDescribe = if (currentCommitHash == closestHighestTagHash) {
            closestTag
        } else {
            mavenVersion?.replace("-SNAPSHOT", "")
        }
        appVersion = mavenVersion?.replace("-SNAPSHOT", "")
        if (logger.isDebugEnabled) {
            logger.debug("found gitAppDescribe: $gitAppDescribe")
        }
    }

    private fun getClosestTagForHash(hash: String): String {
        val returnValue = execGitCommand("git", "describe", "--tags", hash) ?: "0.0.0"
        if (logger.isDebugEnabled) {
            logger.debug("ClosestTagForHash: $hash tag: $returnValue")
        }
        return returnValue
    }

    private fun isProjectDirty(): Boolean = execGitCommand(
        "git", "status", "--porcelain"
    ).let { resultValue ->
        ((resultValue?.length ?: 0) > 2) &&
                ((resultValue?.split("\n")?.size ?: 0) > 0)
    }

    private fun compareVersions(v1: String, v2: String): Int {
        if (v1.isNotEmpty() && v2.isEmpty()) return -1
        if (v1.isEmpty() && v2.isEmpty()) return 0
        if (v1.isEmpty() && v2.isNotEmpty()) return 1
        val pos1 = v1.indexOf('.')
        val pos2 = v2.indexOf('.')
        val num1 = if (pos1 > 0) v1.substring(0, pos1).toInt() else 0
        val num2 = if (pos2 > 0) v2.substring(0, pos2).toInt() else 0
        if (num1 != num2) return num1.compareTo(num2)
        val tail1 = if (pos1 > 0) v1.substring(pos1 + 1) else ""
        val tail2 = if (pos2 > 0) v2.substring(pos2 + 1) else ""
        return compareVersions(tail1, tail2)
    }

    companion object {
        @Suppress("DefaultLocale")
        fun Long.to00() = String.format("%02d", this)

        fun parseBoolean(
            input: String,
            default: Boolean = false
        ): Boolean = runCatching {
            java.lang.Boolean.parseBoolean(input)
        }.onFailure { e ->
            Log.error("E: ${e.message}")
        }.getOrDefault(default)
    }
}