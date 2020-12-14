import org.eclipse.jgit.api.*
import org.eclipse.jgit.junit.RepositoryTestCase
import org.eclipse.jgit.junit.TestRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class GitDiffTest : RepositoryTestCase() {
//    private var git: Git? = null
//    private var tr: TestRepository<Repository>? = null
//    @Throws(Exception::class)
//    override fun setUp() {
//        super.setUp()
//        tr = TestRepository(db)
//        git = Git(db)
//        // commit something
//        writeTrashFile("Test.txt", "Hello world")
//        git!!.add().addFilepattern("Test.txt").call()
//        git!!.commit().setMessage("Initial commit").call()
//        val head = git!!.tag().setName("tag-initial").setMessage("Tag initial")
//            .call()
//
//        // create a test branch and switch to it
//        git!!.checkout().setCreateBranch(true).setName("test").call()
//
//
//        // commit something on the test branch
//        writeTrashFile("Test.txt", "Some change")
//        git!!.add().addFilepattern("Test.txt").call()
//        git!!.commit().setMessage("Second commit").call()
//        val blob: RevBlob = (tr as TestRepository<Repository>).blob("blob-not-in-master-branch")
//        git!!.tag().setName("tag-for-blob").setObjectId(blob).call()
//    }

    @Test
    fun `test two commits on master branch`() {
        setUp()
        val testRepo = TestRepository(db)
        val git = Git(db)
        @Language("JAVA")
        val code = """
            class MyClass {
                /**
                * Very complicated math function
                * @param arg1: radius of the Earth
                * @param arg2: mass of the electron
                * @return something very meaningful 
                */
                public int doMath(int arg1, int arg2) {
                    return (arg1 + arg2) * (arg1 - arg2);
                }
            }
        """.trimIndent()
        writeTrashFile("sample.java", code)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Initial commit").call()

        @Language("JAVA")
        val newCode = """
            class MyClass {
                /**
                * Very complicated math function
                * @param arg1: radius of the Earth
                * @param arg2: mass of the electron
                * @return something very meaningful 
                */
                public int doMath(int arg1, int arg2) {
                    arg1 = arg1 * 3.14159 - 2.718281828459045 * 3.14159;
                    return (arg1 + arg2) * (arg1 - arg2);
                }
            }
        """.trimIndent()
        writeTrashFile("sample.java", newCode)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Second commit").call()

        val changes: List<Sample> = walkRepo(testRepo.repository, verbose = false)
        assertEquals(1, changes.size)
        val sample = Sample(D1 = """
            /**
                * Very complicated math function
                * @param arg1: radius of the Earth
                * @param arg2: mass of the electron
                * @return something very meaningful 
                */""".trimIndent(), M1 = """
            public int doMath(int arg1, int arg2) {
                    return (arg1 + arg2) * (arg1 - arg2);
                }
        """.trimIndent(),
            D2 = """
            /**
                * Very complicated math function
                * @param arg1: radius of the Earth
                * @param arg2: mass of the electron
                * @return something very meaningful 
                */""".trimIndent(),
        M2 = """
            public int doMath(int arg1, int arg2) {
                    arg1 = arg1 * 3.14159 - 2.718281828459045 * 3.14159;
                    return (arg1 + arg2) * (arg1 - arg2);
                }
        """.trimIndent())
        assertEquals(sample, changes[0])
    }


    @Test
    fun `test five commits om master with small changes`() {
        setUp()
        val testRepo = TestRepository(db)
        val git = Git(db)

        var functionBefore = """
            void hello() {
                System.out.println("Hello!");
            }
            """

        var docBefore = """
            /**
                * Javadoc
                */
        """

        val code1 = """
            class MyClass {
                $docBefore
                $functionBefore
            }
        """.trimIndent()
        writeTrashFile("sample.java", code1)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Initial commit").call()

        var functionAfter = """
            void hello() {
                    System.out.println("Hello!");
                }
        """

        var docAfter = """
            /**
                * Javadoc changed
                */
        """

        @Language("JAVA")
        val code2 = """
            class MyClass {
                $docAfter
                $functionAfter
            }
        """.trimIndent()
        writeTrashFile("sample.java", code2)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Second commit").call()

        var changes: List<Sample> = walkRepo(testRepo.repository, verbose = false)
        assertEquals(1, changes.size)
        var sample = Sample(D1 =docBefore.trimIndent(),
            M1 = functionBefore.trimIndent(),
            D2 = docAfter.trimIndent(),
            M2 = functionAfter.trimIndent())
        assertEquals(sample, changes[0])

        functionBefore = functionAfter
        functionAfter = """
            void hello() {
                    System.out.println("Hello!");
                    System.out.println("World!");
                }
        """

        docBefore = docAfter
        docAfter = """
            /**
                * Javadoc changed
                */
        """


        @Language("JAVA")
        val code3 = """
            class MyClass {
                $docAfter
                $functionAfter
            }
        """.trimIndent()
        writeTrashFile("sample.java", code3)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Third commit").call()

        changes = walkRepo(testRepo.repository, verbose = false)

        assertEquals(2, changes.size)
        sample = Sample(
            D1 = docBefore.trimIndent(),
            M1 = functionBefore.trimIndent() ,
            D2 = docAfter.trimIndent(),
            M2 = functionAfter.trimIndent())
        assertEquals(sample, changes[0])


        functionBefore = functionAfter
        functionAfter = """
            void hello() {
                    System.out.println("Hello,");
                    System.out.println("Kotlin");
                    System.out.println("World!");
                }
        """

        docBefore = docAfter
        docAfter = """
            /**
                * Javadoc changed also
                */
        """


        @Language("JAVA")
        val code4 = """
            class MyClass {
                $docAfter
                $functionAfter
            }
        """.trimIndent()
        writeTrashFile("sample.java", code4)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Fourth commit").call()

        changes = walkRepo(testRepo.repository, verbose = false)
        assertEquals(3, changes.size)
        sample = Sample(
            D1 = docBefore.trimIndent(),
            M1 = functionBefore.trimIndent() ,
            D2 = docAfter.trimIndent(),
            M2 = functionAfter.trimIndent())
        assertEquals(sample, changes[0])

        functionBefore = functionAfter
        functionAfter = """
            int hello() {
                    System.out.println("Hello,");
                    System.out.println("Kotlin");
                    System.out.println("World!");
                    return 1;
                }
        """

        docBefore = docAfter
        docAfter = """
            /**
                * Javadoc changed also
                */
        """


        @Language("JAVA")
        val code5 = """
            class MyClass {
                $docAfter
                $functionAfter
            }
        """.trimIndent()
        writeTrashFile("sample.java", code5)
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Fifth commit").call()

        changes = walkRepo(testRepo.repository, verbose = false)
        assertEquals(4, changes.size)
        sample = Sample(
            D1 = docBefore.trimIndent(),
            M1 = functionBefore.trimIndent(),
            D2 = docAfter.trimIndent(),
            M2 = functionAfter.trimIndent())
        assertEquals(sample, changes[0])
    }

}