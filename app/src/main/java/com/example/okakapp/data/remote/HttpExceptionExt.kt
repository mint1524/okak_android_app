package com.example.okakapp.data.remote

import kotlinx.serialization.json.Json
import retrofit2.HttpException

private val json = Json { ignoreUnknownKeys = true }

fun HttpException.parseError(): ApiError? {
    val body = response()?.errorBody()?.string() ?: return null
    return runCatching { json.decodeFromString<ApiError>(body) }.getOrNull()
}
