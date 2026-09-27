package com.kmpboilerplate.domain.cat

class CatService(
    private val catRepository: CatRepositoryInterface,
) {
    suspend fun getCats(tag: String?): List<Cat> =
        catRepository.getCats(tags = listOfNotNull(tag), skip = FIRST_PAGE_OFFSET, limit = PAGE_SIZE)

    suspend fun getRandomCat(): Cat = catRepository.getRandomCat()

    suspend fun getTags(): List<String> = catRepository.getTags().filter { tag -> tag.isNotBlank() }.take(TAG_CAP)

    companion object {
        const val PAGE_SIZE = 20
        const val TAG_CAP = 20
        private const val FIRST_PAGE_OFFSET = 0
    }
}
