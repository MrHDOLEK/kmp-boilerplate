package com.kmpboilerplate.application.action.cat

import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import com.kmpboilerplate.application.viewmodel.mapper.cat.CatViewModelMapper
import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.domain.shared.resultOf

class GetCatsAction(
    private val catService: CatService,
    private val mapper: CatViewModelMapper,
) {
    suspend operator fun invoke(tag: String? = null): Result<List<CatViewModel>> =
        resultOf { mapper.mapCollection(catService.getCats(tag)) }
}
