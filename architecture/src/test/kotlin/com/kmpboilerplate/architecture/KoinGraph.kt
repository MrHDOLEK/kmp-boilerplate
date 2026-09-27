package com.kmpboilerplate.architecture

/**
 * The Koin graph as the app starts it, read from the text of `infrastructure/config`.
 *
 * `bootstrap()` installs `container`, and `container` names the common modules and calls the platform
 * ones: `expect fun x(): Module` in commonMain, whose `actual fun x(): Module = module { }` in
 * androidMain, iosMain and desktopMain is each platform's body. A definition counts only in a body on that path,
 * and a platform body counts for its own platform alone. A mention anywhere else in the package — a
 * module nobody lists, an import, a comment — registers nothing at runtime, so it registers nothing
 * here either.
 *
 * Three shapes are read: `val x = module { }`, `[actual] fun x(): Module = module { }` and
 * `val container = listOf(names and calls)`. Anything else — another way of declaring a module,
 * `includes(...)`, an expression as an entry of the list — is reported as a problem rather than
 * guessed at, so a shape this reading does not know fails the gate instead of passing it.
 */
object KoinGraph {
    const val COMMON = "commonMain"

    val PLATFORMS = listOf("androidMain", "iosMain", "desktopMain")

    val ALL = listOf(COMMON) + PLATFORMS

    /** One `module { }` block: [kind] is `val` or `fun`, [code] the block with comments and literals blanked. */
    class Body(
        val kind: String,
        val name: String,
        val sourceSet: String,
        val path: String,
        val code: String,
    )

    /** The bodies the app starts, and every way the configuration strayed from the shapes above. */
    class Reading(
        val started: List<Body>,
        val problems: List<String>,
    )

    val reading: Reading by lazy { read() }

    /** The code of every started body compiled into one of [sourceSets]. */
    fun registrationsIn(sourceSets: Collection<String>): String =
        reading.started
            .filter { body -> body.sourceSet in sourceSets }
            .joinToString(separator = "\n") { body -> body.code }

    /** The classes the started bodies of [sourceSets] build: `::Name` or a constructor call `Name(`. */
    fun registeredNamesIn(sourceSets: Collection<String>): Set<String> =
        REGISTERED
            .findAll(registrationsIn(sourceSets))
            .map { match -> match.groupValues[1].ifEmpty { match.groupValues[2] } }
            .toSet()

    /**
     * For every place a started body builds [name] — `::Name` or `Name(` — the definition nearest before
     * it: `single`, `factory` or `scoped`, in the DSL or the constructor-reference form. A class built
     * inside another definition's block, `single { Foo(Bar(get())) }`, is read as that definition, which
     * is what Koin does with it.
     */
    fun definitionKindsOf(name: String): List<String> {
        val built = Regex("""::\s*$name\b|\b$name\s*\(""")

        return reading.started.flatMap { body ->
            built
                .findAll(body.code)
                .map { use ->
                    DEFINITION
                        .findAll(body.code.substring(0, use.range.first))
                        .lastOrNull()
                        ?.groupValues
                        ?.get(1) ?: UNDEFINED
                }.toList()
        }
    }

    private class Source(
        val sourceSet: String,
        val path: String,
        val code: String,
    )

    private fun read(): Reading {
        val sources =
            ProjectScope
                .inModule("shared")
                .files
                .filter { file -> file.packagee?.name == CONFIG }
                .map { file ->
                    Source(
                        sourceSet = file.sourceSetName,
                        path = file.projectPath.trimStart('/'),
                        code = KotlinSources.symbolsOf(file.text),
                    )
                }
        val problems = mutableListOf<String>()
        val declared = sources.flatMap { source -> bodiesIn(source, problems) }
        val started = startedFrom(sources, declared, problems)

        declared
            .filterNot { body -> body in started }
            .forEach { body -> problems += "${body.path}: ${body.name} is declared, but container never starts it." }

        if (sources.none { source -> INSTALL.containsMatchIn(source.code) }) {
            problems += "bootstrap() no longer installs modules(container), the root this reading starts from."
        }

        return Reading(started, problems)
    }

    private fun bodiesIn(
        source: Source,
        problems: MutableList<String>,
    ): List<Body> {
        val heads = HEAD.findAll(source.code).toList()
        val owned = heads.map { head -> head.range.last }.toSet()

        MODULE_CALL
            .findAll(source.code)
            .filterNot { call -> call.range.last in owned }
            .forEach { call ->
                problems += "${source.path}:${lineOf(source.code, call.range.first)}: a module that is neither " +
                    "val x = module { } nor fun x(): Module = module { }."
            }

        return heads.map { head ->
            Body(
                kind = head.groupValues[1],
                name = head.groupValues[2],
                sourceSet = source.sourceSet,
                path = source.path,
                code = blockAt(source.code, head.range.last),
            )
        }
    }

    private fun startedFrom(
        sources: List<Source>,
        declared: List<Body>,
        problems: MutableList<String>,
    ): List<Body> {
        val common = sources.filter { source -> source.sourceSet == COMMON }
        val lists =
            common.flatMap { source -> CONTAINER.findAll(source.code).map { match -> source to match }.toList() }

        if (lists.size != 1) {
            problems += "Expected one val container = listOf(...) in the commonMain config, found ${lists.size}."

            return emptyList()
        }

        val (source, list) = lists.single()
        val expected =
            common
                .flatMap { candidate -> EXPECT.findAll(candidate.code).map { match -> match.groupValues[1] }.toList() }
                .toSet()

        return argumentsAt(source.code, list.range.last)
            .split(',')
            .map { entry -> entry.trim() }
            .filter { entry -> entry.isNotEmpty() }
            .flatMap { entry ->
                resolve(entry, declared, expected) ?: emptyList<Body>().also {
                    problems += "container lists $entry, which is no module declared in infrastructure/config " +
                        "for every source set that compiles it."
                }
            }
    }

    /**
     * The bodies one entry of `container` starts: a common `val` for a bare name, a common `fun` for a
     * call, or — for a call to an `expect fun` — exactly one `actual` body on every platform. Null for
     * anything else.
     */
    private fun resolve(
        entry: String,
        declared: List<Body>,
        expected: Set<String>,
    ): List<Body>? {
        val parsed = ENTRY.matchEntire(entry) ?: return null
        val name = parsed.groupValues[1]
        val kind = if (parsed.groupValues[2].isEmpty()) VAL else FUN
        val common = declared.filter { body -> body.sourceSet == COMMON && body.kind == kind && body.name == name }
        val platform = declared.filter { body -> body.sourceSet in PLATFORMS && body.kind == FUN && body.name == name }
        val onEveryPlatform = PLATFORMS.all { sourceSet -> platform.count { body -> body.sourceSet == sourceSet } == 1 }

        return when {
            common.size == 1 && platform.isEmpty() -> common
            kind == FUN && common.isEmpty() && name in expected && onEveryPlatform -> platform
            else -> null
        }
    }

    private fun blockAt(
        code: String,
        open: Int,
    ): String = code.substring(open, closingOf(code, open) + 1)

    private fun argumentsAt(
        code: String,
        open: Int,
    ): String = code.substring(open + 1, closingOf(code, open))

    /** Comments and literals are already blanked, so the brackets that are left balance. */
    private fun closingOf(
        code: String,
        open: Int,
    ): Int {
        val opening = code[open]
        val closing = if (opening == '{') '}' else ')'
        var depth = 0

        for (at in open until code.length) {
            when (code[at]) {
                opening -> depth++
                closing -> if (--depth == 0) return at
            }
        }

        error("Unbalanced $opening at offset $open")
    }

    private fun lineOf(
        code: String,
        offset: Int,
    ): Int = code.take(offset).count { char -> char == '\n' } + 1

    private const val CONFIG = "com.kmpboilerplate.infrastructure.config"

    private const val VAL = "val"

    private const val FUN = "fun"

    private val HEAD =
        Regex(
            """(?m)^[ \t]*(?:(?:actual|internal|private|public)\s+)*(val|fun)\s+(\w+)\s*""" +
                """(?:\(\s*\))?\s*(?::\s*Module\s*)?=\s*module\s*\{""",
        )

    private val MODULE_CALL = Regex("""\bmodule\s*\{""")

    private val CONTAINER =
        Regex("""(?m)^[ \t]*(?:(?:internal|private|public)\s+)*val\s+container\s*(?::[^=]*)?=\s*listOf\s*\(""")

    private val EXPECT = Regex("""(?m)^[ \t]*expect\s+fun\s+(\w+)\s*\(\s*\)\s*:\s*Module\b""")

    private val ENTRY = Regex("""(\w+)\s*(\(\s*\))?""")

    private val INSTALL = Regex("""\bmodules\s*\(\s*container\s*\)""")

    private val REGISTERED = Regex("""::\s*([A-Z]\w*)|\b([A-Z]\w*)\s*\(""")

    private const val UNDEFINED = "no definition"

    private val DEFINITION = Regex("""\b(single|factory|scoped)(?:Of)?\b""")
}
