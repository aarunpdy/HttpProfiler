package com.aaatech.profiler.transfer

import android.annotation.SuppressLint
import android.os.*
import android.util.Log
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import java.io.IOException
import java.nio.charset.Charset


/**
 * @author Arun A
 */
class LogDataTransfer : DataTransfer {
    companion object {
        private const val LOG_LENGTH = 4000
        private const val SLOW_DOWN_PARTS_AFTER = 20
        private const val BODY_BUFFER_SIZE = 1024 * 1024 * 10
        private const val LOG_PREFIX = "OKPRFL"
        private const val DELIMITER = "_"
        private const val HEADER_DELIMITER = ':'
        private const val SPACE = ' '
        private const val KEY_TAG = "TAG"
        private const val KEY_VALUE = "VALUE"
        private const val KEY_PARTS_COUNT = "PARTS_COUNT"
        private const val CONTENT_TYPE = "Content-Type"
        private const val CONTENT_LENGTH = "Content-Length"
    }
    private var mHandler: Handler? = null

    init {
        val handlerThread = HandlerThread("OkHttpProfiler", Process.THREAD_PRIORITY_BACKGROUND)
        handlerThread.start()
        mHandler = LogBodyHandler(handlerThread.looper)
    }

    @Throws(IOException::class)
    override fun sendRequest(id: String, request: Request) {
        fastLog(id, MessageType.REQUEST_METHOD, request.method)
        val url = request.url.toString()
        fastLog(id, MessageType.REQUEST_URL, url)
        fastLog(id, MessageType.REQUEST_TIME, System.currentTimeMillis().toString())
        val copy = request.newBuilder().build()
        val buffer = Buffer()
        val body = copy.body
        if (body != null) {
            val type = body.contentType()
            if (type != null) {
                fastLog(
                    id,
                    MessageType.REQUEST_HEADER,
                    CONTENT_TYPE + HEADER_DELIMITER + SPACE + type.toString()
                )
            }
            val contentLength = body.contentLength()
            if (contentLength != -1L) {
                fastLog(
                    id,
                    MessageType.REQUEST_HEADER,
                    CONTENT_LENGTH + HEADER_DELIMITER + SPACE + contentLength
                )
            }
        }
        val headers = request.headers
        for (name in headers.names()) {
            //We have logged them before
            if (CONTENT_TYPE.equals(name, ignoreCase = true) || CONTENT_LENGTH.equals(
                    name,
                    ignoreCase = true
                )
            ) {
                continue
            }
            fastLog(id, MessageType.REQUEST_HEADER, name + HEADER_DELIMITER + SPACE + headers[name])
        }
        if (body != null) {
            body.writeTo(buffer)
            largeLog(id, MessageType.REQUEST_BODY, buffer.readString(Charset.defaultCharset()))
        }
    }

    @Throws(IOException::class)
   override fun sendResponse(id: String, response: Response) {
        val responseBodyCopy = response.peekBody(BODY_BUFFER_SIZE.toLong())
        largeLog(id, MessageType.RESPONSE_BODY, responseBodyCopy.string())
        val headers = response.headers
        logWithHandler(id, MessageType.RESPONSE_STATUS, response.code.toString(), 0)
        for (name in headers.names()) {
            logWithHandler(
                id,
                MessageType.RESPONSE_HEADER,
                name + HEADER_DELIMITER + headers[name],
                0
            )
        }
    }

    override fun sendException(id: String, response: Exception) {
        logWithHandler(id, MessageType.RESPONSE_ERROR, response.localizedMessage ?: "", 0)
    }

    override fun sendDuration(id: String, duration: Long) {
        logWithHandler(id, MessageType.RESPONSE_TIME, duration.toString(), 0)
        logWithHandler(id, MessageType.RESPONSE_END, "-->", 0)
    }

    @SuppressLint("LogNotTimber")
    private fun fastLog(id: String, type: MessageType, message: String?) {
        val tag = LOG_PREFIX + DELIMITER + id + DELIMITER + type.api
        if (message != null) {
            Log.v(tag, message)
        }
    }

    private fun logWithHandler(id: String, type: MessageType, message: String, partsCount: Int) {
        val handlerMessage = mHandler!!.obtainMessage()
        val tag = LOG_PREFIX + DELIMITER + id + DELIMITER + type.api
        val bundle = Bundle()
        bundle.putString(KEY_TAG, tag)
        bundle.putString(KEY_VALUE, message)
        bundle.putInt(KEY_PARTS_COUNT, partsCount)
        handlerMessage.data = bundle
        mHandler!!.sendMessage(handlerMessage)
    }

    private fun largeLog(id: String, type: MessageType, content: String) {
        val contentLength = content.length
        if (content.length > LOG_LENGTH) {
            val parts = contentLength / LOG_LENGTH
            for (i in 0..parts) {
                val start = i * LOG_LENGTH
                var end = start + LOG_LENGTH
                if (end > contentLength) {
                    end = contentLength
                }
                logWithHandler(id, type, content.substring(start, end), parts)
            }
        } else {
            logWithHandler(id, type, content, 0)
        }
    }


    private class LogBodyHandler(looper: Looper) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            val bundle = msg.data
            if (bundle != null) {
                val partsCount = bundle.getInt(KEY_PARTS_COUNT, 0)
                if (partsCount > SLOW_DOWN_PARTS_AFTER) {
                    try {
                        Thread.sleep(5L)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
                val data = bundle.getString(KEY_VALUE)
                val key = bundle.getString(KEY_TAG)
                if (data != null && key != null) {
                    Log.v(key, data)
                }
            }
        }
    }
}
