
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.visitor.VoidVisitorAdapter
import java.io.File


object javaparsersample {
    @JvmStatic
    fun main(args: Array<String>) {
        val FILE_PATH = "./src/main/java/sample.java"
        val cu: CompilationUnit = StaticJavaParser.parse(File(FILE_PATH))
        print(cu)
//        val comments = cu.allComments
//            .stream()
//            .map { CommentReportEntry(it.javaClass.simpleName,
//                it.content,
//                it.range.get().begin.line,
//                if(!it.commentedNode.isEmpty) it.commentedNode.get().javaClass.simpleName else null) }
//        comments.forEach { println(it) }
//        //val methodNameVisitor = MethodNamePrinter();
        //methodNameVisitor.visit(cu, null);
    }
}

private class MethodNamePrinter() :VoidVisitorAdapter<Void>() {

    override fun visit(md: MethodDeclaration, arg: Void?) {
        super.visit(md, arg);
        System.out.println("Method Name Printed: " + md.getName());
    }
}

data class CommentReportEntry(val type: String, val text: String, val lineNumber: Int, val dad: String?) {
    override fun toString(): String {
        return "$lineNumber|$type|$dad|${text.replace("\n", "").trim()}";
    }
}