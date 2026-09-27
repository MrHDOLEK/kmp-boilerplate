package com.kmpboilerplate.architecture

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Comments: production code says what it does through its names and its shape.
 * A `//` or `/* */` comment inside it is a sentence the code failed to say, and the fix is the code — a
 * name, an extracted function, a value object — not the comment.
 *
 * Two comments stay, each where it belongs:
 *
 * - **Block tags on a declaration.** A `/** */` holding only block tags that documentation tools read —
 *   `@param`, `@property`, `@return`, `@throws`… — is kept, the way a PHP docblock keeps its
 *   annotations. Prose in a KDoc is narration like any other comment. The block counts only when the
 *   first code token after it starts a declaration: an annotation (`@`), a modifier or declaration
 *   keyword (`private`, `fun`, `val`, `class`, `init`…), a parameter (`name:`), or an enum entry (a
 *   SCREAMING_SNAKE name followed by `,`, `;`, `(`, `{` or `}`). The check reads that one token only, so
 *   a tag block on a local declaration inside a body passes.
 * - **The reason next to a suppression**, where [SuppressionTest] looks for it: any comment on the
 *   annotation's own line, or one `//` line directly above it. The annotation is found by
 *   [SuppressAnnotation], so both tests read the same spellings.
 *
 * Tests are outside this rule; it reads production source sets only.
 */
class CommentTest {
    @Test
    fun `should say in code what a comment would explain`() {
        val files = ProjectScope.production.files
        val offenders =
            files.flatMap { file ->
                commentsIn(file.projectPath.replace('\\', '/').trimStart('/'), file.text)
            }

        assertTrue(files.isNotEmpty(), "No production file found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$NO_COMMENTS\n${offenders.joinToString(separator = "\n")}")
    }

    private fun commentsIn(
        path: String,
        text: String,
    ): List<String> {
        val symbols = KotlinSources.symbolsOf(text)
        val reasonLines = reasonLinesOf(text)

        return KotlinSources
            .commentsOf(text)
            .filterNot { range -> isDocumentation(text, symbols, range) }
            .filterNot { range -> SuppressAnnotation.lineAt(text, range.first) in reasonLines }
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

    /** @return true when the first line with any text in the block starts with a block tag. */
    private fun holdsOnlyBlockTags(kdoc: String): Boolean =
        kdoc
            .removePrefix(KDOC)
            .removeSuffix(BLOCK_END)
            .lines()
            .map { line -> line.trim().removePrefix("*").trim() }
            .firstOrNull { line -> line.isNotEmpty() }
            ?.startsWith("@") == true

    private fun firstTokenAfter(
        symbols: String,
        from: Int,
    ): Int {
        var at = from

        while (at < symbols.length && symbols[at].isWhitespace()) at++

        return at
    }

    /**
     * The lines a suppression's reason may start on: the annotation's own line, and the line directly
     * above it when that line is a `//` comment and nothing else.
     */
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

        /** What may follow a KDoc: see the class KDoc. Matched at the first code token after it. */
        val DECLARATION_START =
            Regex(
                """@|(?:public|private|protected|internal|open|final|abstract|sealed|data|enum|annotation|""" +
                    """inner|value|inline|noinline|crossinline|override|lateinit|const|suspend|tailrec|""" +
                    """operator|infix|external|expect|actual|vararg|companion|fun|val|var|class|interface|""" +
                    """object|typealias|constructor|init)\b|`?[A-Za-z_]\w*`?\s*:(?!:)|[A-Z][A-Z0-9_]*\s*[,;({}]""",
            )

        const val NO_COMMENTS =
            "Production code explains itself: rename, extract a function or a value, and drop the comment. " +
                "Block tags that documentation tools read (@param, @property, @return…) on a declaration and the " +
                "reason next to a @Suppress are the only comments allowed."
    }
}
