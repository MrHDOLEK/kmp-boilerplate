package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

/**
 * Display text: code, identifiers and string literals in production Kotlin are English and plain ASCII, and the
 * words a user reads come from composeResources, one `values*` folder per language. The domain carries facts; the
 * screen writes the sentence, in whichever language the device is set to.
 *
 * It reads letters, not language: a translation written without accents, an English sentence hard-coded in a
 * screen and a key missing from one strings file pass and stay the reviewer's. A comment may hold any letter.
 */
class DisplayTextTest {
    @Test
    fun `should keep non ascii letters out of the code and its literals`() {
        ProjectScope.production.files.assertFalse(strict = true, additionalMessage = LETTERS) { file ->
            NON_ASCII_LETTER.containsMatchIn(KotlinSources.charactersOf(file.text))
        }
    }

    @Test
    fun `should keep the words a user reads out of the domain`() {
        ProjectScope.inPackage(DomainCollaborator.DOMAIN).files.assertFalse(
            strict = true,
            additionalMessage = WORDS,
        ) { file -> KotlinSources.codeOf(file.text).lines().any { line -> statesAPhrase(line) } }
    }

    /**
     * A literal is a phrase when a word of two letters or more stands against a space in it, punctuation between
     * them aside, once the values it interpolates are set aside; a phrase in the domain is a sentence the screen
     * should have written. A key, a fragment of an identifier or a separator such as " " or ", " holds none. The
     * line that throws is exempt: a failure message is read by a developer in a stack trace, never by a user.
     */
    private fun statesAPhrase(line: String): Boolean =
        !FAILURE_MESSAGE.containsMatchIn(line) &&
            STRING_LITERAL.findAll(line).any { literal ->
                WORD_BY_SPACE.containsMatchIn(literal.value.replace(TEMPLATE, ""))
            }

    private companion object {
        const val LETTERS =
            "Production Kotlin is English and ASCII; a word in another alphabet or with an accent is text a " +
                "user reads, and it belongs in composeResources."

        const val WORDS =
            "The domain carries facts; the screen writes the words. Move the text to composeResources and give " +
                "the entity the fact behind it."

        /**
         * A letter outside ASCII, in the code or in a literal as the compiler reads it: a \u escape in a string or
         * char literal is the letter it spells.
         */
        val NON_ASCII_LETTER = Regex("""[\p{L}&&[^\x00-\x7F]]""")

        /** Newline-free, so an unbalanced quote cannot swallow a whole file. */
        val STRING_LITERAL = Regex(""""(\\.|[^"\\\n])*"""")

        /** A `${…}` or `$name` template: the value it interpolates, not a word of the literal. */
        val TEMPLATE = Regex("""\$\{[^}]*}|\$[A-Za-z_]\w*""")

        /** Punctuation may stand between the word and the space: "Tags: $list" is a label a user reads. */
        val WORD_BY_SPACE = Regex("""\p{L}{2,}\p{P}*\s|\s\p{P}*\p{L}{2,}""")

        val FAILURE_MESSAGE = Regex("""\b(require|requireNotNull|check|checkNotNull|error)\s*\(|\bthrow\b""")
    }
}
