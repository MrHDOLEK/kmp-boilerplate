package com.kmpboilerplate.application.action.cat

import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import com.kmpboilerplate.application.viewmodel.mapper.cat.CatViewModelMapper
import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.domain.shared.resultOf

class GetRandomCatAction(
    private val catService: CatService,
    private val mapper: CatViewModelMapper,
) {
    suspend operator fun invoke(): Result<CatViewModel> = resultOf { mapper.map(catService.getRandomCat()) }
}
