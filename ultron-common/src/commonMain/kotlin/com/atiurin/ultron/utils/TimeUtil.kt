@file:OptIn(ExperimentalTime::class)

package com.atiurin.ultron.utils

import kotlin.time.*

fun now() = Clock.System.now()
fun nowMs() = now().toEpochMilliseconds()

fun measureTimeMillis(function: () -> Any): Long {
    val start = now()
    function()
    return now().minus(start).inWholeMilliseconds
}