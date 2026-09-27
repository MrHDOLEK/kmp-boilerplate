package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFileDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration
import com.lemonappdev.konsist.api.declaration.KoImportDeclaration
import com.lemonappdev.konsist.api.declaration.KoInterfaceDeclaration
import com.lemonappdev.konsist.api.declaration.KoObjectDeclaration
import com.lemonappdev.konsist.api.declaration.KoPropertyDeclaration
import com.lemonappdev.konsist.api.declaration.KoTypeAliasDeclaration
import com.lemonappdev.konsist.api.provider.KoNameProvider
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Testing conventions, held over [ProjectScope.tests] — the only rules that read the test source sets: a test is a
 * backticked sentence starting with `should`; it runs in commonTest, and so on every target, unless it needs the
 * JVM; and a test double is a hand-written fake, never a mock.
 *
 * Plain assertions, as in [SuppressionTest]: a Konsist assertion honours a `@Suppress` on what it judges, and
 * SuppressionTest reads production code only, so a test file could silence a Konsist rule about itself. Each rule
 * checks instead that it found something to judge, where there is something to find.
 *
 * Not checked, so that it is written down: where a fake lives, and the Arrange-Act-Assert layout.
 */
class TestConventionTest {
    /**
     * A test is a function annotated `Test` — kotlin.test's or JUnit's, bare, qualified or under an import alias.
     * Konsist reports a name without its backticks, so the name is read from the code: the first one after `fun`,
     * as written. The tests of this module are held to it as well.
     */
    @Test
    fun `should name every test a backticked sentence starting with should`() {
        val tests = ProjectScope.tests.files.flatMap { file -> testsIn(file).map { test -> file to test } }
        val offenders =
            tests
                .filterNot { (_, test) -> isSentence(test) }
                .map { (file, test) -> "${pathOf(file)}: ${test.name}" }

        assertTrue(
            tests.any { (file, _) -> file.moduleName == SHARED },
            "No test of shared found; the scope is misconfigured.",
        )
        assertTrue(offenders.isEmpty(), "$SENTENCES\n${offenders.joinToString(separator = "\n")}")
    }

    /**
     * Left in desktopTest or androidUnitTest, a test that could run in commonTest silently drops the other targets'
     * coverage, iOS above all. A test there uses what only the JVM has — see [needsTheJvm].
     *
     * No guard against an empty list: every test lives in commonTest today, which is what the rule asks for.
     */
    @Test
    fun `should keep in a jvm test source set only the tests that need the jvm`() {
        val jvmOnly = jvmOnlyNames()
        val offenders =
            jvmTestFiles()
                .filter { file -> testsIn(file).isNotEmpty() }
                .filterNot { file -> needsTheJvm(file, jvmOnly) }
                .map(::pathOf)

        assertTrue(offenders.isEmpty(), "$COMMON_FIRST\n${offenders.joinToString(separator = "\n")}")
    }

    /** No mocking library imported or named qualified, and nothing the tests declare named as a mock. */
    @Test
    fun `should use fakes and no mocking library in the tests`() {
        val files = ProjectScope.tests.files
        val libraries =
            files.flatMap { file ->
                (file.imports.map { import -> import.name } + KotlinSources.qualifiedNamesOf(file.text))
                    .filter { name -> MOCKING_LIBRARIES.any { library -> ImportRule.reaches(name, library) } }
                    .distinct()
                    .map { name -> "${pathOf(file)}: $name" }
            }
        val doubles =
            files.flatMap { file ->
                DECLARED
                    .findAll(KotlinSources.symbolsOf(file.text))
                    .map { declared -> declared.groupValues[1] }
                    .filter { name -> NAMED_AS_MOCK.containsMatchIn(name) }
                    .distinct()
                    .map { name -> "${pathOf(file)}: $name" }
                    .toList()
            }
        val offenders = libraries + doubles

        assertTrue(files.isNotEmpty(), "No test files found; the scope is misconfigured.")
        assertTrue(offenders.isEmpty(), "$FAKES\n${offenders.joinToString(separator = "\n")}")
    }

    private fun testsIn(file: KoFileDeclaration): List<KoFunctionDeclaration> {
        val aliases = ImportRule.aliasesIn(file)

        return file
            .functions(includeNested = true, includeLocal = false)
            .filter { function -> isTest(function, aliases) }
    }

    private fun isTest(
        function: KoFunctionDeclaration,
        aliases: Map<String, String>,
    ): Boolean = function.annotations.any { annotation -> (aliases[annotation.name] ?: annotation.name) == TEST }

    /** The first name after `fun`, as written: in backticks, `should` and a space first. */
    private fun isSentence(test: KoFunctionDeclaration): Boolean =
        HEADER
            .find(KotlinSources.symbolsOf(test.text))
            ?.groupValues
            ?.get(1)
            ?.startsWith(SHOULD) == true

    private fun jvmTestFiles(): List<KoFileDeclaration> =
        ProjectScope.tests.files.filter { file -> file.sourceSetName in JVM_TESTS }

    /**
     * What only the JVM carries, by fully qualified name: what androidMain or desktopMain declares and commonMain
     * does not, and every helper of a JVM test source set that uses one of those or a JVM package. A helper that
     * needs nothing of the JVM makes no caller need it either.
     */
    private fun jvmOnlyNames(): Set<String> {
        val production = ProjectScope.production.files
        val helpers = jvmTestFiles().filter { file -> testsIn(file).isEmpty() }
        var names =
            JVM_MAINS.flatMap { sourceSet -> declaredIn(production, sourceSet) }.toSet() -
                declaredIn(production, COMMON_MAIN)

        do {
            val known = names

            names = known + helpers.filter { helper -> needsTheJvm(helper, known) }.flatMap(::declaredNamesOf)
        } while (names != known)

        return names
    }

    /**
     * Whether [file] uses what only the JVM carries: a name reaching [JVM_PACKAGES] or [jvmOnly] that it imports
     * and reads, or writes out qualified; a name of [jvmOnly] from its own package; or a class java.lang supplies
     * with no import. An import the code never reads, a wildcard import and a JUnit name kotlin.test mirrors are no
     * reason to stay on the JVM.
     */
    private fun needsTheJvm(
        file: KoFileDeclaration,
        jvmOnly: Set<String>,
    ): Boolean {
        val body = KotlinSources.bodyOf(file.text)
        val read = KotlinSources.identifiersOf(body)
        val packageName = file.packagee?.name.orEmpty()
        val imported =
            file.imports
                .filter { import -> !import.isWildcard && nameOf(import) in read }
                .map { import -> import.name }
        val besideIt =
            jvmOnly.filter { name ->
                name.substringBeforeLast('.') == packageName && name.substringAfterLast('.') in read
            }
        val named = imported + KotlinSources.qualifiedNamesOf(body) + besideIt
        val shadowing = file.imports.map(::nameOf).toSet()
        val compact = body.replace("`", "").replace(AROUND_DOT, ".")

        return named.any { name -> isJvmOnly(name, jvmOnly) } ||
            JAVA_LANG.findAll(compact).any { match -> match.groupValues[1] !in shadowing } ||
            CLASS_JAVA.containsMatchIn(compact)
    }

    private fun isJvmOnly(
        name: String,
        jvmOnly: Set<String>,
    ): Boolean =
        MIRRORED_BY_KOTLIN_TEST.none { mirrored -> ImportRule.reaches(name, mirrored) } &&
            (JVM_PACKAGES + jvmOnly).any { jvm -> ImportRule.reaches(name, jvm) }

    private fun declaredIn(
        files: List<KoFileDeclaration>,
        sourceSet: String,
    ): Set<String> = files.filter { file -> file.sourceSetName == sourceSet }.flatMap(::declaredNamesOf).toSet()

    private fun declaredNamesOf(file: KoFileDeclaration): List<String> =
        file
            .declarations(includeNested = false, includeLocal = false)
            .filter(::isDeclaration)
            .mapNotNull { declaration -> (declaration as? KoNameProvider)?.name }
            .map { name -> "${file.packagee?.name.orEmpty()}.$name" }

    private fun isDeclaration(declaration: KoBaseDeclaration): Boolean =
        declaration is KoClassDeclaration ||
            declaration is KoInterfaceDeclaration ||
            declaration is KoObjectDeclaration ||
            declaration is KoTypeAliasDeclaration ||
            declaration is KoFunctionDeclaration ||
            declaration is KoPropertyDeclaration

    private fun nameOf(import: KoImportDeclaration): String = import.alias?.name ?: import.name.substringAfterLast('.')

    private fun pathOf(file: KoFileDeclaration): String = file.projectPath.trimStart('/')

    private companion object {
        const val SHARED = "shared"
        const val COMMON_MAIN = "commonMain"
        const val TEST = "Test"
        const val SHOULD = "`should "

        /** The test source sets that run on the JVM alone, and the main source sets they see beside commonMain. */
        val JVM_TESTS = setOf("desktopTest", "androidUnitTest")
        val JVM_MAINS = listOf("androidMain", "desktopMain")

        const val SENTENCES =
            "A test is named with a backticked sentence that starts with should, such as `should return the tags`."

        const val COMMON_FIRST =
            "A test that needs no JVM belongs in commonTest, where every target runs it. desktopTest and " +
                "androidUnitTest keep a test that uses what only the JVM has - the JDK, JUnit beyond what " +
                "kotlin.test mirrors, OkHttp, the code of androidMain or desktopMain."

        const val FAKES =
            "A test double is a hand-written fake, such as FakeCatRepository: no mocking library, and nothing " +
                "named as a mock."

        /**
         * What the JVM test classpaths carry and commonTest's does not: the JDK, JUnit (kotlin-test's JVM variant),
         * OkHttp, and the Android and desktop libraries. A new JVM-only test dependency is added here with it.
         */
        val JVM_PACKAGES =
            listOf(
                "java",
                "javax",
                "jdk",
                "sun",
                "org.junit",
                "junit",
                "kotlin.test.junit",
                "android",
                "androidx",
                "okhttp3",
                "io.ktor.client.engine.okhttp",
            )

        /** JUnit names kotlin.test has a common twin of: writing them keeps a test on the JVM for nothing. */
        val MIRRORED_BY_KOTLIN_TEST =
            listOf("org.junit.Test", "org.junit.Before", "org.junit.After", "org.junit.Ignore", "org.junit.Assert")

        val MOCKING_LIBRARIES =
            listOf(
                "io.mockk",
                "org.mockito",
                "com.nhaarman.mockitokotlin2",
                "dev.mokkery",
                "io.mockative",
                "org.kodein.mock",
                "org.easymock",
                "org.powermock",
                "org.jmock",
            )

        val HEADER = Regex("""\bfun\s+(?:<[^>]*>\s*)?(`[^`\n]*`|\w+)""")

        /** Whitespace around a dot, matched from the start of its run only — see KotlinSources. */
        val AROUND_DOT = Regex("""(?<!\s)\s*\.\s*""")

        /** A java.lang class the JVM supplies without an import. */
        val JAVA_LANG = Regex("""(?<![\w.])(System|Thread|Runtime|ProcessBuilder|ClassLoader|Math|Integer)\.""")

        val CLASS_JAVA = Regex("""::\s*class\.java\b""")

        val DECLARED = Regex("""\b(?:class|interface|object|typealias|val|var|fun)\s+`?([A-Za-z_]\w*)""")

        val NAMED_AS_MOCK = Regex("""^mock|Mock""")
    }
}
