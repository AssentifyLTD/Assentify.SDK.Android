package   com.assentify.sdk.RemoteClient

import LanguageTransformationModel
import TransformationModel
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentRequestModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.DocumentTemplatesModel
import com.assentify.sdk.RemoteClient.Models.DocumentTokensModel
import com.assentify.sdk.RemoteClient.Models.SignatureRequestModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.Models.BaseResponseDataModel
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.Models.Templates
import  com.assentify.sdk.RemoteClient.Models.ValidateKeyModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*


interface RemoteAPIService {
    @GET("v1/Manager/Start/{interActionId}")
    @Headers("Content-Type: application/json")
    fun getStart(@Path("interActionId") interActionId: String): Call<ConfigModel>

}

interface RemoteIdPowerService {
    @GET("GetTemplates")
    @Headers(
        "Content-Type: application/json",
        "x-caller: sdk"
    )
    fun getTemplates(): Call<List<Templates>>;

}


interface RemoteSigningService {
    @GET("Document/DocumentTemplates/{tenantIdentifier}")
    @Headers("Content-Type: application/json")
    fun getTemplates(
        @Path("tenantIdentifier") tenantIdentifier: String,
        @Query("templateType") templateType: Int
    ): Call<List<DocumentTemplatesModel>>;

    @GET("Tokens/{templateId}")
    @Headers("Content-Type: application/json")
    fun getTokens(
        @Path("templateId") templateId: Int,
    ): Call<List<DocumentTokensModel>>;

    @POST("Document/v2/CreateUserDocumentInstance")
    @Headers("Content-Type: application/json")
    fun createUserDocumentInstance(
        @Body requestBody: CreateUserDocumentRequestModel
    ): Call<CreateUserDocumentResponseModel>;

    @POST("Signature")
    @Headers("Content-Type: application/json")
    fun signature(
        @Body requestBody: SignatureRequestModel
    ): Call<SignatureResponseModel>;

}


interface RemoteAuthenticationService {
    @POST("ValidateKey")
    @FormUrlEncoded
    fun validateKey(
        @Field("apiKey") apiKey: String,
        @Header("x-tenant-identifier") tenantIdentifier: String,
        @Header("X-Source-Agent") agentSource: String
    ): Call<ValidateKeyModel>;
}

interface RemoteGatewayService {
    @POST("v1/Manager/Submit")
    @Headers("Content-Type: application/json")
    fun submit(
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Body submitRequestModel: List<SubmitRequestModel>
    ): Call<ResponseBody>;


    @GET("v1/ContextAwareSigning/GetStep/{ID}")
    @Headers("Content-Type: application/json")
    fun getStep(
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Path("ID") ID: Int,
    ): Call<ContextAwareSigningModel>;
}


interface RemoteBlobStorageService {
    @Multipart
    @POST("/v1/Document/UploadBulk/{containerName}/{fileName}")
    fun uploadFile(
        @Path("containerName") containerName: String,
        @Path("fileName") fileName: String,
        @Part file: MultipartBody.Part,
        @Part("additionalValues") tenantIdentifier: RequestBody,
        @Part("additionalValues") blockIdentifier: RequestBody,
        @Part("additionalValues") instanceId: RequestBody,
        @Part("additionalValues") templateId: RequestBody,
        @Part("additionalValues") tryNumber: RequestBody,
    ): Call<ResponseBody>
}

interface RemoteWidgetsService {


    @POST
    @Multipart
    @Headers("Accept: application/json, text/plain, */*")
    fun starProcessing(
        @Url url: String,
        @Header("x-step-id") stepID: String,
        @Header("x-block-identifier") xBlockIdentifier: String,
        @Header("x-flow-identifier") xFlowIdentifier: String,
        @Header("x-flow-instance-id") xFlowInstanceId: String,
        @Header("x-instance-hash") xInstanceHash: String,
        @Header("x-instance-id") xInstanceId: String,
        @Header("x-tenant-identifier") xTenantIdentifier: String,
        @Part("tenantId") tenantId: RequestBody,
        @Part("blockId") blockId: RequestBody,
        @Part("instanceId") instanceId: RequestBody,
        @Part("templateId") templateId: RequestBody,
        @Part("livenessCheckEnabled") livenessCheckEnabled: RequestBody,
        @Part("isLivenessEnabled") isLivenessEnabled: RequestBody,
        @Part("processMrz") processMrz: RequestBody,
        @Part("DisableDataExtraction") disableDataExtraction: RequestBody,
        @Part("storeImageStream") storeImageStream: RequestBody,
        @Part("isVideo") isVideo: RequestBody,
        @Part("clipsPath") clipsPath: RequestBody,
        @Part("isMobile") isMobile: RequestBody,
        @Part("videoClipB64") videoClip: RequestBody,
        @Part("checkForFace") checkForFace: RequestBody,
        @Part("callerConnectionId") callerConnectionId: RequestBody,
        @Part("SecondImage") secondImage: RequestBody,
        @Part("saveCapturedVideo") saveCapturedVideo: RequestBody,
        @Part("storeCapturedDocument") storeCapturedDocument: RequestBody,
        @Part("traceIdentifier") traceIdentifier: RequestBody,
        @Part("selfieImage") selfieImage: RequestBody,
    ): Call<ResponseBody>
}

interface RemoteTranslatedService {
    @Headers(
        "accept: application/json, text/plain, */*",
        "Content-Type: application/json"
    )
    @POST("LanguageTransform/LanguageTransformation")
    fun transformData(
        @Header("'x-api-key") apiKey: String,
        @Header("accept-language") language: String,
        @Body request: TransformationModel
    ): Call<List<LanguageTransformationModel>>
}

