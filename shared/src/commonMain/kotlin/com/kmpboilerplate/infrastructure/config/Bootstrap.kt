package com.kmpboilerplate.infrastructure.config

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

fun bootstrap(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(container)
    }

fun bootstrap() = bootstrap {}
