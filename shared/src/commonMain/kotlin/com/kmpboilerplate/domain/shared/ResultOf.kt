package com.kmpboilerplate.domain.shared

import kotlin.coroutines.cancellation.CancellationException

inline fun <T> resultOf(block: () -> T): Result<T> {
    val result = runCatching(block)
    val failure = result.exceptionOrNull()

    if (failure is CancellationException) {
        throw failure
    }

    return result
}
