package com.kmpboilerplate.application.action

import com.kmpboilerplate.domain.entity.Cat
import com.kmpboilerplate.domain.repository.CatRepositoryInterface

class FakeCatRepository(
    private val cats: List<Cat> = emptyList(),
    private val tags: List<String> = emptyList(),
    private val shouldThrow: Boolean = false,
) : CatRepositoryInterface {
    override suspend fun getRandomCat(): Cat {
        if (shouldThrow) throw RuntimeException("Repository error")
        return cats.first()
    }

    override suspend fun getCatById(id: String): Cat {
        if (shouldThrow) throw RuntimeException("Repository error")
        return cats.first { it.id == id }
    }

    override suspend fun getCats(
        tags: List<String>?,
        skip: Int,
        limit: Int,
    ): List<Cat> {
        if (shouldThrow) throw RuntimeException("Repository error")
        return cats
            .let { list -> if (tags != null) list.filter { it.tags.any { t -> t in tags } } else list }
            .drop(skip)
            .take(limit)
    }

    override suspend fun getTags(): List<String> {
        if (shouldThrow) throw RuntimeException("Repository error")
        return tags
    }
}
