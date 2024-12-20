package prog8.code.source

import prog8.code.core.Position
import java.nio.file.Path
import kotlin.io.path.Path


// Resource caching "filesystem".
// Note that it leaves the decision to load a resource or an actual disk file to the caller.

object ImportFileSystem {
    fun getFile(path: Path): SourceCode {
        val cached = cache[path.toString()]
        if (cached != null) return cached
        val file = SourceCode.File(path)
        cache[path.toString()] = file
        return file
    }

    fun getResource(name: String): SourceCode {
        val cached = cache[name]
        if (cached != null) return cached
        val resource = SourceCode.Resource(name)
        cache[name] = resource
        return resource
    }

    fun retrieveSourceLine(position: Position): String {
        if(SourceCode.isLibraryResource(position.file)) {
            val cached = cache[SourceCode.withoutPrefix(position.file)]
            if(cached != null)
                return getLine(cached, position.line)
        }
        val cached = cache[position.file]
        if(cached != null)
            return getLine(cached, position.line)
        val path = Path(position.file).toAbsolutePath().normalize()
        val cached2 = cache[path.toString()]
        if(cached2 != null)
            return getLine(cached2, position.line)
        throw NoSuchElementException("cannot get source line $position")
    }

    private fun getLine(code: SourceCode, lineIndex: Int): String {
        var spans = lineSpanCache[code]
        if(spans==null) {
            val lineSpans = Regex("^", RegexOption.MULTILINE).findAll(code.text).map { it.range.first }
            val ends = lineSpans.drop(1) + code.text.length
            spans = lineSpans.zip(ends).map { (start, end) -> LineSpan(start, end) }.toList().toTypedArray()
            lineSpanCache[code] = spans
        }
        val span = spans[lineIndex - 1]
        return code.text.substring(span.start, span.end).trim()
    }

    private class LineSpan(val start: Int, val end: Int)

    private val cache = mutableMapOf<String, SourceCode>()
    private val lineSpanCache = mutableMapOf<SourceCode, Array<LineSpan>>()
}