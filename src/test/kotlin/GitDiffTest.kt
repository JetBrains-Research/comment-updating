import org.eclipse.jgit.api.*
import org.eclipse.jgit.junit.RepositoryTestCase
import org.eclipse.jgit.junit.TestRepository
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.api.MergeCommand







class GitDiffTest : RepositoryTestCase() {

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

    @Test
    fun `Test two branches`() {
        setUp()
        val testRepo = TestRepository(db)
        val git = Git(db)

        writeTrashFile("sample.java", """
            class MyClass {
                /**
                * Main function 
                */
                public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
            }
        """.trimIndent())
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Initial commit").call()

        // create a test branch and switch to it
        git.checkout().setCreateBranch(true).setName("test").call()

        // commit something on the test branch
        writeTrashFile("sample.java", """
            class MyClass {
                /**
                * Main function 
                */
                public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
                
                /**
                * 
                * @param a
                * @param b
                * @return sum of a and b
                */
                public int add(int a, int b) {
                    return a + b;
                }
            }
        """.trimIndent())
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Function add added").call()

        // commit something on the test branch
        writeTrashFile("sample.java", """
            class MyClass {
                /**
                * Main function 
                */
                public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
                
                /**
                * Read more about laplandian algebraic field here: [link](https://www.youtube.com/watch?v=oHg5SJYRHA0)
                * @param a
                * @param b
                * @return laplandian sum of a and b
                */
                public int add(int a, int b) {
                    if (a > b) {
                        return a % b + b;
                    }
                    return a + b;
                }
            }
        """.trimIndent())
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Laplandian addition added").call()

        val mergeBase: ObjectId = testRepo.repository.resolve("test")
        val merge: MergeResult? =
            git.
            merge().
            include(mergeBase).
            setCommit(true)
            .setFastForward(MergeCommand.FastForwardMode.NO_FF).
                setMessage("Merged test to master")
                .call()

        git.checkout().setCreateBranch(false).setName("master").call()
        // commit something on the master branch

        writeTrashFile("sample.java", """
            class MyClass {
                /**
                * Main function on master
                */
                public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
                
                /**
                * Read more about laplandian algebraic field here: [link](https://www.youtube.com/watch?v=oHg5SJYRHA0)
                * @param a
                * @param b
                * @return laplandian sum of a and b
                */
                public int add(int a, int b) {
                    if (a > b) {
                        return a % b + b;
                    }
                    return a + b;
                }
            }
        """.trimIndent())
        git.add().addFilepattern("sample.java").call()
        git.commit().setMessage("Javadoc changed for main").call()

        val changes = walkRepo(testRepo.repository, verbose = false)
        assertEquals(2, changes.size)
        val sample1 = Sample(
            D1 = """
            /**
                * Main function 
                */
        """.trimIndent(),
        M1 = """
            public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
        """.trimIndent(),
        D2 = """
            /**
                * Main function on master
                */
        """.trimIndent(),
        M2 = """
            public static void main(String[] args) {
                    System.out.println("Hello, world!");
                }
        """.trimIndent())

        val sample2 = Sample(
            D1 = """
            /**
                * 
                * @param a
                * @param b
                * @return sum of a and b
                */
        """.trimIndent(),
            M1 = """
            public int add(int a, int b) {
                    return a + b;
                }
        """.trimIndent(),
            D2 = """
            /**
                * Read more about laplandian algebraic field here: [link](https://www.youtube.com/watch?v=oHg5SJYRHA0)
                * @param a
                * @param b
                * @return laplandian sum of a and b
                */
        """.trimIndent(),
            M2 = """
            public int add(int a, int b) {
                    if (a > b) {
                        return a % b + b;
                    }
                    return a + b;
                }
        """.trimIndent())
        assert(sample1 in changes.toSet())
        assert(sample2 in changes.toSet())
    }

}