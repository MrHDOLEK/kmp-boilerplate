package com.kmpboilerplate.architecture

import kotlin.test.Test
import kotlin.test.assertTrue

class SuppressionTest {
    @Test
    fun `should never silence an architecture rule`() {
        val rules = architectureRules()
        val offenders =
            suppressionsIn(ProjectScope.production.files.map { file -> file.projectPath to file.text })
                .filter { suppression ->
                    suppression.literals.any(::namesAnArchitectureRule) ||
                        suppression.asKonsistReadsThem.any(rules::contains)
                }

        assertTrue(offenders.isEmpty(), "$ARCHITECTURE\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should never silence the long method rule in the user interface`() {
        val offenders =
            suppressionsIn(ProjectScope.inModule("composeApp").files.map { file -> file.projectPath to file.text })
                .filter { suppression ->
                    (suppression.literals + suppression.asDetektReadsThem).any(::namesLongMethod)
                }

        assertTrue(offenders.isEmpty(), "$LONG_SCREEN\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should never switch ktlint off as a whole`() {
        val offenders =
            suppressionsIn(SourceTree.kotlinFiles.map { file -> SourceTree.pathOf(file) to file.readText() })
                .filter { suppression ->
                    (suppression.literals + suppression.asKtlintReadsThem).any { argument ->
                        WHOLE_OF_KTLINT.matches(argument)
                    }
                }

        assertTrue(offenders.isEmpty(), "$KTLINT\n${offenders.joinToString(separator = "\n")}")
    }

    @Test
    fun `should say why next to every suppression`() {
        val suppressions = suppressionsIn(ProjectScope.production.files.map { file -> file.projectPath to file.text })
        val offenders = suppressions.filterNot { suppression -> suppression.justified }

        assertTrue(offenders.isEmpty(), "$JUSTIFIED\n${offenders.joinToString(separator = "\n")}")
    }

    private fun suppressionsIn(files: List<Pair<String, String>>): List<Suppression> {
        assertTrue(files.isNotEmpty(), "No files found; the scope is misconfigured.")

        return files.flatMap { (path, text) -> suppressionsOf(path.trimStart('/'), text) }
    }

    private fun suppressionsOf(
        path: String,
        text: String,
    ): List<Suppression> {
        val symbols = KotlinSources.symbolsOf(text)
        val code = KotlinSources.codeOf(text)
        val rawLines = text.lines()
        val codeLines = code.lines()

        return SuppressAnnotation.findAll(symbols).toList().map { annotation ->
            val open = annotation.range.last
            val close = closingParenthesis(symbols, open)
            val line = SuppressAnnotation.lineAt(symbols, annotation.range.first)
            val expressions =
                topLevelArguments(symbols, open, close).map { range ->
                    expressionOf(code.substring(range))
                }
            val justified =
                explains(rawLines[line], codeLines[line]) ||
                    (line > 0 && isReason(rawLines[line - 1], codeLines[line - 1]))

            Suppression(
                where = "$path:${line + 1}",
                literals =
                    LITERAL
                        .findAll(code.substring(open, close))
                        .map { literal ->
                            literal.groupValues[1]
                        }.toList(),
                asKonsistReadsThem =
                    text
                        .substring(open + 1, close)
                        .split(',')
                        .map { piece -> piece.trim().removePrefix(QUOTE).removeSuffix(QUOTE) },
                asDetektReadsThem =
                    expressions.map { expression ->
                        expression.replace(DETEKT_PREFIX, "").replace(QUOTE, "")
                    },
                asKtlintReadsThem = expressions.map { expression -> expression.removeSurrounding(QUOTE) },
                justified = justified,
            )
        }
    }

    private fun closingParenthesis(
        symbols: String,
        open: Int,
    ): Int {
        var depth = 0

        for (at in open until symbols.length) {
            when (symbols[at]) {
                '(' -> depth++
                ')' -> if (--depth == 0) return at
            }
        }

        return symbols.length
    }

    private fun topLevelArguments(
        symbols: String,
        open: Int,
        close: Int,
    ): List<IntRange> {
        val arguments = mutableListOf<IntRange>()
        var start = open + 1
        var depth = 0

        for (at in open + 1 until close) {
            when (symbols[at]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                ',' ->
                    if (depth == 0) {
                        arguments += start until at
                        start = at + 1
                    }
            }
        }

        return arguments + listOf(start until close)
    }

    private fun expressionOf(argument: String): String = argument.trim().replace(NAMED, "")

    private fun architectureRules(): Set<String> {
        val names =
            SourceTree.kotlinFiles
                .filter { file -> SourceTree.pathOf(file).startsWith(ARCHITECTURE_SOURCES) }
                .flatMap { file -> TEST_NAME.findAll(file.readText()).map { match -> match.groupValues[1] } }
                .toSet()

        assertTrue(names.isNotEmpty(), "No architecture test found under $ARCHITECTURE_SOURCES; the path is stale.")

        return names + names.map { name -> "$KONSIST_PREFIX$name" }
    }

    private fun isReason(
        raw: String,
        code: String,
    ): Boolean = code.isBlank() && raw.trimStart().startsWith("//") && explains(raw, code)

    private fun explains(
        raw: String,
        code: String,
    ): Boolean = raw.indices.any { at -> code[at] == ' ' && raw[at].isLetter() }

    private fun namesAnArchitectureRule(literal: String): Boolean = ' ' in literal || literal.startsWith(KONSIST_PREFIX)

    private fun namesLongMethod(argument: String): Boolean = LONG_METHOD.matches(argument.replace(DETEKT_PREFIX, ""))

    private data class Suppression(
        val where: String,
        val literals: List<String>,
        val asKonsistReadsThem: List<String>,
        val asDetektReadsThem: List<String>,
        val asKtlintReadsThem: List<String>,
        val justified: Boolean,
    ) {
        override fun toString(): String = "$where $asKonsistReadsThem"
    }

    private companion object {
        const val KONSIST_PREFIX = "konsist."

        const val QUOTE = "\""

        const val ARCHITECTURE_SOURCES = "architecture/src/test/"

        const val ARCHITECTURE = "Konsist rules are not suppressed; change the code or, deliberately, the rule."

        const val LONG_SCREEN = "A screen over 80 lines is split into private composables, not suppressed."

        const val KTLINT =
            "ktlint is not switched off as a whole. Fix the style, or suppress the one rule by its " +
                "qualified id (ktlint:standard:<rule>) with a reason."

        const val JUSTIFIED =
            "A suppression says why, in a comment on its line or a // line right above it, so a review can weigh it."

        val LITERAL = Regex(""""((?:[^"\\]|\\.)*)"""")

        val NAMED = Regex("""^\w+\s*=(?!=)\s*""")

        val TEST_NAME = Regex("""fun\s+`([^`]+)`\s*\(""")

        val DETEKT_PREFIX = Regex("""detekt[.:]""", RegexOption.IGNORE_CASE)

        val LONG_METHOD = Regex("""(?:complexity[.:])?LongMethod|complexity|all""", RegexOption.IGNORE_CASE)

        val WHOLE_OF_KTLINT = Regex("""ktlint(?::[\w-]+)?""", RegexOption.IGNORE_CASE)
    }
}
