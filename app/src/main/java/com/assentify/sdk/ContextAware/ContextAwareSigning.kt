package com.assentify.sdk.CheckEnvironment


import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentRequestModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.SignatureRequestModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
import com.assentify.sdk.RemoteClient.Models.TokensMappings
import com.assentify.sdk.RemoteClient.RemoteClient.remoteGatewayService
import com.assentify.sdk.RemoteClient.RemoteClient.remoteSigningService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ContextAwareSigning(
    private val contextAwareSigningCallback: ContextAwareSigningCallback,
    private val tenantIdentifier: String,
    private val interaction: String,
    private val stepID: Int,
    private val configModel: ConfigModel,
    private val apiKey: String
) {


    private var templateId = -1;
    private var contextAwareSigningModel :ContextAwareSigningModel? = null;

    init {
        getContextAwareSigningStep()
    }


    private fun getContextAwareSigningStep() {
        val remoteService = remoteGatewayService
        val call = remoteService.getContextAwareSigningStep(
            apiKey, "SDK",
            configModel.flowInstanceId,
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            configModel.instanceId,
            configModel.flowIdentifier,
            configModel.instanceHash,
            stepID
        )
        call.enqueue(object : Callback<ContextAwareSigningModel> {
            override fun onResponse(
                call: Call<ContextAwareSigningModel>,
                response: Response<ContextAwareSigningModel>
            ) {
                if (response.isSuccessful) {
                    contextAwareSigningModel = response.body();
                    templateId = response.body()!!.data.selectedTemplates[0]
                    getTokensMappings(response.body()!!.data.selectedTemplates[0])
                }
            }

            override fun onFailure(call: Call<ContextAwareSigningModel>, t: Throwable) {
                contextAwareSigningCallback.onError(t.message!!)
            }
        })
    }




    private  fun getTokensMappings(documentId: Int) {
        val remoteService = remoteSigningService
        val call = remoteService.mappings(
            configModel.tenantIdentifier,
            configModel.blockIdentifier,
            stepID,
            documentId
        )
        call.enqueue(object : Callback<List<TokensMappings>> {
            override fun onResponse(
                call: Call<List<TokensMappings>>,
                response: Response<List<TokensMappings>>
            ) {
                if (response.isSuccessful) {
                    contextAwareSigningCallback.onHasTokens(response.body()!!,contextAwareSigningModel);
                }
            }

            override fun onFailure(call: Call<List<TokensMappings>>, t: Throwable) {
                contextAwareSigningCallback.onError(t.message!!)
            }
        })
    }


    fun createUserDocumentInstance(data: Map<String, String>) {
        val remoteService = remoteSigningService
        val createUserDocumentRequestModel =
            CreateUserDocumentRequestModel(
                userId = "UserId",
                documentTemplateId = templateId,
                data = data,
                outputType = 1,
            );
        val call = remoteService.createUserDocumentInstance(createUserDocumentRequestModel)
        call.enqueue(object : Callback<CreateUserDocumentResponseModel> {
            override fun onResponse(
                call: Call<CreateUserDocumentResponseModel>,
                response: Response<CreateUserDocumentResponseModel>
            ) {

                if (response.isSuccessful) {
                    contextAwareSigningCallback.onCreateUserDocumentInstance(
                        response.body()!!
                    );
                }
            }

            override fun onFailure(call: Call<CreateUserDocumentResponseModel>, t: Throwable) {
                contextAwareSigningCallback.onError(t.message!!)
            }
        })
    }

    fun signature(
        documentInstanceId: Int,
        documentId: Int,
        signature: String,
    ) {
        val signatureRequestModel = SignatureRequestModel(
            documentId = documentId,
            documentInstanceId = documentInstanceId,
            documentName = "documentName",
            username = "UserId",
            requiresAdditionalData = false,
            signature = signature,
        )
        val remoteService = remoteSigningService
        val call = remoteService.signature(signatureRequestModel)
        call.enqueue(object : Callback<SignatureResponseModel> {
            override fun onResponse(
                call: Call<SignatureResponseModel>,
                response: Response<SignatureResponseModel>
            ) {
                if (response.isSuccessful) {
                    contextAwareSigningCallback.onSignature(response.body()!!);
                }
            }

            override fun onFailure(call: Call<SignatureResponseModel>, t: Throwable) {
                contextAwareSigningCallback.onError(t.message!!)
            }
        })
    }
}
