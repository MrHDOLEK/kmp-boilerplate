package com.kmpboilerplate.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope

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
