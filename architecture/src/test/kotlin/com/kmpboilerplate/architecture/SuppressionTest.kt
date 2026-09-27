package com.kmpboilerplate.architecture

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Quality gates: a rule — Konsist's, detekt's or ktlint's — is satisfied by fixing the
 * code, never by silencing it, and the one suppression a review lets through says why — in a comment
 * on the same line, or in a `//` line right above it.
 *
 * The annotations are found in the code, so a comment or a string that quotes one is not one. The
 * spellings are the ones the tools honour: `Suppress`, `SuppressWarnings` and Android's
 * `SuppressLint`, bare, backticked or qualified however it is spaced, on a declaration, on the file
 * (`@file:`) or on a use-site target (`@get:`) — [SuppressAnnotation], which [CommentTest] reads too.
 *
 * The arguments are then read three times, once the way each tool reads them, because none of the
 * three reads a string literal as the compiler does — and a rule matches if any reading names it:
 *
 * - **Konsist** takes the annotation's raw text between its parentheses, comments included, splits
 *   it on every comma, trims each piece and drops one quote from either end, then compares it with the
 *   test's name, bare or behind `konsist.`. A comma inside a comment therefore starts an argument of
 *   its own.
 * - **detekt** takes each argument's expression, drops every `detekt.` and `detekt:` wherever it
 *   stands, then every quote, and compares what is left with the rule, its rule set and `all`. A raw
 *   string with quotes inside it therefore reaches the rule its letters spell.
 * - **ktlint** takes each argument's expression with one pair of surrounding quotes removed; `ktlint`
 *   alone is the whole of it.
 *
 * These are plain assertions on purpose. A Konsist assertion here would honour the very suppression
 * it looks for: a file annotated with this class's test name would drop out of its own check.
 */
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

    /** ktlint checks every source set and every script, so this reads all of them, tests included. */
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

    /** No guard against an empty list: the code suppresses nothing today, and that is the state to keep. */
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

    /** The `)` that closes the one at [open], counted in code only, so a parenthesis in a string is text. */
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

    /**
     * Each argument between [open] and [close], split at the commas that stand in the argument list
     * itself — not in a string, a comment or a nested call.
     */
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

    /** An argument's expression, as the PSI gives it: without the comments around it or the name before it. */
    private fun expressionOf(argument: String): String = argument.trim().replace(NAMED, "")

    /** Every test name in the architecture module — the names Konsist takes from the stack as it asserts. */
    private fun architectureRules(): Set<String> {
        val names =
            SourceTree.kotlinFiles
                .filter { file -> SourceTree.pathOf(file).startsWith(ARCHITECTURE_SOURCES) }
                .flatMap { file -> TEST_NAME.findAll(file.readText()).map { match -> match.groupValues[1] } }
                .toSet()

        assertTrue(names.isNotEmpty(), "No architecture test found under $ARCHITECTURE_SOURCES; the path is stale.")

        return names + names.map { name -> "$KONSIST_PREFIX$name" }
    }

    /**
     * A line of its own that gives the reason: a `//` comment with a word in it. A KDoc ending right
     * above the annotation documents the declaration, not the suppression, so it does not count.
     */
    private fun isReason(
        raw: String,
        code: String,
    ): Boolean = code.isBlank() && raw.trimStart().startsWith("//") && explains(raw, code)

    /** A word in a comment: a letter the source has where the code, with its comments blanked, has none. */
    private fun explains(
        raw: String,
        code: String,
    ): Boolean = raw.indices.any { at -> code[at] == ' ' && raw[at].isLetter() }

    /**
     * A string literal read as a sentence: Konsist test names are backticked sentences, and no compiler,
     * detekt or ktlint id holds a space. It catches a suppression of a test before the test exists.
     */
    private fun namesAnArchitectureRule(literal: String): Boolean = ' ' in literal || literal.startsWith(KONSIST_PREFIX)

    /** With every `detekt.` and `detekt:` dropped, compared to LongMethod's ids. */
    private fun namesLongMethod(argument: String): Boolean = LONG_METHOD.matches(argument.replace(DETEKT_PREFIX, ""))

    /**
     * One suppression and its arguments: the string literals the compiler sees, and the pieces each
     * tool compares with its rule ids after reading the annotation its own way.
     */
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

        /** A named argument's `name =`; `==` is a comparison and stays. */
        val NAMED = Regex("""^\w+\s*=(?!=)\s*""")

        val TEST_NAME = Regex("""fun\s+`([^`]+)`\s*\(""")

        /**
         * detekt's own prefix. detekt removes it wherever it stands and as often as it occurs, not once
         * at the start, so `detekt:detekt:LongMethod` and `Longdetekt.Method` both reach the rule.
         */
        val DETEKT_PREFIX = Regex("""detekt[.:]""", RegexOption.IGNORE_CASE)

        /**
         * Every id detekt reads as LongMethod once [DETEKT_PREFIX] is gone from the argument — wherever it
         * stood: the rule, its rule set, and `all`.
         */
        val LONG_METHOD = Regex("""(?:complexity[.:])?LongMethod|complexity|all""", RegexOption.IGNORE_CASE)

        /**
         * `ktlint` is the whole of it. ktlint reads `ktlint:<id>` as one rule of the standard set, but the
         * gate asks for the qualified `ktlint:standard:<rule>`, so that a rule set's name — which reads
         * like all of its rules — is never what a suppression says.
         */
        val WHOLE_OF_KTLINT = Regex("""ktlint(?::[\w-]+)?""", RegexOption.IGNORE_CASE)
    }
}
