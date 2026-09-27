package com.kmpboilerplate.application.viewmodel.cat

data class CatViewModel(
    val id: String,
    val imageUrl: String,
    val tags: List<String>,
)
