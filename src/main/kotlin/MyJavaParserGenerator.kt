import com.github.gumtreediff.gen.Register
import com.github.gumtreediff.gen.Registry
import com.github.gumtreediff.gen.SyntaxException
import com.github.gumtreediff.gen.TreeGenerator
import com.github.gumtreediff.io.LineReader
import com.github.gumtreediff.tree.TreeContext
import com.github.javaparser.ParseProblemException
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.printer.DotPrinter
import java.io.FileWriter
import java.io.IOException
import java.io.PrintWriter
import java.io.Reader


@Register(id = "java-javaparser", accept = ["\\.java$"], priority = Registry.Priority.MEDIUM)
class MyJavaParserGenerator : TreeGenerator() {

    public fun getCU(r: Reader): CompilationUnit? {
        val lr = LineReader(r)
        val cu = StaticJavaParser.parse(lr)
        return cu
    }

    @Throws(IOException::class)
    public override fun generate(r: Reader): TreeContext {
        val lr = LineReader(r)
        return try {
            val cu = StaticJavaParser.parse(lr)

            //val printer = DotPrinter(true)
            //FileWriter("ast.dot").use { fileWriter -> PrintWriter(fileWriter).use { printWriter -> printWriter.print(printer.output(cu)) } }

            val v = JavaParserVisitor(lr)
            v.visitPreOrder(cu)
            v.treeContext
        } catch (e: ParseProblemException) {
            throw SyntaxException(e.message, e)
        }
    }
}