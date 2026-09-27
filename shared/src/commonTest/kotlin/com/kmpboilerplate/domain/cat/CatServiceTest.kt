package com.kmpboilerplate.domain.cat

import com.kmpboilerplate.domain.cat.FakeCatRepository.Companion.cat
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CatServiceTest {
    @Test
    fun `should request the first page of twenty cats`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(cats = (1..30).map { index -> cat(id = "$index") })
            val service = CatService(repository)

            // Act
            val cats = service.getCats(tag = null)

            // Assert
            assertEquals(CatService.PAGE_SIZE, cats.size)
            assertEquals(0, repository.lastRequestedSkip)
            assertEquals(20, repository.lastRequestedLimit)
        }

    @Test
    fun `should request no tag filter when no tag is selected`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(cats = listOf(cat(id = "1", tags = listOf("cute"))))
            val service = CatService(repository)

            // Act
            service.getCats(tag = null)

            // Assert
            assertEquals(emptyList(), repository.lastRequestedTags)
        }

    @Test
    fun `should filter cats by the selected tag`() =
        runTest {
            // Arrange
            val repository =
                FakeCatRepository(
                    cats =
                        listOf(
                            cat(id = "1", tags = listOf("cute")),
                            cat(id = "2", tags = listOf("funny")),
                            cat(id = "3", tags = listOf("cute", "funny")),
                        ),
                )
            val service = CatService(repository)

            // Act
            val cats = service.getCats(tag = "funny")

            // Assert
            assertEquals(listOf("funny"), repository.lastRequestedTags)
            assertEquals(listOf("2", "3"), cats.map { cat -> cat.id })
        }

    @Test
    fun `should cap the tags at twenty`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(tags = (1..30).map { index -> "tag$index" })
            val service = CatService(repository)

            // Act
            val tags = service.getTags()

            // Assert
            assertEquals(CatService.TAG_CAP, tags.size)
            assertEquals("tag1", tags.first())
            assertEquals("tag20", tags.last())
        }

    @Test
    fun `should leave out blank tags`() =
        runTest {
            // Arrange
            val repository = FakeCatRepository(tags = listOf("", "cute", " ", "funny"))
            val service = CatService(repository)

            // Act
            val tags = service.getTags()

            // Assert
            assertEquals(listOf("cute", "funny"), tags)
        }
}
