package com.dynatech2012.kamleonuserapp.repositories

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Response<out T : Any> {

    data class Success<out T : Any>(val data: T) : Response<T>()
    data class Failure(val exception: Exception) : Response<Nothing>()
    data class Loading(val loading: Boolean = true) : Response<Nothing>()
    data class Inactive(val inactive: Boolean = true) : Response<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$exception]"
            is Loading -> "Loading"
            is Inactive -> "Inactive"
        }
    }

    val isSuccess: Boolean
        get() = this is Success
    val isLoading: Boolean
        get() = this is Loading

    val dataValue: T?
        get() = if (this is Success) this.data else null
    val error: String?
        get() = if (this is Failure) this.exception.message else null
}

sealed class ResponseNullable<out T : Any> {

    data class Success<out T : Any>(val data: T? = null) : ResponseNullable<T>()
    data class Failure(val exception: Exception) : ResponseNullable<Nothing>()
    data class Loading(val loading: Boolean = true) : ResponseNullable<Nothing>()
    data class Inactive(val inactive: Boolean = true) : ResponseNullable<Nothing>()

    override fun toString(): String {
        return when (this) {
            is Success<*> -> "Success[data=$data]"
            is Failure -> "Error[exception=$exception]"
            is Loading -> "Loading"
            is Inactive -> "Inactive"
        }
    }

    val isSuccess: Boolean
        get() = this is Success
    val isLoading: Boolean
        get() = this is Loading

    val dataValue: T?
        get() = if (this is Success) this.data else null
    val error: String?
        get() = if (this is Failure) this.exception.message else null
}