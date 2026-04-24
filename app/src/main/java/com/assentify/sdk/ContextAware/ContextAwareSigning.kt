package com.assentify.sdk.CheckEnvironment


import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.ContextAwareSigningModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentRequestModel
import com.assentify.sdk.RemoteClient.Models.CreateUserDocumentResponseModel
import com.assentify.sdk.RemoteClient.Models.Customization
import com.assentify.sdk.RemoteClient.Models.DataModel
import com.assentify.sdk.RemoteClient.Models.SignatureRequestModel
import com.assentify.sdk.RemoteClient.Models.SignatureResponseModel
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


    private var contextAwareSigningModel :ContextAwareSigningModel? = null;

    init {
        getContextAwareSigningStepFromConfigFile()
    }



    private fun getContextAwareSigningStepFromConfigFile() {
        val stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
            if (it.stepId == this.stepID) {
                contextAwareSigningModel = it.customization.toContextAwareSigningModel()
                contextAwareSigningModel!!.data.selectedTemplates.forEach {it->
                    getTokensMappings(it)
                }
            }
        }
    }

    fun Customization.toContextAwareSigningModel(): ContextAwareSigningModel {
        return ContextAwareSigningModel(
                statusCode = 200,
                data = DataModel(
                    header = this.header,
                    subHeader = this.subHeader,
                    selectedTemplates = this.selectedTemplates ?: emptyList(),
                    confirmationMessage = this.confirmationMessage,
                    autoDownload = this.autoDownload ?: false,
                    enableDigitalSignature = !(this.hideSignatureBoard ?: false), // Kotlin logic like you used before
                    hideSignatureBoard = this.hideSignatureBoard ?: false,
                    otpInputType = this.otpInputType ?: "TEXT",
                    enableOtp = this.enableOtp ?: false,
                    otpSize = this.otpSize,
                    otpType = this.otpType,
                    otpExpiryTime = this.otpExpiryTime
                )
        )
    }




    private  fun getTokensMappings(templateId: Int) {
        val stepDefinitions = configModel.stepDefinitions
        stepDefinitions.forEach {
            if (it.stepId == this.stepID) {
                val mappings = it.mappings
                contextAwareSigningCallback.onHasTokens(templateId,mappings!!,contextAwareSigningModel);
            }
        }

    }


    fun createUserDocumentInstance(templateId:Int,data: Map<String, String>) {
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
        val call = remoteService.signature(signatureRequestModel,configModel.tenantIdentifier)
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
