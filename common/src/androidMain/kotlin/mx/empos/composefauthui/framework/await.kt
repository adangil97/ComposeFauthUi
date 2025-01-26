package mx.empos.composefauthui.framework

import com.google.android.gms.tasks.Task
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun <T> Task<T>.await(runException: (Exception) -> Unit = {}): T? =
    suspendCoroutine { continuation ->
        addOnCompleteListener {
            if (it.isSuccessful) {
                continuation.resume(it.result)
            } else {
                runException(it.exception ?: Exception("Some was wrong"))
                continuation.resume(null)
            }
        }
    }