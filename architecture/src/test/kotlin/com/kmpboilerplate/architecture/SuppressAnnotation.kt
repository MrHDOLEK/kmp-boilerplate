package com.kmpboilerplate.architecture

/**
 * Where a suppression annotation stands in a file, read the one way [SuppressionTest] and [CommentTest]
 * both need it, so that the two agree on what a suppression is and where its reason may sit.
 */
internal object SuppressAnnotation {
    /**
     * The annotation's own name, whatever stands before it. The tools differ in what they honour —
     * detekt takes the bare text only, ktlint the name alone, so `kotlin .Suppress`, a qualifier
     * split over lines, a use-site target (`@get:`) and a backticked name switch ktlint off like the
     * bare one — and this reads the widest of them. It is matched in [KotlinSources.symbolsOf], so a
     * comment or a string that quotes one is not one.
     */
    val pattern = Regex("""(?<!\w)`?Suppress(?:Warnings|Lint)?`?\s*\(""")

    /** Every suppression annotation in [symbols], a view of the file from [KotlinSources.symbolsOf]. */
    fun findAll(symbols: String): Sequence<MatchResult> = pattern.findAll(symbols)

    /** The zero-based line of every suppression annotation in [text]. */
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
