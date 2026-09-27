package com.kmpboilerplate.architecture

/**
 * Kotlin source read the way the compiler reads it: a comment is a comment and a string is a
 * string, whatever characters either of them holds.
 *
 * The rules below are about what the code says, not what it explains: a word in a KDoc is prose,
 * the same word in a literal is text the app shows. Every view blanks rather than deletes, so each
 * character keeps its offset and each line its index, and a rule can point at, or exempt, the line
 * it judges.
 *
 * One pass from the first character to the last decides what everything is, so a URL inside a
 * string is not a line comment, a glob inside a string opens no block comment, a quote inside a
 * comment or a char literal opens no string, and the code of a string template is still code.
 * Kotlin nests block comments, and so does the scan.
 */
object KotlinSources {
    /**
     * Whitespace around a dot, matched from the start of its run only: a blanked KDoc is one long
     * run, and trying every position inside it would cost the square of its length.
     */
    private val aroundDot = Regex("""(?<!\s)\s*\.\s*""")

    /**
     * A dotted name that starts a segment chain: not after a letter, a digit or a member dot, but after
     * the range operator, which ends in a dot and is followed by an expression like any other operator.
     */
    private val qualified = Regex("""(?:(?<![\w.])|(?<=\.\.))(?:[a-z][A-Za-z0-9_]*\.)+[A-Za-z_]\w*""")

    private val identifier = Regex("""[A-Za-z_]\w*""")

    private val directive = Regex("""^[ \t]*(?:package|import)[ \t]+[\w.`* \t]+;?[ \t]*$""", RegexOption.MULTILINE)

    /** Comments blanked; string and char literals kept, because some rules read what a literal says. */
    fun codeOf(text: String): String = Scan(text, blankLiterals = false).result()

    /** Comments and the text of every literal blanked; the code of a string template is kept. */
    fun symbolsOf(text: String): String = Scan(text, blankLiterals = true).result()

    /**
     * Comments blanked and every literal as the compiler reads it: a \u escape in a string or char literal
     * is the character it spells, followed by blanks so each offset holds. A raw string has no escapes, so
     * a \u in one stays six characters of text.
     */
    fun charactersOf(text: String): String = Scan(text, blankLiterals = false, decodeEscapes = true).result()

    /**
     * Where each comment sits, in source order: a `//` to the end of its line, a `/* */` together with the
     * comments nested in it.
     */
    fun commentsOf(text: String): List<IntRange> =
        Scan(text, blankLiterals = false).also { scan -> scan.result() }.comments

    /**
     * The dotted names the code spells — `com.kmpboilerplate.x.Y`, `kotlinx.serialization.Serializable`.
     * Backticks and whitespace around a dot are dropped first, so neither a quoted segment nor a line
     * break inside the name hides it.
     *
     * Package and import lines are read like any other. `import` is a soft keyword and an ordinary
     * name everywhere else, so a line that starts with it may be an expression; and a name on a real
     * import line is one the same rule's import half already forbids.
     */
    fun qualifiedNamesOf(text: String): List<String> =
        qualified
            .findAll(
                symbolsOf(text)
                    .replace("`", "")
                    .replace(aroundDot, "."),
            ).map { match -> match.value }
            .toList()

    /**
     * The symbols below a file's header: comments, literals, and the package and import directives blanked, offsets
     * kept. What the code does with a name, rather than what the file says it may use: an import the code never reads
     * is not a use.
     */
    fun bodyOf(text: String): String = directive.replace(symbolsOf(text)) { match -> " ".repeat(match.value.length) }

    /** What a typealias declaration stands for: its right-hand side, comments and strings blanked. */
    fun aliasedIn(declaration: String): String = symbolsOf(declaration).substringAfter("typealias").substringAfter('=')

    /** Every identifier the code spells, import lines included. */
    fun identifiersOf(text: String): Set<String> =
        identifier
            .findAll(symbolsOf(text).replace("`", ""))
            .map { match -> match.value }
            .toSet()

    /**
     * The offset of the bracket closing the one at [open], in a view whose comments and literals are
     * blanked — [symbolsOf] — so that the brackets left in it balance.
     */
    fun closingOf(
        symbols: String,
        open: Int,
    ): Int {
        val opening = symbols[open]
        val closing =
            when (opening) {
                '{' -> '}'
                '(' -> ')'
                '[' -> ']'
                else -> error("No bracket at offset $open")
            }
        var depth = 0

        for (at in open until symbols.length) {
            when (symbols[at]) {
                opening -> depth++
                closing -> if (--depth == 0) return at
            }
        }

        error("Unbalanced $opening at offset $open")
    }

    private class Scan(
        private val text: String,
        private val blankLiterals: Boolean,
        private val decodeEscapes: Boolean = false,
    ) {
        private val view = text.toCharArray()

        val comments = mutableListOf<IntRange>()

        fun result(): String {
            code(from = 0, inTemplate = false)

            return String(view)
        }

        /** Code up to the end of the text, or up to the brace that closes a `${` template. */
        private fun code(
            from: Int,
            inTemplate: Boolean,
        ): Int {
            var at = from
            var braces = 0

            while (at < text.length) {
                when (text[at]) {
                    '{' -> {
                        braces++
                    }

                    '}' -> {
                        if (inTemplate && braces == 0) return at + 1

                        braces--
                    }
                }

                at = step(at)
            }

            return at
        }

        private fun step(at: Int): Int =
            when {
                text.startsWith("//", at) -> comment(at, endOfLine(at))
                text.startsWith("/*", at) -> comment(at, endOfBlockComment(at))
                text.startsWith(RAW, at) -> literal(at + RAW.length, raw = true)
                text[at] == '"' -> literal(at + 1, raw = false)
                text[at] == '\'' -> charLiteral(at)
                text[at] == '`' -> endOfQuotedName(at)
                else -> at + 1
            }

        private fun literal(
            from: Int,
            raw: Boolean,
        ): Int {
            var at = from

            while (at < text.length) {
                at =
                    when {
                        raw && closesRawString(at) -> return at + RAW.length
                        !raw && text[at] == '"' -> return at + 1
                        !raw && text[at] == '\n' -> return at
                        text.startsWith(TEMPLATE, at) -> code(at + TEMPLATE.length, inTemplate = true)
                        text[at] == '$' && isNameStart(text.getOrNull(at + 1)) -> endOfName(at + 1)
                        !raw && text[at] == '\\' -> escape(at)
                        else -> hide(at, at + 1)
                    }
            }

            return at
        }

        /** `""""x""""` closes on its last three quotes: the ones before them are text. */
        private fun closesRawString(at: Int): Boolean = text.startsWith(RAW, at) && !text.startsWith("$RAW\"", at)

        private fun charLiteral(from: Int): Int {
            var at = from + 1

            while (at < text.length && text[at] != '\'' && text[at] != '\n') {
                at = if (text[at] == '\\') escape(at) else hide(at, at + 1)
            }

            return minOf(at + 1, text.length)
        }

        /** A backslash and what it escapes; when escapes are decoded, a \u escape is the one character it spells. */
        private fun escape(at: Int): Int {
            val unicode = if (decodeEscapes) UNICODE.matchAt(text, at) else null

            if (unicode == null) return hide(at, at + 2)

            view[at] = Char(unicode.groupValues[1].toInt(HEX))

            return blank(at + 1, at + unicode.value.length)
        }

        private fun endOfBlockComment(from: Int): Int {
            var at = from + 2
            var depth = 1

            while (at < text.length && depth > 0) {
                when {
                    text.startsWith("/*", at) -> {
                        depth++
                        at += 2
                    }

                    text.startsWith("*/", at) -> {
                        depth--
                        at += 2
                    }

                    else -> {
                        at++
                    }
                }
            }

            return minOf(at, text.length)
        }

        /** A backticked name may hold a space, a quote or a slash; none of them is a token of its own. */
        private fun endOfQuotedName(from: Int): Int {
            val close = text.indexOf('`', from + 1)
            val end = endOfLine(from)

            return if (close in 0 until end) close + 1 else end
        }

        private fun endOfLine(from: Int): Int = text.indexOf('\n', from).takeIf { end -> end >= 0 } ?: text.length

        private fun endOfName(from: Int): Int {
            var at = from

            while (at < text.length && (text[at].isLetterOrDigit() || text[at] == '_')) at++

            return at
        }

        private fun isNameStart(char: Char?): Boolean = char != null && (char.isLetter() || char == '_')

        private fun comment(
            from: Int,
            until: Int,
        ): Int {
            comments += from until minOf(until, text.length)

            return blank(from, until)
        }

        private fun hide(
            from: Int,
            until: Int,
        ): Int = if (blankLiterals) blank(from, until) else minOf(until, text.length)

        private fun blank(
            from: Int,
            until: Int,
        ): Int {
            val end = minOf(until, text.length)

            for (at in from until end) {
                if (view[at] != '\n' && view[at] != '\r') view[at] = ' '
            }

            return end
        }

        private companion object {
            const val RAW = "\"\"\""

            const val TEMPLATE = "\${"

            const val HEX = 16

            val UNICODE = Regex("""\\u([0-9a-fA-F]{4})""")
        }
    }
}
