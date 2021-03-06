package ludwig.script

import ludwig.changes.*
import ludwig.interpreter.ClassType
import ludwig.model.*
import ludwig.utils.isField
import ludwig.workspace.Workspace
import org.pcollections.HashPMap
import org.pcollections.HashTreePMap
import java.io.IOException
import java.io.Reader

class Parser private constructor(private val tokens: List<String>, private val workspace: Workspace) {

    private var pos: Int = 0
    private var locals: HashPMap<String, NamedNode> = HashTreePMap.empty()
    private var superFunction: Node? = null

    @Throws(ParserException::class)
    private fun parse(projectNode: ProjectNode) {
        val packageNode = parseSignatures(projectNode)
        parseBodies(packageNode)
    }

    @Throws(ParserException::class)
    private fun parseSignatures(projectNode: ProjectNode): PackageNode {
        consume("(")
        consume("package")

        val packageName = nextToken()
        val packageNode = projectNode.stream().map { it as PackageNode }
                .filter { it.name == packageName }
                .findFirst()
                .orElseGet { append(projectNode, PackageNode().apply { name = packageName }) }

        consume(")")


        while (pos < tokens.size) {
            parseSignature(packageNode)
        }

        return packageNode
    }

    @Throws(ParserException::class)
    private fun parseSignature(packageNode: PackageNode) {
        consume("(")

        when (nextToken()) {
            "class" -> {
                val classNode = append(packageNode, ClassNode().apply { name = nextToken() })
                if (currentToken() != ")") {
                    val superClass = find(nextToken()) as ClassNode?
                    appendRef(classNode, superClass)
                }
                while (currentToken() != ")") {
                    append(classNode, VariableNode().apply { name = nextToken() })
                }
                consume(")")
                ClassType.of(classNode)
            }
            "def" -> {
                var lazy = false
                if (currentToken() == "lazy") {
                    lazy = true
                    consume("lazy")
                }
                val fn = append(packageNode, FunctionNode().apply { name = nextToken(); this.lazy = lazy })
                while (currentToken() != ")") {
                    append(fn, VariableNode().apply { name = nextToken() })
                }
                consume(")")

                skipBody()
            }
            "method" -> {
                while (nextToken() != ")");
                skipBody()
            }
        }
    }

    @Throws(ParserException::class)
    private fun skipBody() {
        consume("(")
        var level = 1
        while (level != 0 && pos < tokens.size) {
            when (nextToken()) {
                "(" -> level++
                ")" -> level--
            }
        }
    }

    @Throws(ParserException::class)
    private fun parseBodies(packageNode: PackageNode) {
        rewind()
        consume("(")
        consume("package")

        nextToken()
        consume(")")

        while (pos < tokens.size) {
            parseBody(packageNode)
        }
    }

    @Throws(ParserException::class)
    private fun parseBody(packageNode: PackageNode) {
        consume("(")
        when (nextToken()) {
            "class" -> while (nextToken() != ")");
            "def" -> {
                if (currentToken() == "lazy") {
                    consume("lazy")
                }
                val node = item(packageNode, nextToken()) as FunctionNode?
                locals = HashTreePMap.empty<String, NamedNode>()
                for (child in node!!) {
                    if (child !is VariableNode) {
                        break
                    }
                    locals = locals.plus(child.name, child)
                }
                while (nextToken() != ")");
                consume("(")
                while (pos < tokens.size && currentToken() != ")") {
                    parseChild(node)
                }

                if (pos < tokens.size) {
                    nextToken()
                }
            }
            "method" -> {
                val classNode = find(nextToken()) as ClassNode
                val fn = find(nextToken()) as FunctionNode
                val node = append(packageNode, OverrideNode())
                appendRef(node, fn)

                superFunction = findSuper(classNode, fn)


                locals = HashTreePMap.empty<String, NamedNode>()

                for (child in fn) {
                    if (child !is VariableNode) {
                        break
                    }
                    locals = locals.plus(child.name, child)
                }

                for (child in classNode) {
                    if (child !is VariableNode) {
                        continue
                    }
                    locals = locals.plus(child.name, child)
                }

                while (nextToken() != ")");
                consume("(")
                while (pos < tokens.size && currentToken() != ")") {
                    parseChild(node)
                }

                if (pos < tokens.size) {
                    nextToken()
                }

                ClassType.of(classNode).overrides().put(fn, node)
            }
            "field" -> {
                nextToken()
                consume(")")
            }
        }
    }

    private fun findSuper(classNode: ClassNode, fn: FunctionNode): Node {
        var t: ClassType? = ClassType.of(classNode)
        val s = t!!.implementation(fn)

        while (t != null) {
            val s1 = t.implementation(fn)
            if (s1 !== s) {
                return s1
            }

            t = t.superClass()
        }
        return fn
    }


    @Throws(ParserException::class)
    private fun parseChild(parent: Node) {
        var level = 0
        while (currentToken() == "(") {
            level++
            nextToken()
        }

        parseChildBody(parent)

        for (i in 0 until level) {
            consume(")")
        }
    }

    private fun parseChildBody(parent: Node) {
        val head = nextToken()

        when (head) {
            "call", "if", "else", "return", "throw", "try", "catch", "list", "break", "continue" -> {
                val node = append(parent, createSpecial(head)!!)
                while (currentToken() != ")") {
                    parseChild(node)
                }
            }
            "ref" -> {
                val ref = append(parent, RefNode())
                appendRef(ref, find(nextToken()))
            }
            "for" -> {
                val node = append(parent, ForNode())
                val variable = VariableNode()
                variable.name = nextToken()
                val savedLocals = locals
                locals = locals.plus(variable.name, append(node, variable))

                while (currentToken() != ")") {
                    parseChild(node)
                }
                locals = savedLocals
            }
            "=" -> {
                val name = nextToken()
                val node = append(parent, AssignmentNode())

                var isField = false
                val savedPos = pos

                val f = find(name)
                if (isField(f)) {
                    val r = appendRef(node, f)
                    parseChild(r)
                    if (currentToken() != ")") {
                        parseChild(node)
                        isField = true
                    }
                }

                if (!isField) {
                    pos = savedPos
                    node.clear()
                    if (locals.containsKey(name)) {
                        appendRef(node, locals[name])
                        parseChild(node)
                    } else {
                        val lhs = append(node, VariableNode().apply { this.name = name })
                        locals = locals.plus(name, lhs)
                        parseChild(node)
                    }
                }
            }
            "λ", "\\" -> {
                val node = append(parent, LambdaNode())
                val savedLocals = locals
                while (currentToken() != ")") {
                    val param = append(node, VariableNode().apply { name = nextToken() })
                    locals = locals.plus(param.name, param)
                }
                consume(")")
                consume("(")
                while (currentToken() != ")") {
                    parseChild(node)
                }
                locals = savedLocals
            }

            else -> {
                if (locals.containsKey(head)) {
                    val local = locals[head]
                    if (isField(local)) {
                        val savedPos = pos
                        val fn = local as VariableNode
                        val r = appendRef(parent, fn)
                        if (currentToken() == ")") {
                            pos = savedPos
                            parent.removeAt(parent.size - 1)
                        } else {
                            parseChild(r)
                            return
                        }
                    } else {
                        appendRef(parent, local)
                    }
                    return
                }

                val headNode = if ("super" == head) superFunction else find(head)
                if (headNode is FunctionNode) {
                    val fn = headNode as FunctionNode?
                    val r = appendRef(parent, fn)
                    for (param in fn!!) {
                        if (param !is VariableNode) {
                            break
                        }
                        parseChild(r)
                    }
                } else if (headNode is OverrideNode) {
                    val fn = (headNode[0] as SymbolNode).ref as FunctionNode
                    val r = appendRef(parent, headNode)
                    for (param in fn) {
                        if (param !is VariableNode) {
                            break
                        }
                        parseChild(r)
                    }
                } else if (headNode is ClassNode) {
                    val cn = headNode as ClassNode?
                    val r = appendRef(parent, cn)
                    while (currentToken() != ")") {
                        parseChild(r)
                    }
                } else if (Lexer.isLiteral(head)) {
                    append(parent, LiteralNode().apply { text = head })
                } else {
                    throw ParserException("Unknown symbol: " + head)
                }
            }
        }
    }

    private fun nextToken(): String {
        return tokens[pos++]
    }

    private fun currentToken(): String {
        return tokens[pos]
    }

    @Throws(ParserException::class)
    private fun consume(token: String) {
        if (nextToken() != token) {
            throw ParserException("Expected " + token)
        }
    }

    private fun rewind() {
        pos = 0
    }

    // TODO: Optimize
    private fun find(name: String): NamedNode? {
        return workspace.projects
                .flatMap { it }
                .map { it as PackageNode }
                .firstOrNull { item(it, name) != null }
                ?.let { item(it, name) }
    }

    private fun createSpecial(token: String): Node? {
        when (token) {
            "call" -> return CallNode()
            "if" -> return IfNode()
            "else" -> return ElseNode()
            "return" -> return ReturnNode()
            "list" -> return ListNode()
            "throw" -> return ThrowNode()
            "try" -> return TryNode()
            "catch" -> return ClassNode()
            "break" -> return BreakNode()
            "continue" -> return ContinueNode()
        }
        return null
    }

    private fun <T : Node> append(parent: Node, node: T): T {
        val create = Create(node::class.simpleName!!, parent.id, prev = if (parent.isEmpty()) null else parent.last().id)
        val changes = mutableListOf<Change>(create)
        if (node is NamedNode) {
            create.changeId = parent.id + ":" + node.name
            changes.add(Rename(create.changeId, node.name))
        }
        if (node is LiteralNode) {
            changes.add(Value(create.changeId, node.text))
        }
        if (node is FunctionNode && node.lazy) {
            changes.add(Lazy(create.changeId, true))
        }
        workspace.apply(*changes.toTypedArray())
        return workspace.node(create.changeId) as T
    }

    private fun appendRef(parent: Node?, node: Node?): SymbolNode {
        val create = Create(SymbolNode::class.simpleName!!, parent!!.id, prev = if (parent.isEmpty()) null else parent.last().id)
        val value = Value().apply { nodeId = create.changeId; value = node!!.id }
        workspace.apply(create, value)
        return workspace.node(create.changeId) as SymbolNode
    }

    companion object {


        @Throws(ParserException::class, IOException::class, LexerException::class)
        fun parse(reader: Reader, workspace: Workspace, projectNode: ProjectNode) {
            parse(Lexer.read(reader), workspace, projectNode)
        }

        @Throws(ParserException::class)
        fun parse(tokens: List<String>, workspace: Workspace, projectNode: ProjectNode) {
            Parser(tokens, workspace).parse(projectNode)
        }

        fun item(findByName: PackageNode, name: String): NamedNode? {
            return findByName.stream().filter { it is NamedNode }.map { it as NamedNode }.filter { it.name == name }.findFirst().orElse(null)
        }
    }
}
