package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

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

        val NON_ASCII_LETTER = Regex("""[\p{L}&&[^\x00-\x7F]]""")

        val STRING_LITERAL = Regex(""""(\\.|[^"\\\n])*"""")

        val TEMPLATE = Regex("""\$\{[^}]*}|\$[A-Za-z_]\w*""")

        val WORD_BY_SPACE = Regex("""\p{L}{2,}\p{P}*\s|\s\p{P}*\p{L}{2,}""")

        val FAILURE_MESSAGE = Regex("""\b(require|requireNotNull|check|checkNotNull|error)\s*\(|\bthrow\b""")
    }
}
