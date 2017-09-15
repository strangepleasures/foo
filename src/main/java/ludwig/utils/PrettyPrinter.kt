package ludwig.utils

import ludwig.model.*
import java.util.*

class PrettyPrinter private constructor() : NodeVisitor<Unit> {
    private val builder = StringBuilder()

    override fun toString(): String {
        if (builder.length == 0 || builder[builder.length - 1] != '\n') {
            builder.append('\n')
        }
        return builder.toString()
    }

    private var indentation: Int = 0

    override fun visitList(node: ListNode): Unit {
        print("list")
        val inline = level(node) < 4
        for (n in node.children()) {
            child(n, inline)
        }
    }

    override fun visitFunction(functionNode: FunctionNode): Unit {
        var body = false
        for (child in functionNode.children()) {
            if (child !is VariableNode) {
                body = true
            }
            if (body) {
                child(child, false)
            }
        }
    }


    override fun visitClass(classNode: ClassNode): Unit {
    }

    override fun visitCatch(catchNode: CatchNode): Unit {
        print("catch")
        catchNode.children().forEach { node -> child(node, false) }
    }


    override fun visitLiteral(literalNode: LiteralNode): Unit {
        print(literalNode.text())
    }

    override fun visitPackage(packageNode: PackageNode): Unit {
    }

    override fun visitVariable(variableNode: VariableNode): Unit {
        print(variableNode.name())
    }

    override fun visitReference(referenceNode: ReferenceNode): Unit {
        print(referenceNode.toString())
        val inline = canBeInlined(referenceNode)
        referenceNode.children().forEach { node -> child(node, inline) }
    }

    override fun visitFunctionReference(functionReference: FunctionReferenceNode): Unit {
        print("ref ")
        if (!functionReference.children().isEmpty()) {
            print(functionReference.children()[0].toString())
        }
    }

    override fun visitThrow(throwNode: ThrowNode): Unit {
        print("throw ")
        throwNode.children()[0].accept(this)
     }

    override fun visitTry(tryNode: TryNode): Unit {
        print("try")
        tryNode.children().forEach { node -> child(node, false) }
    }

    override fun visitPlaceholder(placeholderNode: PlaceholderNode): Unit {
        print(placeholderNode.toString())
    }

    override fun visitBreak(breakNode: BreakNode): Unit {
        print("break ")
        breakNode.children()[0].accept(this)
    }

    override fun visitContinue(continueNode: ContinueNode): Unit {
        print("continue ")
        continueNode.children()[0].accept(this)
    }

    override fun visitOverride(overrideNode: OverrideNode): Unit {
        for (i in 1..overrideNode.children().size - 1) {
            child(overrideNode.children()[i], false)
        }
    }

    override fun visitCall(callNode: CallNode): Unit {
        print("call")
        child(callNode.children()[0], true)
        val inline = level(callNode) < 4
        for (i in 1..callNode.children().size - 1) {
            child(callNode.children()[i], inline)
        }
    }

    override fun visitLambda(lambdaNode: LambdaNode): Unit {
        print("λ")

        var body = false
        val inline = level(lambdaNode) < 4
        for (n in lambdaNode.children()) {
            if (!body && n !is VariableNode) {
                body = true
                print(" : ")
            }
            if (body) {
                child(n, inline)
            } else {
                print(" " + n)
            }
        }
    }

    override fun visitReturn(returnNode: ReturnNode): Unit {
        print("return")
        returnNode.children().forEach { node -> child(node, true) }
    }

    override fun visitProject(projectNode: ProjectNode): Unit {
    }

    override fun visitIf(ifNode: IfNode): Unit {
        print("if")
        child(ifNode.children()[0], true)
        for (i in 1 until ifNode.children().size) {
            child(ifNode.children()[i], false)
        }
    }

    override fun visitAssignment(assignmentNode: AssignmentNode): Unit {
        print("= ")
        child(assignmentNode.children()[0], true)
        val inline = level(assignmentNode) < 4
        for (i in 1..assignmentNode.children().size - 1) {
            child(assignmentNode.children()[i], inline)
        }
    }

    override fun visitElse(elseNode: ElseNode): Unit {
        print("else")
        elseNode.children().forEach { node -> child(node, false) }
    }

    override fun visitFor(forNode: ForNode): Unit {
        print("for")
        for (i in 0..forNode.children().size - 1) {
            child(forNode.children()[i], i < 2)
        }
    }

    private fun child(node: Node<*>, inline: Boolean) {
        indentation++
        if (!inline) {
            if (builder.length > 0) {
                builder.append('\n')
            }
            for (i in 1..indentation - 1) {
                print("    ")
            }
        } else {
            print(" ")
        }
        node.accept(this)
        indentation--
    }

    internal fun print(s: String?) {
        builder.append(s)
    }

    companion object {

        fun print(parent: Node<*>): String {
            val printer = PrettyPrinter()
            parent.accept(printer)
            return printer.toString()
        }

        private fun canBeInlined(node: Node<*>): Boolean {
            if (level(node) > 3) {
                return false
            }

            for (i in 0..node.children().size - 1 - 1) {
                if (NodeUtils.argumentsCount(node.children()[i]) > 3) {
                    return false
                }
            }

            return true
        }

        private fun level(node: Node<*>): Int {
            return node.children().stream().map<Int>({ level(it) }).max(Comparator.naturalOrder()).orElse(0) + 1
        }
    }

}
