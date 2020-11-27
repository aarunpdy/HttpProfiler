package com.aaatech.profiler.transfer

import java.io.IOException
import okhttp3.Request
import okhttp3.Response

/**
 * @author Arun A
 */
interface DataTransfer {
    @Throws(IOException::class)
    fun sendRequest(id: String, request: Request)

    @Throws(IOException::class)
    fun sendResponse(id: String, response: Response)

    fun sendException(id: String, response: Exception)

    fun sendDuration(id: String, duration: Long)
}
