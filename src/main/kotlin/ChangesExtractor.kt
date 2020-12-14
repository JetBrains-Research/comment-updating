import com.github.gumtreediff.actions.ChawatheScriptGenerator
import com.github.gumtreediff.actions.EditScriptGenerator
import com.github.gumtreediff.actions.SimplifiedChawatheScriptGenerator
import com.github.gumtreediff.actions.model.*
import com.github.gumtreediff.client.Run
import com.github.gumtreediff.gen.TreeGenerators
import com.github.gumtreediff.gen.jdt.JdtTreeGenerator
import com.github.gumtreediff.matchers.MappingStore
import com.github.gumtreediff.matchers.Matcher
import com.github.gumtreediff.matchers.Matchers
import com.github.gumtreediff.tree.ITree
import com.github.gumtreediff.tree.Tree
import com.github.gumtreediff.tree.TreeContext
import com.github.gumtreediff.tree.Type
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.comments.JavadocComment
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.Charset
import com.github.gumtreediff.actions.EditScript





fun main() {
    val srcFile = "sample.java"
    val dstFile = "sample2.java"
    val samples = ChangesExtractor().extract(File(srcFile).readBytes(), File(dstFile).readBytes())
    println(samples)

}

class ChangesExtractor {
    init {
        Run.initGenerators()
    }

    public fun extract(srcFile: File, dstFile: File): List<Sample> {
        val src = JdtTreeGenerator().generate(InputStreamReader(srcFile.inputStream())).root
        val dst = JdtTreeGenerator().generate(InputStreamReader(dstFile.inputStream())).root
        val defaultMatcher: Matcher = Matchers.getInstance().matcher
        val mappings: MappingStore = defaultMatcher.match(src, dst)
        val editScriptGenerator: EditScriptGenerator = ChawatheScriptGenerator()
        val actions = editScriptGenerator.computeActions(mappings)
        val srcActions = mutableListOf<Pair<Int, Int>>()
        val dstActions = mutableListOf<Pair<Int, Int>>()
        for (action in actions) {
            val range = action.node.pos to action.node.endPos
            when(action) {
                is Insert -> dstActions.add(range)
                is Move -> null // Can't do anything, ignore this changes
                is Delete -> srcActions.add(range)
                is Update -> srcActions.add(range)
                is TreeInsert -> srcActions.add(range)
                is TreeDelete -> dstActions.add(range)
            }
        }
        val intersectNames = listMethodsNames(dst as Tree).intersect(listMethodsNames(src as Tree)).toSet()
        val srcChanged = extractChangedMethods(src as Tree, srcActions)
        val dstChanged = extractChangedMethods(dst as Tree, dstActions)
        val changed = srcChanged.union(dstChanged)
        val samples = mutableListOf<Sample>()
        for (node in changed) {
            val name: String = (node.getMetadata("NAME") as? String) ?: ""
            if (name in intersectNames) {
                val srcPair = extractCodeComment(node, srcFile.readText())
                val dstPair = extractCodeComment(node, dstFile.readText())
                samples.add(Sample(srcPair.second, srcPair.first, dstPair.second, dstPair.first))
            }
        }
        return samples
    }


    // TODO: Catch SyntaxException from JdtTreeGenerator
    public fun extract(srcBytes: ByteArray, dstBytes: ByteArray): List<Sample> {
        val srctext = srcBytes.toString(Charsets.UTF_8)
        val dsttext = dstBytes.toString(Charsets.UTF_8)
        val src = JdtTreeGenerator().generate(InputStreamReader(srcBytes.inputStream())).root
        val dst = JdtTreeGenerator().generate(InputStreamReader(dstBytes.inputStream())).root
        val defaultMatcher: Matcher = Matchers.getInstance().matcher
        val mappings: MappingStore = defaultMatcher.match(src, dst)
        val editScriptGenerator: EditScriptGenerator = ChawatheScriptGenerator()
        val actions = editScriptGenerator.computeActions(mappings)
        val srcActions = mutableListOf<Pair<Int, Int>>()
        val dstActions = mutableListOf<Pair<Int, Int>>()
        for (action in actions) {
            val range = action.node.pos to action.node.endPos
            when(action) {
                is Insert -> dstActions.add(range)
                is Move -> null // Can't do anything, ignore this changes
                is Delete -> srcActions.add(range)
                is Update -> srcActions.add(range)
                is TreeInsert -> srcActions.add(range)
                is TreeDelete -> dstActions.add(range)
            }
        }
        val intersectNames = listMethodsNames(dst as Tree).intersect(listMethodsNames(src as Tree)).toSet()
        val srcChanged = extractChangedMethods(src as Tree, srcActions)
        val dstChanged = extractChangedMethods(dst as Tree, dstActions)
        val samples = mutableListOf<Sample>()
        for (srcNode in srcChanged) {
            val name: String = (srcNode.getMetadata("NAME") as? String) ?: ""
            if (name in intersectNames) {
                val dstNode = mappings.getDstForSrc(srcNode)
                if (dstNode.type.name == "MethodDeclaration") {
                    val srcPair = extractCodeComment(srcNode, srcBytes.toString(Charsets.UTF_8))
                    val dstPair = extractCodeComment(dstNode as Tree, dstBytes.toString(Charsets.UTF_8))
                    samples.add(Sample(srcPair.second, srcPair.first, dstPair.second, dstPair.first))
                }
            }
        }
        for (dstNode in dstChanged) {
            val name: String = (dstNode.getMetadata("NAME") as? String) ?: ""
            if (name in intersectNames) {
                val srcNode = mappings.getSrcForDst(dstNode)
                if (dstNode.type.name == "MethodDeclaration") {
                    val srcPair = extractCodeComment(srcNode as Tree, srcBytes.toString(Charsets.UTF_8))
                    val dstPair = extractCodeComment(dstNode, dstBytes.toString(Charsets.UTF_8))
                    samples.add(Sample(srcPair.second, srcPair.first, dstPair.second, dstPair.first))
                }
            }
        }
        return samples
    }


    private fun extractCodeComment(node: Tree, code: String): Pair<String, String> {
        // TODO: try catch on is MethodDeclaration

//        println("DEBUG: CODE:${node.children.get(1).pos to
//            node.endPos} COMMENT:${
//            node.children.get(0).pos to
//            node.children.get(0).endPos}")
         return (
                code.substring(
                    node.children.get(1).pos,
                    node.endPos)
                ) to (
                code.substring(
                    node.children.get(0).pos,
                    node.children.get(0).endPos
                ))
    }

    private fun listMethodsNames(node: Tree, path: String = ""): List<String> {
        return if (node.type.name == "MethodDeclaration" && node.children.any { it.type.name == "Javadoc" }) {
            val name = node.children.find { it.type.name == "SimpleName"}?.label
            node.setMetadata("NAME", "$path.${name ?: ""}")
            listOf("$path.${name ?: ""}")
        } else {
            val list = mutableListOf<String>()
            if (node.type.name == "TypeDeclaration") {
                val name = node.children.find { it.type.name == "SimpleName"}?.label
                node.children.forEach { list.addAll(listMethodsNames(it as Tree, "$path.${name ?: ""}"))}
            } else {
                node.children.forEach { list.addAll(listMethodsNames(it as Tree, path)) }
            }
            list
        }
    }

    private fun extractChangedMethods(node: Tree, changesPoses: List<Pair<Int, Int>>): List<Tree> {
        if (changesPoses.any { (from, to) ->  node.pos  <= from && to <= node.endPos}) {
            // If it's nested methods, only top level method parsed
            if (node.type.name == "MethodDeclaration" && node.children.any { it.type.name == "Javadoc" }) {
                return listOf(node)
            }
            val list = mutableListOf<Tree>()
            node.children.forEach {
                list.addAll(extractChangedMethods(it as Tree, changesPoses))
            }
            return list
        }
        return listOf()
    }




}
