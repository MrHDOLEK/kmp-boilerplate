package com.kmpboilerplate.architecture

import java.io.File

object SourceTree {
    private const val MISSING_ROOT = "kmpboilerplate.rootDir is not set; see architecture/build.gradle.kts."

    val root: File = File(checkNotNull(System.getProperty("kmpboilerplate.rootDir")) { MISSING_ROOT }).canonicalFile

    private val outputs = setOf("build", "target")

    val kotlinFiles: List<File> by lazy { filesWith(setOf("kt", "kts")) }

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
