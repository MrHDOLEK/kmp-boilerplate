package com.kmpboilerplate.architecture

internal object SuppressAnnotation {
    val pattern = Regex("""(?<!\w)`?Suppress(?:Warnings|Lint)?`?\s*\(""")

    fun findAll(symbols: String): Sequence<MatchResult> = pattern.findAll(symbols)

    fun linesOf(text: String): Set<Int> {
        val symbols = KotlinSources.symbolsOf(text)

        return findAll(symbols)
            .map { annotation -> lineAt(symbols, annotation.range.first) }
            .toSet()
    }

    fun lineAt(
        text: String,
        offset: Int,
    ): Int = text.substring(0, offset).count { char -> char == '\n' }
}
