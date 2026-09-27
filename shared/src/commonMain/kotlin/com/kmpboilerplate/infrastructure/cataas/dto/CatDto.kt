package com.kmpboilerplate.infrastructure.cataas.dto

import kotlinx.serialization.Serializable

@Serializable
data class CatDto(
    val id: String,
    val tags: List<String> = emptyList(),
)
