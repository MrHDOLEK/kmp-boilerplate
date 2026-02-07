package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.repository.CatRepositoryInterface

class GetCatTagsAction(
    private val catRepository: CatRepositoryInterface,
) {
    suspend operator fun invoke(): Result<List<String>> =
        try {
            Result.success(catRepository.getTags())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
