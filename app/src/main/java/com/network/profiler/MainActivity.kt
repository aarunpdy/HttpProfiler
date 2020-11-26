package com.network.profiler

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class MainActivity : AppCompatActivity() {

    //    private static final String JSON_URL = "https://raw.githubusercontent.com/itkacher/OkHttpProfiler/master/large_random_json.json";
    private val JSON_URL =
        "https://raw.githubusercontent.com/itkacher/OkHttpProfiler/master/colors.json"
    private var mClient: OkHttpClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                @Throws(CertificateException::class)
                override fun checkClientTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }

                @Throws(CertificateException::class)
                override fun checkServerTrusted(
                    chain: Array<X509Certificate>,
                    authType: String
                ) {
                }

                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return emptyArray()
                }
            }
        )

        // Install the all-trusting trust manager

        // Install the all-trusting trust manager
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())
        // Create an ssl socket factory with our all-trusting manager
        // Create an ssl socket factory with our all-trusting manager
        val sslSocketFactory = sslContext.socketFactory

        val builder = OkHttpClient.Builder()
        builder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        builder.hostnameVerifier { hostname, session -> true }
        if (BuildConfig.DEBUG) {
            builder.addInterceptor(OkHttpProfilerInterceptor())
        }
        mClient = builder.build()
        sendRequest()
        findViewById<View>(R.id.send_request).setOnClickListener { v: View? -> sendRequest() }
    }

    private fun sendRequest() {
        Thread {
            val request: Request = Request.Builder()
                .url(JSON_URL)
                .get()
                .build()
            val response = mClient?.newCall(request)?.execute()
            response?.toString()?.let { Log.d("TAG", it) }
        }.start()

        /*mClient!!.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d("TAG", call.toString())
                Log.d("TAG", e.toString())
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    if (response.body != null) {
                        val unusedText: String = response.body!!.string()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        })*/
    }
}
