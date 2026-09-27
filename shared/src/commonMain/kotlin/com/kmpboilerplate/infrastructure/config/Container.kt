package com.kmpboilerplate.infrastructure.config

import com.kmpboilerplate.application.action.cat.GetCatTagsAction
import com.kmpboilerplate.application.action.cat.GetCatsAction
import com.kmpboilerplate.application.action.cat.GetRandomCatAction
import com.kmpboilerplate.application.viewmodel.mapper.cat.CatViewModelMapper
import com.kmpboilerplate.domain.cat.CatRepositoryInterface
import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.infrastructure.cataas.CatRepository
import com.kmpboilerplate.infrastructure.cataas.mapper.CatDtoMapper
import com.kmpboilerplate.infrastructure.http.createHttpClient
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val httpModule =
    module {
        single { createHttpClient() }
    }

val repositoryModule =
    module {
        singleOf(::CatRepository) bind CatRepositoryInterface::class
    }

val serviceModule =
    module {
        factoryOf(::CatService)
    }

val mapperModule =
    module {
        factoryOf(::CatDtoMapper)
        factoryOf(::CatViewModelMapper)
    }

val actionModule =
    module {
        factoryOf(::GetRandomCatAction)
        factoryOf(::GetCatsAction)
        factoryOf(::GetCatTagsAction)
    }

val container = listOf(httpModule, repositoryModule, serviceModule, mapperModule, actionModule)
