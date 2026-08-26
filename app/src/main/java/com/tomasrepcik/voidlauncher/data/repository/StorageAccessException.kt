package com.tomasrepcik.voidlauncher.data.repository

import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

internal class StorageAccessException(cause: Throwable) : RuntimeException(cause)

internal suspend inline fun <T> storageCall(crossinline block: suspend () -> T): T = flow {
    emit(block())
}.catch { cause ->
    throw cause.asStorageAccessException()
}.first()

internal fun Throwable.asStorageAccessException(): StorageAccessException =
    this as? StorageAccessException ?: StorageAccessException(this)
