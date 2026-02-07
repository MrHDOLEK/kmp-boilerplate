package com.kmpboilerplate.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Cat(
    val id: String,
    val tags: List<String> = emptyList(),
    val mimetype: String? = null,
    val url: String? = null,
    val createdAt: String? = null,
    @SerialName("created_at") val createdAtAlt: String? = null,
) {
    val imageUrl: String get() = url ?: "https://cataas.com/cat/$id"
    val created: String? get() = createdAt ?: createdAtAlt
}
