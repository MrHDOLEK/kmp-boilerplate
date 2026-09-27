package com.kmpboilerplate.application.viewmodel.mapper.cat

import com.kmpboilerplate.application.viewmodel.cat.CatViewModel
import com.kmpboilerplate.domain.cat.Cat

class CatViewModelMapper {
    fun map(cat: Cat): CatViewModel =
        CatViewModel(
            id = cat.id,
            imageUrl = cat.imageUrl,
            tags = cat.tags,
        )

    fun mapCollection(cats: List<Cat>): List<CatViewModel> = cats.map { cat -> map(cat) }
}
