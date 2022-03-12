package prog8.code.ast

import prog8.code.core.IMemSizer
import prog8.code.core.IStringEncoding
import prog8.code.core.Position
import java.util.*

// New (work-in-progress) simplified AST for the code generator.


sealed class PtNode(val position: Position, val children: MutableList<PtNode> = mutableListOf()) {

    lateinit var parent: PtNode

    protected fun printIndented(indent: Int) {
        print("    ".repeat(indent))
        print("${this.javaClass.simpleName}  ")
        printProperties()
        println()
        children.forEach { it.printIndented(indent+1) }
    }

    abstract fun printProperties()

    fun add(child: PtNode) {
        children.add(child)
        child.parent = this
    }

    fun add(index: Int, child: PtNode) {
        children.add(index, child)
        child.parent = this
    }
}


class PtNodeGroup: PtNode(Position.DUMMY) {
    override fun printProperties() {}
}


abstract class PtNamedNode(val name: String, position: Position): PtNode(position) {
    val scopedName: List<String> by lazy {
        if(this is PtModule)
            emptyList()
        else {
            var namedParent: PtNode = this.parent
            while(namedParent !is PtNamedNode)
                namedParent = namedParent.parent
            namedParent.scopedName + name
        }
    }
}


class PtProgram(
    val name: String,
    val memsizer: IMemSizer,
    val encoding: IStringEncoding
) : PtNode(Position.DUMMY) {
    fun print() = printIndented(0)
    override fun printProperties() {
        print("'$name'")
    }

    fun allModuleDirectives(): Sequence<PtDirective> =
        children.asSequence().flatMap { it.children }.filterIsInstance<PtDirective>().distinct()

    fun allBlocks(): Sequence<PtBlock> =
        children.asSequence().flatMap { it.children }.filterIsInstance<PtBlock>()

    fun entrypoint(): PtSub? =
        allBlocks().firstOrNull { it.name == "main" }?.children?.firstOrNull { it is PtSub && it.name == "start" } as PtSub?
}


class PtModule(
    name: String,
    val loadAddress: UInt?,
    val library: Boolean,
    position: Position
) : PtNamedNode(name, position) {
    override fun printProperties() {
        print("$name  addr=$loadAddress  library=$library")
    }
}


class PtBlock(name: String,
              val address: UInt?,
              val library: Boolean,
              position: Position
) : PtNamedNode(name, position) {
    override fun printProperties() {
        print("$name  addr=$address  library=$library")
    }
}


class PtDirective(var name: String, position: Position) : PtNode(position) {
    val args: List<PtDirectiveArg>
        get() = children.map { it as PtDirectiveArg }

    override fun printProperties() {
        print(name)
    }

    override fun hashCode(): Int {
        return Objects.hash(name, args)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is PtDirective)
            return false
        if(other===this)
            return true
        return(name==other.name && args.zip(other.args).all { it.first==it.second })
    }
}


class PtDirectiveArg(val str: String?,
                     val name: String?,
                     val int: UInt?,
                     position: Position
): PtNode(position) {
    override fun printProperties() {
        print("str=$str name=$name int=$int")
    }

    override fun hashCode(): Int {
        return Objects.hash(str, name, int)
    }

    override fun equals(other: Any?): Boolean {
        if(other !is PtDirectiveArg)
            return false
        if(other===this)
            return true
        return str==other.str || name==other.name || int==other.int
    }
}


class PtInlineAssembly(val assembly: String, position: Position) : PtNode(position) {
    override fun printProperties() {}
}


class PtLabel(name: String, position: Position) : PtNamedNode(name, position) {
    override fun printProperties() {
        print(name)
    }
}

