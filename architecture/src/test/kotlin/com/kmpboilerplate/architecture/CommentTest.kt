package com.kmpboilerplate.architecture

import kotlin.test.Test
import kotlin.test.assertTrue

class CommentTest {
    @Test
    fun `should say in code what a comment would explain`() {
        val files = SourceTree.kotlinFiles
        val offenders = files.flatMap { file -> commentsIn(SourceTree.pathOf(file), file.readText()) }

        assertTrue(files.isNotEmpty(), "No Kotlin file found under ${SourceTree.root}; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$NO_COMMENTS\n${offenders.joinToString(separator = "\n")}")
    }

    private fun commentsIn(
        path: String,
        text: String,
    ): List<String> {
        val symbols = KotlinSources.symbolsOf(text)
        val reasonLines = reasonLinesOf(text)
        val codeLines = KotlinSources.codeOf(text).lines()
        val inTestSourceSet = isTestSourceSet(path)

        return KotlinSources
            .commentsOf(text)
            .filterNot { range -> isDocumentation(text, symbols, range) }
            .filterNot { range -> SuppressAnnotation.lineAt(text, range.first) in reasonLines }
            .filterNot { range -> inTestSourceSet && isArrangeActAssertMarker(text, codeLines, range) }
            .map { range ->
                "$path:${SuppressAnnotation.lineAt(text, range.first) + 1}: " +
                    text.substring(range).lineSequence().first()
            }
    }

    private fun isDocumentation(
        text: String,
        symbols: String,
        range: IntRange,
    ): Boolean =
        text.startsWith(KDOC, range.first) &&
            !text.startsWith(EMPTY_BLOCK, range.first) &&
            holdsOnlyBlockTags(text.substring(range)) &&
            DECLARATION_START.matchesAt(symbols, firstTokenAfter(symbols, range.last + 1))

    private fun holdsOnlyBlockTags(kdoc: String): Boolean =
        kdoc
            .removePrefix(KDOC)
            .removeSuffix(BLOCK_END)
            .lines()
            .map { line -> line.trim().removePrefix("*").trim() }
            .firstOrNull { line -> line.isNotEmpty() }
            ?.startsWith("@") == true

    private fun isTestSourceSet(path: String): Boolean {
        val segments = path.split('/')
        val sourceSet =
            segments
                .indexOf(SOURCE_ROOT)
                .takeIf { index -> index >= 0 }
                ?.let { index -> segments.getOrNull(index + 1) }

        return sourceSet != null && (sourceSet.endsWith("Test") || sourceSet.endsWith("test"))
    }

    private fun isArrangeActAssertMarker(
        text: String,
        codeLines: List<String>,
        range: IntRange,
    ): Boolean =
        text.substring(range).trimEnd() in ARRANGE_ACT_ASSERT &&
            codeLines[SuppressAnnotation.lineAt(text, range.first)].isBlank()

    private fun firstTokenAfter(
        symbols: String,
        from: Int,
    ): Int {
        var at = from

        while (at < symbols.length && symbols[at].isWhitespace()) at++

        return at
    }

    private fun reasonLinesOf(text: String): Set<Int> {
        val rawLines = text.lines()
        val codeLines = KotlinSources.codeOf(text).lines()

        return SuppressAnnotation.linesOf(text).flatMapTo(mutableSetOf()) { line ->
            val above = line - 1

            if (above >= 0 && isLineComment(rawLines[above], codeLines[above])) listOf(line, above) else listOf(line)
        }
    }

    private fun isLineComment(
        raw: String,
        code: String,
    ): Boolean = code.isBlank() && raw.trimStart().startsWith(LINE_COMMENT)

    private companion object {
        const val KDOC = "/**"
        const val EMPTY_BLOCK = "/**/"
        const val BLOCK_END = "*/"
        const val LINE_COMMENT = "//"
        const val SOURCE_ROOT = "src"

        val ARRANGE_ACT_ASSERT = setOf("// Arrange", "// Act", "// Assert")

        val DECLARATION_START =
            Regex(
                """@|(?:public|private|protected|internal|open|final|abstract|sealed|data|enum|annotation|""" +
                    """inner|value|inline|noinline|crossinline|override|lateinit|const|suspend|tailrec|""" +
                    """operator|infix|external|expect|actual|vararg|companion|fun|val|var|class|interface|""" +
                    """object|typealias|constructor|init)\b|`?[A-Za-z_]\w*`?\s*:(?!:)|[A-Z][A-Z0-9_]*\s*[,;({}]""",
            )

        const val NO_COMMENTS =
            "Code explains itself - production, tests and Gradle scripts alike: rename, extract a function or a " +
                "value, and drop the comment. The only comments allowed are block tags that documentation tools " +
                "read (@param, @property, @return…) on a declaration, the reason next to a @Suppress, and the " +
                "exact // Arrange, // Act and // Assert markers in a test source set."
    }
}
