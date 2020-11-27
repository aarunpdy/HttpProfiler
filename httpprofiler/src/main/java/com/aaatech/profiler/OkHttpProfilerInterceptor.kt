package com.aaatech.profiler

import com.aaatech.profiler.transfer.DataTransfer
import com.aaatech.profiler.transfer.LogDataTransfer
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

import okhttp3.*;
import org.jetbrains.annotations.NotNull

/**
 * @author Arun A
 */
class OkHttpProfilerInterceptor : Interceptor {
    private val dataTransfer: DataTransfer = LogDataTransfer()
    private val format: DateFormat = SimpleDateFormat("ddhhmmssSSS", Locale.US)
    private val previousTime: AtomicLong = AtomicLong()

    @NotNull
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val id = generateId()
        val startTime = System.currentTimeMillis()
        dataTransfer.sendRequest(id, chain.request())
        return try {
            val response: Response = chain.proceed(chain.request())
            dataTransfer.sendResponse(id, response)
            dataTransfer.sendDuration(id, System.currentTimeMillis() - startTime)
            response
        } catch (e: Exception) {
            dataTransfer.sendException(id, e)
            dataTransfer.sendDuration(id, System.currentTimeMillis() - startTime)
            throw e
        }
    }

    /**
     * Generates unique string id via a day and time
     * Based on a current time.
     * @return string id
     */
    @Synchronized
    private fun generateId(): String {
        var currentTime: Long = format.format(Date()).toLong()
        //Increase time if it the same, as previous (unique id)
        var previousTime: Long = previousTime.get()
        if (currentTime <= previousTime) {
            currentTime = ++previousTime
        }
        this.previousTime.set(currentTime)
        return currentTime.toString(Character.MAX_RADIX)
    }
}
