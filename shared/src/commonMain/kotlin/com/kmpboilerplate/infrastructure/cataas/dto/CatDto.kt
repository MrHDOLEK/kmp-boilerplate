package com.kmpboilerplate.infrastructure.cataas.dto

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CatDto(
    val id: String,
    val tags: List<String> = emptyList(),
    val mimetype: String? = null,
    @SerialName("created_at")
    @JsonNames("createdAt")
    val createdAt: String? = null,
)
