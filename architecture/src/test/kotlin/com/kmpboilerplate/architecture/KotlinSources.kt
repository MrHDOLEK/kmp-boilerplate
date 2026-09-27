package com.kmpboilerplate.architecture

object KotlinSources {
    private val aroundDot = Regex("""(?<!\s)\s*\.\s*""")

    private val qualified = Regex("""(?:(?<![\w.])|(?<=\.\.))(?:[a-z][A-Za-z0-9_]*\.)+[A-Za-z_]\w*""")

    private val identifier = Regex("""[A-Za-z_]\w*""")

    private val directive = Regex("""^[ \t]*(?:package|import)[ \t]+[\w.`* \t]+;?[ \t]*$""", RegexOption.MULTILINE)

    fun codeOf(text: String): String = Scan(text, blankLiterals = false).result()

    fun symbolsOf(text: String): String = Scan(text, blankLiterals = true).result()

    fun charactersOf(text: String): String = Scan(text, blankLiterals = false, decodeEscapes = true).result()

    fun commentsOf(text: String): List<IntRange> =
        Scan(text, blankLiterals = false).also { scan -> scan.result() }.comments

    fun qualifiedNamesOf(text: String): List<String> =
        qualified
            .findAll(
                symbolsOf(text)
                    .replace("`", "")
                    .replace(aroundDot, "."),
            ).map { match -> match.value }
            .toList()

    fun bodyOf(text: String): String = directive.replace(symbolsOf(text)) { match -> " ".repeat(match.value.length) }

    fun aliasedIn(declaration: String): String = symbolsOf(declaration).substringAfter("typealias").substringAfter('=')

    fun identifiersOf(text: String): Set<String> =
        identifier
            .findAll(symbolsOf(text).replace("`", ""))
            .map { match -> match.value }
            .toSet()

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

        private fun closesRawString(at: Int): Boolean = text.startsWith(RAW, at) && !text.startsWith("$RAW\"", at)

        private fun charLiteral(from: Int): Int {
            var at = from + 1

            while (at < text.length && text[at] != '\'' && text[at] != '\n') {
                at = if (text[at] == '\\') escape(at) else hide(at, at + 1)
            }

            return minOf(at + 1, text.length)
        }

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
