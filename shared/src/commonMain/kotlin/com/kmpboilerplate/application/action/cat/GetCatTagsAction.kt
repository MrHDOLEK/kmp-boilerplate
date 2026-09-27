package com.kmpboilerplate.application.action.cat

import com.kmpboilerplate.domain.cat.CatService
import com.kmpboilerplate.domain.shared.resultOf

class GetCatTagsAction(
    private val catService: CatService,
) {
    suspend operator fun invoke(): Result<List<String>> = resultOf { catService.getTags() }
}
