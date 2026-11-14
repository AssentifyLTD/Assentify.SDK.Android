package com.assentify.sdk.RemoteClient

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RemoteClient {

    // Defaults
    private const val DEFAULT_WIDGETS_SOCKET_URL = "https://widgets.socket.assentify.com/"
    private const val DEFAULT_BASE_URL_SIGNING = "https://signme.assentify.com/api/"
    private const val DEFAULT_BASE_URL_GATEWAY = "https://api.gateway.assentify.com/webapi/"
    private const val DEFAULT_BLOB_STORAGE_URL = "https://blob.assentify.com/"
    private const val DEFAULT_LANGUAGE_TRANSFORM_URL = "https://widgets.socket.assentify.com/api/"

    @Volatile var WIDGETS_SOCKET_URL = DEFAULT_WIDGETS_SOCKET_URL
        private set
    @Volatile var BASE_URL_SIGNING = DEFAULT_BASE_URL_SIGNING
        private set
    @Volatile var BASE_URL_GATEWAY = DEFAULT_BASE_URL_GATEWAY
        private set
    @Volatile var BLOB_STORAGE_URL = DEFAULT_BLOB_STORAGE_URL
        private set
    @Volatile var LANGUAGE_TRANSFORM_URL = DEFAULT_LANGUAGE_TRANSFORM_URL
        private set

    /**
     * Override any URL. null or "" = keep the default.
     * Call this before using any service.
     */
    @Synchronized
    fun configure(
        widgetsSocketUrl: String? = null,
        baseUrlSigning: String? = null,
        baseUrlGateway: String? = null,
        blobStorageUrl: String? = null,
        languageTransformUrl: String? = null,
    ) {
        WIDGETS_SOCKET_URL = widgetsSocketUrl!!
        BASE_URL_SIGNING = baseUrlSigning!!
        BASE_URL_GATEWAY = baseUrlGateway!!
        BLOB_STORAGE_URL = blobStorageUrl!!
        LANGUAGE_TRANSFORM_URL = languageTransformUrl!!
        serviceCache.clear() // services get rebuilt with the new URLs on next use
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
            )
            .build()
    }

    // Each service is built on first use and cached until configure() is called again
    private val serviceCache = HashMap<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    private inline fun <reified T : Any> service(baseUrl: String): T =
        synchronized(this) {
            serviceCache.getOrPut(T::class.java) {
                Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .build()
                    .create(T::class.java)
            } as T
        }

    // Same names as before, so existing code keeps working
    val remoteSigningService: RemoteSigningService
        get() = service(BASE_URL_SIGNING)

    val remoteGatewayService: RemoteGatewayService
        get() = service(BASE_URL_GATEWAY)

    val remoteBlobStorageService: RemoteBlobStorageService
        get() = service(BLOB_STORAGE_URL)

    val remoteWidgetsService: RemoteWidgetsService
        get() = service(WIDGETS_SOCKET_URL)

    val remoteLanguageTransform: RemoteTranslatedService
        get() = service(LANGUAGE_TRANSFORM_URL)
}