package   com.assentify.sdk.RemoteClient

import  com.assentify.sdk.Core.Constants.Routes.BaseUrls
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RemoteClient {
    private const val BASE_URL_ID_POWER = "https://idpower.assentify.com/api/IDPower/"
    private const val BASE_URL_SIGNING = "https://signme.assentify.com/api/"
    private const val BASE_URL_API = "https://api.gateway.assentify.com/webapi/"
    private const val BASE_URL_AUTHENTICATION =
        "https://api.admin.assentify.com/api/Authentication/"
    private const val BASE_URL_GATEWAY = "https://api.gateway.assentify.com/webapi/"
    private const val BLOB_STORAGE_URL = "https://blob.assentify.com"
    val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val newRequest = originalRequest.newBuilder()
                .build()
            chain.proceed(newRequest)
        }
        .build()


    val remoteApiService: RemoteAPIService = Retrofit.Builder()
        .baseUrl(BASE_URL_API)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteAPIService::class.java)

    val remoteIdPowerService: RemoteIdPowerService = Retrofit.Builder()
        .baseUrl(BASE_URL_ID_POWER)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteIdPowerService::class.java)

    val remoteSigningService: RemoteSigningService = Retrofit.Builder()
        .baseUrl(BASE_URL_SIGNING)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteSigningService::class.java)

    val remoteAuthenticationService: RemoteAuthenticationService = Retrofit.Builder()
        .baseUrl(BASE_URL_AUTHENTICATION)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteAuthenticationService::class.java)

    val remoteGatewayService: RemoteGatewayService = Retrofit.Builder()
        .baseUrl(BASE_URL_GATEWAY)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteGatewayService::class.java)

    val remoteBlobStorageService: RemoteBlobStorageService = Retrofit.Builder()
        .baseUrl(BLOB_STORAGE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteBlobStorageService::class.java)

    val remoteWidgetsService: RemoteWidgetsService = Retrofit.Builder()
        .baseUrl(BaseUrls.SignalRHub)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()
        .create(RemoteWidgetsService::class.java)
}
