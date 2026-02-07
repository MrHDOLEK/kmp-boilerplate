package com.kmpboilerplate.infrastructure.config

import com.kmpboilerplate.application.action.GetCatTagsAction
import com.kmpboilerplate.application.action.GetCatsAction
import com.kmpboilerplate.application.action.GetRandomCatAction
import com.kmpboilerplate.domain.repository.CatRepositoryInterface
import com.kmpboilerplate.infrastructure.http.createHttpClient
import com.kmpboilerplate.infrastructure.repository.CatRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val networkModule =
    module {
        single { createHttpClient() }
    }

val repositoryModule =
    module {
        singleOf(::CatRepository) bind CatRepositoryInterface::class
    }

val actionModule =
    module {
        factoryOf(::GetRandomCatAction)
        factoryOf(::GetCatsAction)
        factoryOf(::GetCatTagsAction)
    }

val container = listOf(networkModule, repositoryModule, actionModule)
