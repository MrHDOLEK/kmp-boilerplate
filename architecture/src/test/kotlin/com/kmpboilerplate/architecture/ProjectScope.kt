package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

/**
 * The code the rules judge. [production] is what ships; every rule about layers, names and files reads it and
 * nothing else, because tests are free to reach across layers to wire fakes together. [tests] is every source set
 * Konsist reads as a test one — its name holds "test": commonTest of shared, any platform test source set, and this
 * module's own — and only the test conventions read it (TestConventionTest).
 */
object ProjectScope {
    val production: KoScope by lazy { Konsist.scopeFromProduction() }

    val tests: KoScope by lazy { Konsist.scopeFromTest() }

    fun inPackage(prefix: String): KoScope =
        production.slice { file ->
            val name = file.packagee?.name.orEmpty()

            name == prefix || name.startsWith("$prefix.")
        }

    fun inModule(moduleName: String): KoScope = production.slice { file -> file.moduleName == moduleName }
}
