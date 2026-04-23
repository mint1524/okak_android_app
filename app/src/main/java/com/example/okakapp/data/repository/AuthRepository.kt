package com.example.okakapp.data.repository

import com.example.okakapp.data.local.TokenStorage
import com.example.okakapp.data.remote.AuthRequest
import com.example.okakapp.data.remote.OkakApi
import com.example.okakapp.data.remote.UserDto
import com.example.okakapp.data.remote.parseError
import retrofit2.HttpException
import java.io.IOException

class AuthRepository(
    private val api: OkakApi,
    private val tokenStorage: TokenStorage
) {

    suspend fun login(email: String, password: String): Result<Unit> = runCatching {
        val resp = api.login(AuthRequest(email.trim(), password))
        tokenStorage.save(resp.accessToken)
    }.mapErrors()

    suspend fun register(email: String, password: String): Result<Unit> = runCatching {
        val resp = api.register(AuthRequest(email.trim(), password))
        tokenStorage.save(resp.accessToken)
    }.mapErrors()

    suspend fun me(): Result<UserDto> = runCatching { api.me() }.mapErrors()

    suspend fun logout() {
        tokenStorage.clear()
    }
}

internal fun <T> Result<T>.mapErrors(): Result<T> = recoverCatching { e ->
    when (e) {
        is HttpException -> {
            val parsed = e.parseError()
            throw ApiException(e.code(), parsed?.error ?: "HTTP_${e.code()}", parsed?.message ?: e.message())
        }
        is IOException -> throw ApiException(0, "NETWORK", e.message ?: "no internet")
        else -> throw e
    }
}

class ApiException(val code: Int, val errorCode: String, override val message: String) : RuntimeException(message)
