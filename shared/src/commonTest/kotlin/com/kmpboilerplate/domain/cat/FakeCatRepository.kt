package com.kmpboilerplate.domain.cat

class FakeCatRepository(
    private val cats: List<Cat> = emptyList(),
    private val tags: List<String> = emptyList(),
    private val shouldThrow: Boolean = false,
) : CatRepositoryInterface {
    var lastRequestedTags: List<String>? = null
        private set
    var lastRequestedSkip: Int? = null
        private set
    var lastRequestedLimit: Int? = null
        private set

    override suspend fun getRandomCat(): Cat {
        failIfRequested()
        return cats.first()
    }

    override suspend fun getCats(
        tags: List<String>,
        skip: Int,
        limit: Int,
    ): List<Cat> {
        failIfRequested()
        lastRequestedTags = tags
        lastRequestedSkip = skip
        lastRequestedLimit = limit
        return cats
            .filter { cat -> tags.isEmpty() || cat.tags.any { tag -> tag in tags } }
            .drop(skip)
            .take(limit)
    }

    override suspend fun getTags(): List<String> {
        failIfRequested()
        return tags
    }

    private fun failIfRequested() {
        if (shouldThrow) {
            throw IllegalStateException(ERROR_MESSAGE)
        }
    }

    companion object {
        const val ERROR_MESSAGE = "Repository error"

        fun cat(
            id: String,
            tags: List<String> = emptyList(),
        ): Cat = Cat(id = id, tags = tags, imageUrl = "https://example.com/cat/$id")
    }
}
