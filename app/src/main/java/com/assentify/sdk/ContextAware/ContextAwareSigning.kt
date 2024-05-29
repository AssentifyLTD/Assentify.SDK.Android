package com.assentify.sdk.CheckEnvironment

import ContextAwareSigningModel
import CreateUserDocumentRequestModel
import CreateUserDocumentResponseModel
import DocumentTokensModel
import SignatureRequestModel
import SignatureResponseModel
import  com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import  com.assentify.sdk.RemoteClient.Models.ConfigModel
import  com.assentify.sdk.RemoteClient.RemoteClient.remoteGatewayService
import  com.assentify.sdk.RemoteClient.RemoteClient.remoteSigningService
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

    init {
        getStep()
    }


    private fun getStep() {
        val remoteService = remoteGatewayService
        val call = remoteService.getStep(
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
                    templateId = response.body()!!.data.selectedTemplates[0]
                    getTokens(response.body()!!.data.selectedTemplates[0])
                }
            }

            override fun onFailure(call: Call<ContextAwareSigningModel>, t: Throwable) {
                contextAwareSigningCallback.onError(t.message!!)
            }
        })
    }


   private  fun getTokens(documentId: Int) {
        val remoteService = remoteSigningService
        val call = remoteService.getTokens(documentId)
        call.enqueue(object : Callback<List<DocumentTokensModel>> {
            override fun onResponse(
                call: Call<List<DocumentTokensModel>>,
                response: Response<List<DocumentTokensModel>>
            ) {
                if (response.isSuccessful) {
                    contextAwareSigningCallback.onHasTokens(response.body()!!);
                }
            }

            override fun onFailure(call: Call<List<DocumentTokensModel>>, t: Throwable) {
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
        signature: String,
    ) {
        val signatureRequestModel = SignatureRequestModel(
            documentId = templateId,
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
