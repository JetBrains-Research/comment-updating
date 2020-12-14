//import org.apache.commons.io.FileUtils;

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ListBranchCommand.ListMode
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevSort
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import java.io.File
import java.io.PrintWriter
import java.util.Map.entry


fun main(args: Array<String>) {
    val REMOTE_URL = "https://github.com/h0tk3y/kotlin-spbsu-2019-project-south.git"
    val repo = getRepo("", repoPath = "./repo")
    //logCommits(repo)
    //logBranches(repo)
    val list = walkRepo(repo, verbose=true)
    list.forEach {
        println("COMMENT BEFORE: ${it.D1}")
        println("COMMENT AFTER: ${it.D2}")
        println("CODE BEFORE: ${it.M1}")
        println("CODE AFTER: ${it.M2}")
    }
}

fun getRepo(repoUrl: String, repoPath: String = "./TestGitRepository") : Repository {
    val localPath = File(repoPath)
    if (!localPath.exists()) {
        System.out.println("Cloning from " + repoUrl.toString() + " to " + localPath)
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(localPath)
                .setProgressMonitor(TextProgressMonitor(PrintWriter(System.out)))
                .call().let { result ->
                    // Note: the call() returns an opened repository already which needs to be closed to avoid file handle leaks!
                    println("Having repository: " + result.getRepository().getDirectory())
                }
    }
    val repoDir = File(repoPath + "/.git")
    val builder = FileRepositoryBuilder()
    return builder.setGitDir(repoDir)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build()
}

private fun walkBranch(repo: Repository, branchName: String = "refs/heads/master",
                       lookedPairs: HashSet<Pair<String, String>>, changesExtractor: ChangesExtractor,
                       verbose: Boolean = false): List<Sample> {
    val head = repo.exactRef(branchName) ?: repo.exactRef("refs/heads/main")


    val list = mutableListOf<Sample>()
    // a RevWalk allows to walk over commits based on some filtering that is defined
    RevWalk(repo).let { walk ->
        val commit = walk.parseCommit(head.getObjectId())
        if (verbose) println("Start-Commit: " + commit)

        if(verbose) println("Walking all commits starting at HEAD")
        walk.markStart(commit)
        walk.sort(RevSort.COMMIT_TIME_DESC, true)

        Git(repo).let { git ->

            var prev: RevCommit? = null
            for (rev in walk) {
                if(verbose) println("Commit: " + rev.id.name.substring(0, 10) + " " + rev.shortMessage)
                if (prev != null) {
                    if (lookedPairs.contains(Pair(rev.id.name, prev.id.name))) {
                        continue
                    }
                    if(verbose) println("Changes between ${rev.shortMessage} and ${prev.shortMessage}: ")
                    list.addAll(listDiff(repo, git, rev.id.name, prev.id.name, changesExtractor, verbose = verbose))
                    lookedPairs.add(Pair(rev.id.name, prev.id.name))
                }
                prev = rev
            }
        }
        walk.dispose()
    }
    return list
}

fun walkRepo(repo: Repository, verbose: Boolean = false): List<Sample> {
    val changesExtractor = ChangesExtractor()
    val lookedPairs = HashSet<Pair<String, String>>()
    val branches = getBranches(repo)
    val list = mutableListOf<Sample>()
    for (branch in branches) {
        if (verbose) println("Processing branch: $branch")
        list.addAll(walkBranch(repo, branch, lookedPairs, changesExtractor, verbose=verbose))
    }
    return list
}

private fun getBranches(repo: Repository, local: Boolean = false): List<String> {
    Git(repo).let { git ->
        var call: List<Ref> = git.branchList().call()
        if (local)
            return call.map { it.name }

        call = git.branchList().setListMode(ListMode.ALL).call()
        return  call.map { it.name }
    }
}

private fun listDiff(repository: Repository, git: Git, oldCommit: String, newCommit: String, changesExtractor: ChangesExtractor,
                     verbose: Boolean = false): List<Sample> {
    val diffs = git.diff()
        .setOldTree(prepareTreeParser(repository, oldCommit))
        .setNewTree(prepareTreeParser(repository, newCommit))
        .call()
    val list = mutableListOf<Sample>()
    for (diff: DiffEntry in diffs) {
        if (diff.changeType == DiffEntry.ChangeType.MODIFY) {
            if (diff.oldPath.endsWith(".java") && diff.newPath.endsWith(".java")) {
                if (verbose) {
                    println(
                        "Diff: Java " + diff.changeType + ": " +
                                if (diff.oldPath == diff.newPath) diff.newPath else diff.oldPath + " -> " + diff.newPath
                    )
                }

//                val loader: ObjectLoader = repository.open(diff.oldId.toObjectId())
//                val charset = Charsets.UTF_8
//                if (verbose) println(loader.bytes.toString(charset))

                val samples = changesExtractor.extract(repository.open(diff.oldId.toObjectId()).bytes,
                                repository.open(diff.newId.toObjectId()).bytes)
                list.addAll(samples)
            }

            if (diff.oldPath.endsWith(".kt") && diff.newPath.endsWith(".kt")) {
                if (verbose) {
                    println(
                            "Diff: Kotlin " + diff.changeType + ": " +
                                    if (diff.oldPath == diff.newPath) diff.newPath else diff.oldPath + " -> " + diff.newPath
                    )
                }
                //val loader: ObjectLoader = repository.open(diff.oldId.toObjectId())
                //val charset = Charsets.UTF_8
                //println(loader.bytes.toString(charset))
                //val srcBytes: ByteArray  = repository.open(diff.oldId.toObjectId()).bytes
                //srcBytes.

//                if (verbose) {
//                    DiffFormatter(System.out).let { formatter ->
//                        formatter.setRepository(repository)
//                        formatter.format(diff)
//                    }
//                }

                val samples = changesExtractor.extract(repository.open(diff.oldId.toObjectId()).bytes,
                    repository.open(diff.newId.toObjectId()).bytes)
                list.addAll(samples)
            }


        }
    }
    return list
}

private fun prepareTreeParser(repository: Repository, objectId: String): AbstractTreeIterator {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).let { walk ->
        val commit: RevCommit = walk.parseCommit(repository.resolve(objectId))
        val tree: RevTree = walk.parseTree(commit.getTree().getId())
        val treeParser: CanonicalTreeParser = CanonicalTreeParser()
        repository.newObjectReader().let { reader -> treeParser.reset(reader, tree.getId()) }
        walk.dispose()
        return treeParser
    }
}
