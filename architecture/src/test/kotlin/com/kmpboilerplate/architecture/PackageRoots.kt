package com.kmpboilerplate.architecture

object PackageRoots {
    private val standard =
        setOf(
            "android",
            "androidx",
            "com",
            "dalvik",
            "io",
            "java",
            "javax",
            "jdk",
            "kotlin",
            "kotlinx",
            "okhttp3",
            "org",
            "platform",
            "sun",
        )

    val known: Set<String> by lazy {
        ProjectScope.production.imports
            .map { import -> import.name.substringBefore('.') }
            .toSet() + standard
    }
}
