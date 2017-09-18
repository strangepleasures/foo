package ludwig.model

class ListNode : Node() {
    override fun <T> accept(visitor: NodeVisitor<T>): T {
        return visitor.visitList(this)
    }

    override fun toString(): String {
        return "list"
    }
}