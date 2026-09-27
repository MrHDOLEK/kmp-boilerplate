package com.kmpboilerplate.architecture

import java.io.File

/**
 * Every Kotlin file of the repository as it sits on disk, scripts and test sources included.
 *
 * Konsist is not that list. It reads `.kt` only, and it drops every file whose path has a `build/`
 * or a `target/` directory anywhere below the root, or a segment — a folder or the file's own name —
 * that starts with `buildsrc` in any case. A source folder of that name counts as much as any, so a
 * file placed there is judged by no rule at all. A rule that must see what Konsist hides, or a
 * `.gradle.kts` script, walks this tree instead.
 *
 * The walk skips what is not source: hidden directories (`.git`, `.gradle`, `.kotlin`, `.idea`),
 * `node_modules`, and a `build` or `target` directory outside `src/` — a module's output, often
 * thousands of generated files. The same names inside `src/` are walked, because that is exactly
 * where Konsist stops looking.
 */
object SourceTree {
    private const val MISSING_ROOT = "kmpboilerplate.rootDir is not set; see architecture/build.gradle.kts."

    val root: File = File(checkNotNull(System.getProperty("kmpboilerplate.rootDir")) { MISSING_ROOT }).canonicalFile

    private val outputs = setOf("build", "target")

    val kotlinFiles: List<File> by lazy { filesWith(setOf("kt", "kts")) }

    /** Every file of the tree whose extension is one of [extensions], in the order of its path. */
    fun filesWith(extensions: Set<String>): List<File> =
        root
            .walkTopDown()
            .onEnter(::isSearched)
            .filter { file -> file.isFile && file.extension in extensions }
            .sortedBy(::pathOf)
            .toList()

    fun pathOf(file: File): String = file.relativeTo(root).invariantSeparatorsPath

    private fun isSearched(directory: File): Boolean {
        if (directory == root) return true

        val name = directory.name
        val segments = pathOf(directory).split('/')

        return !name.startsWith('.') && name != "node_modules" && (name !in outputs || "src" in segments)
    }
}
