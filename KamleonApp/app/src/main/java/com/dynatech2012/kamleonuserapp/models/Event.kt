package com.dynatech2012.kamleonuserapp.models

import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer

/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
class Event<out T>(private val content: T? = null) {
    private val consumedScopes = HashSet<String>()

    fun isConsumed(scope: String = "") = consumedScopes.contains(scope)

    @MainThread
    fun consume(scope: String = ""): T? {
        return if (isConsumed(scope)) {
            null
        } else {
            consumedScopes.add(scope)
            content
        }
    }

    fun peek(): T? = content
}

fun <T> LiveData<Event<T>>.observeEvent(lifecycleOwner: LifecycleOwner, scope: String = "", observer: Observer<T>) {
    observe(lifecycleOwner) { event ->
        event?.consume(scope)?.let { observer.onChanged(it) }
    }
}