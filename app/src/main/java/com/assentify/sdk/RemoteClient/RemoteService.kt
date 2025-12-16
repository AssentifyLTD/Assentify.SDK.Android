package   com.assentify.sdk.RemoteClient

import com.assentify.sdk.AssistedDataEntry.Models.AssistedDataEntryBaseModel
import com.assentify.sdk.Flow.Models.DataSourceRequestBody
import com.assentify.sdk.Flow.Models.DataSourceResponse
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentRequestModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.DocumentTemplatesModel
import com.assentify.sdk.RemoteClient.Models.DocumentTokensModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpModel
import com.assentify.sdk.RemoteClient.Models.RequestOtpResponseModel
import com.assentify.sdk.RemoteClient.Models.SignatureRequestModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TermsConditionsModel
import com.assentify.sdk.RemoteClient.Models.TokensMappings
import com.assentify.sdk.RemoteClient.Models.ValidateKeyModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpRequestOtpModel
import com.assentify.sdk.RemoteClient.Models.VerifyOtpResponseOtpModel
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url


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

    @GET("Tokens/sdk/gettokens/{templateId}")
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

    @GET("Mappings/{blockIdentifier}/{stepId}/{templateId}")
    @Headers("Content-Type: application/json")
    fun mappings(
        @Header("x-tenant-identifier") tenantIdentifier: String,
        @Path("blockIdentifier") blockIdentifier: String,
        @Path("stepId") stepId: Int,
        @Path("templateId") templateId: Int,
    ): Call<List<TokensMappings>>;

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
    fun getContextAwareSigningStep(
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


    @GET("v1/AssistedDataEntry/GetStep/{ID}")
    @Headers("Content-Type: application/json")
    fun getAssistedDataEntryStep(
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Path("ID") ID: String,
    ): Call<AssistedDataEntryBaseModel>;

    @POST("v1/OtpVerification/RequestOtp")
    @Headers("Content-Type: application/json")
    fun requestOtp(
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Body requestOtpModel: RequestOtpModel
    ): Call<RequestOtpResponseModel>;

    @POST("v1/OtpVerification/VerifyOtp")
    @Headers("Content-Type: application/json")
    fun verifyOtp(
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Body verifyOtpRequestOtpModel: VerifyOtpRequestOtpModel
    ): Call<VerifyOtpResponseOtpModel>;


    @GET("v1/TermsConditions/GetStep/{ID}")
    @Headers("Content-Type: application/json")
    fun getTermsConditionsStep(
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Path("ID") ID: String,
    ): Call<TermsConditionsModel>;

    @POST("v1/DataSource/DataSourceValues")
    @Headers("Content-Type: application/json")
    fun getDataSourceValues(
        @Header("X-Api-Key") apiKey: String,
        @Header("X-Source-Agent") userAgent: String,
        @Header("X-Flow-Instance-Id") flowInstanceId: String,
        @Header("X-Tenant-Identifier") tenantIdentifier: String,
        @Header("X-Block-Identifier") blockIdentifier: String,
        @Header("X-Instance-Id") instanceId: String,
        @Header("X-Flow-Identifier") flowIdentifier: String,
        @Header("X-Instance-Hash") instanceHash: String,
        @Query("elementIdentifier") elementIdentifier: String,
        @Query("stepId") stepId: Int,
        @Query("endpointId") endpointId: Int,
        @Body body: DataSourceRequestBody
    ): Call<DataSourceResponse>;
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


    @Multipart
    @POST("v2/Document/UploadFile/userfiles/{path}?skipValidator=true")
    fun uploadImageFile(
        @Header("X-Api-Key") apiKey: String,
        @Header("x-tenant-identifier") tenantId: String,
        @Header("x-block-identifier") blockId: String,
        @Header("x-instance-id") instanceId: String,
        @Header("accept") accept: String = "text/plain",
        @Path(value = "path", encoded = true) filePath: String,
        @Part asset: MultipartBody.Part,
    ): Call<ResponseBody>
}

interface RemoteWidgetsService {


    @POST
    @Multipart
    @JvmSuppressWildcards
    @Headers("Accept: application/json, text/plain, */*")
    fun starQrProcessing(
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
        @Part("isMobile") isMobile: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("callerConnectionId") callerConnectionId: RequestBody,
        @Part("Metadata") metadata: RequestBody,
        @Part("traceIdentifier") traceIdentifier: RequestBody,
        @Part("IsManualCapture") isManualCapture : RequestBody,
        @Part("IsAutoCapture") isAutoCapture: RequestBody,
    ): Call<ResponseBody>


    @POST
    @Multipart
    @JvmSuppressWildcards
    @Headers("Accept: application/json, text/plain, */*")
    fun starProcessingIDs(
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
        @Part("templateId") templateIds: List<RequestBody>,
        @Part("LivenessCheckEnabled") livenessCheckEnabled: RequestBody,
        @Part("processMrz") processMrz: RequestBody,
        @Part("DisableDataExtraction") disableDataExtraction: RequestBody,
        @Part("storeImageStream") storeImageStream: RequestBody,
        @Part("isVideo") isVideo: RequestBody,
        @Part("clipsPath") clipsPath: RequestBody,
        @Part("isMobile") isMobile: RequestBody,
        @Part image: MultipartBody.Part,
        @Part("checkForFace") checkForFace: RequestBody,
        @Part("callerConnectionId") callerConnectionId: RequestBody,
        @Part("saveCapturedVideo") saveCapturedVideo: RequestBody,
        @Part("storeCapturedDocument") storeCapturedDocument: RequestBody,
        @Part("traceIdentifier") traceIdentifier: RequestBody,
        @Part("IsManualCapture") isManualCapture : RequestBody,
        @Part("IsAutoCapture") isAutoCapture: RequestBody,
        @Part("TryNumber") tryNumber: RequestBody,
        @Part("Tag") tag  : RequestBody,
        @Part("ProcessCivilExtractQrCode") processCivilExtractQrCode  : RequestBody,
        @Part("RequireFaceExtraction") requireFaceExtraction  : RequestBody,
    ): Call<ResponseBody>

    @POST
    @Multipart
    @JvmSuppressWildcards
    @Headers("Accept: application/json, text/plain, */*")
    fun starProcessingFace(
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
        @Part selfieImage: MultipartBody.Part,
        @Part livenessFrames:  List<MultipartBody.Part>,
        @Part("traceIdentifier") traceIdentifier: RequestBody,
        @Part("isMobile") isMobile: RequestBody,
        @Part secondImage: MultipartBody.Part,
        @Part("IsLivenessEnabled") livenessCheckEnabled: RequestBody,
        @Part("TryNumber") tryNumber: RequestBody,
        @Part("IsAutoCapture") isAutoCapture: RequestBody,
        @Part("IsManualCapture") isManualCapture : RequestBody,
        @Part("callerConnectionId") callerConnectionId: RequestBody,
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

