package ludwig.model

class PlaceholderNode : Node<PlaceholderNode>() {
    var parameter: String? = null


    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitPlaceholder(this)
    }

    override fun toString(): String {
        return "<$parameter>"
    }
}
