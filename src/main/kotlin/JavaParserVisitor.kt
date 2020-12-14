import com.github.gumtreediff.io.LineReader
import com.github.gumtreediff.tree.*
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.Comment
import com.github.javaparser.ast.comments.LineComment
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.visitor.TreeVisitor
import java.lang.Boolean
import java.util.*
import java.util.function.Consumer

class JavaParserVisitor(reader: LineReader) : TreeVisitor() {
    var treeContext: TreeContext
        protected set
    private val trees: Deque<ITree>
    private val reader: LineReader
    private var path: String = ""
    override fun visitPreOrder(node: Node) {

        //println("NODE: ${node.javaClass.simpleName}")
        val temp_path = path
        process(node)
        ArrayList(node.childNodes).forEach(Consumer { node: Node -> visitPreOrder(node) })
        path = temp_path // TODO: Fix, very bad codestyle (rewrite from TreeVisitor to something else)
        if (trees.size > 0) trees.pop()
    }

    override fun process(node: Node) {
        var label: String? = ""

        if (node is Name) label = node.identifier //FIXME: might be better to flatten name hierarchies.
        else if (node is SimpleName) label = node.identifier
        else if (node is StringLiteralExpr) label = node.asString()
        else if (node is BooleanLiteralExpr) label = Boolean.toString(node.value)
        else if (node is LiteralStringValueExpr) label = node.value
        else if (node is PrimitiveType) label = node.asString()
        else if (node is Modifier) label = node.keyword.asString()
        else if (node is MethodDeclaration) {
            // Qualified method name
            path = "$path.${node.nameAsString}"
            label = path

            if (!node.comment.isEmpty) {
                pushNode(node.comment.get(), node.comment.get().content)
            }
        }
        else if (node is ClassOrInterfaceDeclaration) {
            label = node.fullyQualifiedName.orElse("")
            path = label
        }
        pushNode(node, label)
    }

    protected fun pushNode(n: Node, label: String?) {
        //try {
        val begin = n.range.get().begin
        val end = n.range.get().end
        val startPos = reader.positionFor(begin.line, begin.column)
        val length = reader.positionFor(end.line, end.column) - startPos + 2
        push(nodeAsSymbol(n), label, startPos, length)
        //} catch (ignore: NoSuchElementException) {
        //    println("NO SUCH ELEMENT $n")
        //}
    }

    protected fun nodeAsSymbol(n: Node): Type {
        return TypeSet.type(n.javaClass.simpleName)
    }

    private fun push(type: Type, label: String?, startPosition: Int, length: Int) {
        val t = treeContext.createTree(type, label)
        t.pos = startPosition
        t.length = length

        if (trees.isEmpty()) treeContext.root = t else {
            val parent = trees.peek()
            t.setParentAndUpdateChildren(parent)
        }
        trees.push(t)
    }

    init {
        treeContext = TreeContext()
        trees = ArrayDeque()
        this.reader = reader
    }
}