import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ExtractorTestJUnit5 {
    private val changesExtractor = ChangesExtractor()


    @ExperimentalStdlibApi
    @Test
    fun `Test equal files`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                public void function(int arg1, int arg2) {
                    System.out.println(arg1 + arg2);
                }
            }
        """.trimIndent()
        val changes: List<Sample> = changesExtractor.extract(srcCode.encodeToByteArray(), srcCode.encodeToByteArray())
        assertEquals(0, changes.size)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test files with non-javadoc comment diff equals`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                public int function(int arg1) {
                    // This is function that adds 1 to number
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                public int function(int arg1) {
                    // This is function that adds 2 to number
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(0, changes.size)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test code style doesnt make any diff`() {
        @Language("JAVA")
        val srcCode = """
            class       A 


{
               public void function1( int arg1      ) 

{
                    arg1 = arg1 + 1;

       }
               public            void function2(  int arg2) {

                    arg2 =                                     arg2 + 1;
               }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
               public void function1(int arg1) {
                    arg1 = arg1 + 1;
               }
               public void function2(int arg2) {
                    arg2 = arg2 + 1;
               }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(0, changes.size)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test noncommented method changes arent extracted`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                /**
                * Function just add one to arg1
                * @param arg1
                * @return arg1 but bigger 
                */
                public int javadoc(int arg1) {
                    return arg1 + 1;
                }
                
                
                public int nonjavadoc(int arg1) {
                    // This is function that adds 1 to number
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                /**
                * Function just add one to arg1
                * @param arg1
                * @return arg1 but bigger 
                */
                public int javadoc(int arg1) {
                    return arg1 + 1;
                }
                
                
                public int nonjavadoc(int arg1) {
                    // This is function that adds 1 to number
                    return arg1 + 2;
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(0, changes.size)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test one javadoc change extracted`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 2
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(1, changes.size)

        assertEquals(
            """
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */""".trimIndent(), changes[0].D1)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(), changes[0].M1)
        assertEquals("""
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 2
                */""".trimIndent(), changes[0].D2)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(), changes[0].M2)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test two javadoc changes extracted`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT SOME
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
                
                /**
                * SAMPLE TEXT 
                * @param arg
                */
                public void function2(String arg) {
                    arg.toLowerCase();
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 2
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
                
                /**
                * SOME SAMPLE TEXT 
                * @param arg
                */
                public void function2(String arg) {
                    arg.toLowerCase();
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(2, changes.size)

        assertEquals(
            """
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT SOME
                */""".trimIndent(), changes[0].D1)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(), changes[0].M1)
        assertEquals("""
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 2
                */""".trimIndent(), changes[0].D2)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(), changes[0].M2)



        assertEquals(
            """
            /**
                * SAMPLE TEXT 
                * @param arg
                */""".trimIndent(), changes[1].D1)
        assertEquals("""
            public void function2(String arg) {
                    arg.toLowerCase();
                }
        """.trimIndent(), changes[1].M1)
        assertEquals("""
            /**
                * SOME SAMPLE TEXT 
                * @param arg
                */""".trimIndent(), changes[1].D2)
        assertEquals("""
            public void function2(String arg) {
                    arg.toLowerCase();
                }
        """.trimIndent(), changes[1].M2)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test one code change extracted`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT
                */
                public int function(int arg1) {
                    return arg1 + 2;
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(1, changes.size)

        assertEquals(
            """
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT
                */""".trimIndent(), changes[0].D1)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(), changes[0].M1)
        assertEquals("""
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT
                */""".trimIndent(), changes[0].D2)
        assertEquals("""
            public int function(int arg1) {
                    return arg1 + 2;
                }
        """.trimIndent(), changes[0].M2)
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test manu code changes extracted`() {
        @Language("JAVA")
        val srcCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */
                public int function(int arg1) {
                    return arg1 + 1;
                }
                
                
                /**
                * 
                * @param arg1
                * @return word i string repeated arg1 times
                */
                public String stringFunction(int arg1) {
                    String result = "";
                    for (int i = 0; i < arg1; i++) {
                        result += "word i";
                    }
                    return result;
                }
                
                /**
                * 
                * @param arg1
                * @return true if arg1 less than ten or false otherwise 
                */
                public boolean boolFunction(int arg1) {
                    return arg1 < 10;
                }
                
                /**
                * Some code copypasted from leetcode hard problem solution
                * @param arg1
                * @return 
                */
                public int[] superComplicatedFunction(int arg1) {
                    List<List<Integer>> graph = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        graph.add(new ArrayList<>());
                    }
                    
                    for (int[] edge : edges) {
                        graph.get(edge[0] - 1).add(edge[1] - 1);
                        graph.get(edge[1] - 1).add(edge[0] - 1);
                    }
                    
                    int[] answer = new int[n - 1];
                    
                    for (int bits = 1; bits < (1 << n); bits++) {
                        Set<Integer> nodes = new HashSet<>();
                        
                        for (int j = 0; j < n; j++) {
                            if (((1 << j) & bits) != 0) nodes.add(j);
                        }
                        
                        if (isSubtree(graph, nodes)) {
                            answer[maxDistance(graph, nodes) - 1]++;
                        }
                    }
                    
                    return answer;
                }
                
                /**
                *  Just printing stuff
                * @param arg1
                */
                public void printFunction(int arg1) {
                    System.out.println("THIS IS TEST");
                }
            }
        """.trimIndent()
        @Language("JAVA")
        val dstCode = """
            class A {
                /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */
                public int function(int arg1) {
                    return arg1 + 2;
                }
                
                
                /**
                * 
                * @param arg1
                * @return word i string repeated arg1 times
                */
                public String stringFunction(int arg1) {
                    String result = "";
                    for (int i = 0; i < arg1 * 2; i++) {
                        result += "word i";
                    }
                    return result;
                }
                
                /**
                * 
                * @param arg1
                * @return true if arg1 less than ten or false otherwise 
                */
                public boolean boolFunction(int arg1) {
                    return arg1 < 100;
                }
                
                /**
                * Some code copypasted from leetcode hard problem solution
                * @param arg1
                * @return 
                */
                public int[] superComplicatedFunction(int arg1) {
                    List<List<Integer>> graph = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        graph.add(new ArrayList<>());
                    }
                    
                    for (int[] edge : edges) {
                        graph.get(edge[0] + 1).add(edge[1] + 1);
                        graph.get(edge[1] + 1).add(edge[0] + 1);
                    }
                    
                    int[] answer = new int[n - 1];
                    
                    for (int bits = 1; bits < (1 << n); bits++) {
                        Set<Integer> nodes = new HashSet<>();
                        
                        for (int j = 0; j < n; j++) {
                            if (((1 << j) & bits) != 0) nodes.add(j);
                        }
                        
                        if (isSubtree(graph, nodes)) {
                            answer[maxDistance(graph, nodes) - 1]++;
                        }
                    }
                    
                    return answer;
                }
                
                /**
                *  Just printing stuff
                * @param arg1
                */
                public void printFunction(int arg1) {
                    System.out.println("THIS IS OTHER TEST");
                    System.out.println("THIS IS TEST");
                }
            }
        """.trimIndent()
        val changes = changesExtractor.extract(srcCode.encodeToByteArray(), dstCode.encodeToByteArray())
        assertEquals(5, changes.size)


        val sample1 = Sample(
            D1 = """
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */""".trimIndent(),
            M1 = """
            public int function(int arg1) {
                    return arg1 + 1;
                }
        """.trimIndent(),
            D2 = """
            /**
                * 
                * @param arg1
                * @return THIS IS SOME SAMPLE TEXT 1
                */""".trimIndent(),
            M2 = """
            public int function(int arg1) {
                    return arg1 + 2;
                }
        """.trimIndent()
        )

        val sample2 = Sample(
            D1 = """
            /**
                * 
                * @param arg1
                * @return word i string repeated arg1 times
                */""".trimIndent(),
            M1 = """
            public String stringFunction(int arg1) {
                    String result = "";
                    for (int i = 0; i < arg1; i++) {
                        result += "word i";
                    }
                    return result;
                }
        """.trimIndent(),
            D2 = """
            /**
                * 
                * @param arg1
                * @return word i string repeated arg1 times
                */""".trimIndent(),
            M2 = """
            public String stringFunction(int arg1) {
                    String result = "";
                    for (int i = 0; i < arg1 * 2; i++) {
                        result += "word i";
                    }
                    return result;
                }
        """.trimIndent()
        )

        val sample3 = Sample(
            D1 = """
            /**
                * 
                * @param arg1
                * @return true if arg1 less than ten or false otherwise 
                */""".trimIndent(),
            M1 = """
            public boolean boolFunction(int arg1) {
                    return arg1 < 10;
                }
        """.trimIndent(),
            D2 = """
            /**
                * 
                * @param arg1
                * @return true if arg1 less than ten or false otherwise 
                */""".trimIndent(),
            M2 = """
            public boolean boolFunction(int arg1) {
                    return arg1 < 100;
                }
        """.trimIndent()
        )

        val sample4 = Sample(
            D1 = """
            /**
                * Some code copypasted from leetcode hard problem solution
                * @param arg1
                * @return 
                */""".trimIndent(),
            M1 = """
            public int[] superComplicatedFunction(int arg1) {
                    List<List<Integer>> graph = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        graph.add(new ArrayList<>());
                    }
                    
                    for (int[] edge : edges) {
                        graph.get(edge[0] - 1).add(edge[1] - 1);
                        graph.get(edge[1] - 1).add(edge[0] - 1);
                    }
                    
                    int[] answer = new int[n - 1];
                    
                    for (int bits = 1; bits < (1 << n); bits++) {
                        Set<Integer> nodes = new HashSet<>();
                        
                        for (int j = 0; j < n; j++) {
                            if (((1 << j) & bits) != 0) nodes.add(j);
                        }
                        
                        if (isSubtree(graph, nodes)) {
                            answer[maxDistance(graph, nodes) - 1]++;
                        }
                    }
                    
                    return answer;
                }
        """.trimIndent(),
            D2 = """
            /**
                * Some code copypasted from leetcode hard problem solution
                * @param arg1
                * @return 
                */""".trimIndent(),
            M2 = """
            public int[] superComplicatedFunction(int arg1) {
                    List<List<Integer>> graph = new ArrayList<>();
                    for (int i = 0; i < n; i++) {
                        graph.add(new ArrayList<>());
                    }
                    
                    for (int[] edge : edges) {
                        graph.get(edge[0] + 1).add(edge[1] + 1);
                        graph.get(edge[1] + 1).add(edge[0] + 1);
                    }
                    
                    int[] answer = new int[n - 1];
                    
                    for (int bits = 1; bits < (1 << n); bits++) {
                        Set<Integer> nodes = new HashSet<>();
                        
                        for (int j = 0; j < n; j++) {
                            if (((1 << j) & bits) != 0) nodes.add(j);
                        }
                        
                        if (isSubtree(graph, nodes)) {
                            answer[maxDistance(graph, nodes) - 1]++;
                        }
                    }
                    
                    return answer;
                }
        """.trimIndent()
        )

        val sample5 = Sample(
            D1 = """
            /**
                *  Just printing stuff
                * @param arg1
                */""".trimIndent(),
            M1 = """
            public void printFunction(int arg1) {
                    System.out.println("THIS IS TEST");
                }
        """.trimIndent(),
            D2 = """
            /**
                *  Just printing stuff
                * @param arg1
                */""".trimIndent(),
            M2 = """
            public void printFunction(int arg1) {
                    System.out.println("THIS IS OTHER TEST");
                    System.out.println("THIS IS TEST");
                }
        """.trimIndent()
        )

        assert(sample1 in changes.toSet())
        assert(sample2 in changes.toSet())
        assert(sample3 in changes.toSet())
        assert(sample4 in changes.toSet())
        assert(sample5 in changes.toSet())
    }

    @ExperimentalStdlibApi
    @Test
    fun `Test javadoc and method change`() {
        val functionBefore = """
            void hello() {
                    System.out.println("Hello!");
                    System.out.println("World!");
                }
        """

        val docBefore = """
            /**
                * Javadoc changed
                */
        """


        @Language("JAVA")
        val code = """
            class MyClass {
                $docBefore
                $functionBefore
            }
        """.trimIndent()

        val functionAfter = """
            void hello() {
                    System.out.println("Hello,");
                    System.out.println("Kotlin");
                    System.out.println("World!");
                }
        """

        val docAfter = """
            /**
                * Javadoc changed also
                */
        """


        @Language("JAVA")
        val newCode = """
            class MyClass {
                $docAfter
                $functionAfter
            }
        """.trimIndent()
        val changes = changesExtractor.extract(code.encodeToByteArray(), newCode.encodeToByteArray())
        assertEquals(1, changes.size)
        assertEquals(Sample(
            D1 = docBefore.trimIndent(),
            M1 = functionBefore.trimIndent(),
            D2 = docAfter.trimIndent(),
            M2 = functionAfter.trimIndent()
        ), changes[0])
    }
}