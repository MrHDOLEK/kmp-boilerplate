package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.entity.Cat
import com.kmpboilerplate.domain.repository.CatRepositoryInterface

class GetRandomCatAction(
    private val catRepository: CatRepositoryInterface,
) {
    suspend operator fun invoke(): Result<Cat> =
        try {
            Result.success(catRepository.getRandomCat())
        } catch (e: Exception) {
            Result.failure(e)
        }
}
