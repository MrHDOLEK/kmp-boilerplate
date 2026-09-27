package com.kmpboilerplate.domain.cat

data class Cat(
    val id: String,
    val tags: List<String>,
    val imageUrl: String,
)
