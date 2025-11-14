package  com.assentify.sdk

import android.content.Context
import android.content.Intent
import android.util.Log
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntry
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntryCallback
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.FileUtils.ImageUtils
import com.assentify.sdk.Core.FileUtils.ReadJSONFromAsset
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceMatchManual
import com.assentify.sdk.FaceMatch.FaceMatchResult
import com.assentify.sdk.Flow.BlockLoader.BlockLoaderStepsComposeActivity
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.LanguageTransformation.LanguageTransformation
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.RemoteClient.Models.ValidateKeyModel
import com.assentify.sdk.RemoteClient.RemoteClient
import com.assentify.sdk.RemoteClient.RemoteClient.remoteAuthenticationService
import com.assentify.sdk.ScanIDCard.IDCardCallback
import com.assentify.sdk.ScanIDCard.ScanIDCard
import com.assentify.sdk.ScanIDCard.ScanIDCardManual
import com.assentify.sdk.ScanIDCard.ScanIDCardResult
import com.assentify.sdk.ScanNFC.ScanNfc
import com.assentify.sdk.ScanNFC.ScanNfcCallback
import com.assentify.sdk.ScanOther.ScanOther
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanOther.ScanOtherManual
import com.assentify.sdk.ScanOther.ScanOtherResult
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.ScanPassport.ScanPassportManual
import com.assentify.sdk.ScanPassport.ScanPassportResult
import com.assentify.sdk.ScanQr.ScanQr
import com.assentify.sdk.ScanQr.ScanQrCallback
import com.assentify.sdk.ScanQr.ScanQrManual
import com.assentify.sdk.ScanQr.ScanQrResult
import com.assentify.sdk.SubmitData.SubmitData
import com.assentify.sdk.SubmitData.SubmitDataCallback
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssentifySdk(
    private var apiKey: String? = null,
    private var tenantIdentifier: String? = null,
    private var interaction: String? = null,
    private val environmentalConditions: EnvironmentalConditions,
    private val assentifySdkCallback: AssentifySdkCallback,
    private var performActiveLivenessFace: Boolean? = null,
    private var context: Context,
) {

    private var isKeyValid: Boolean = false;
    private var configModel: ConfigModel? = null;
    private var allTemplates: List<TemplatesByCountry> = emptyList();
    private lateinit var readJSONFromAsset: ReadJSONFromAsset;


    init {
        if (configModel == null) {
            if (apiKey.isNullOrEmpty()) {
                Log.e("AssentifySdk Init Error ", "ApiKey must not be empty or null")
            }
            if (interaction.isNullOrEmpty()) {
                Log.e("AssentifySdk Init Error ", "Interaction must not be empty or null")
            }
            if (tenantIdentifier.isNullOrEmpty()) {
                Log.e("AssentifySdk Init Error ", "TenantIdentifier must not be empty or null")
            }
            if (!apiKey.isNullOrEmpty() && !interaction.isNullOrEmpty() && !tenantIdentifier.isNullOrEmpty()) {
                validateKey()
            }
        }

    }

    private fun validateKey() {
        val remoteService = remoteAuthenticationService
        val call = remoteService.validateKey(apiKey!!, tenantIdentifier!!, "SDK")
        call.enqueue(object : Callback<ValidateKeyModel> {
            override fun onResponse(
                call: Call<ValidateKeyModel>,
                response: Response<ValidateKeyModel>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null && response.body()!!.statusCode == 200 && response.body()!!.isSuccessful) {
                        isKeyValid = true;
                        getStart()
                    }
                } else {
                    isKeyValid = false;
                    assentifySdkCallback.onAssentifySdkInitError("Invalid Keys");
                }
            }

            override fun onFailure(call: Call<ValidateKeyModel>, t: Throwable) {
                isKeyValid = false;
                assentifySdkCallback.onAssentifySdkInitError("Invalid Keys");
            }
        })
    }

    private fun getStart() {
        val remoteService = RemoteClient.remoteApiService
        val call = remoteService.getStart(interaction!!)
        call.enqueue(object : Callback<ConfigModel> {
            override fun onResponse(
                call: Call<ConfigModel>,
                response: Response<ConfigModel>
            ) {
                if (response.isSuccessful) {
                    configModel = response.body()!!
                    iniSdk();
                }

            }

            override fun onFailure(call: Call<ConfigModel>, t: Throwable) {
                assentifySdkCallback.onAssentifySdkInitError(t.message!!);
            }
        })
    }


    private fun iniSdk() {
        getTemplatesByCountry();
    }

    fun startScanPassport(
        scanPassportCallback: ScanPassportCallback,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanPassportResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context,this.environmentalConditions)) {
                val scanPassportManual = ScanPassportManual(
                    configModel,
                    environmentalConditions,
                    apiKey,
                    language
                )
                scanPassportManual.setStepId(stepId?.toString())
                scanPassportManual.setScanPassportCallback(scanPassportCallback)
                return ScanPassportResult.Manual(scanPassportManual)
            } else {
                val scanPassport = ScanPassport(
                    configModel,
                    environmentalConditions,
                    apiKey,
                    language
                )
                scanPassport.setStepId(stepId?.toString())
                scanPassport.setScanPassportCallback(scanPassportCallback)
                return ScanPassportResult.Auto(scanPassport)
            }


        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanIDCard(
        scnIDCardCallback: IDCardCallback,
        kycDocumentDetails: List<KycDocumentDetails>,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanIDCardResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context,this.environmentalConditions)) {
                val scanIDCardManual = ScanIDCardManual(
                    configModel,
                    environmentalConditions, apiKey,
                    scnIDCardCallback,
                    kycDocumentDetails, language
                )
                scanIDCardManual.setStepId(stepId?.toString())
                return ScanIDCardResult.Manual(scanIDCardManual);
            } else {
                val scanIDCard = ScanIDCard(
                    configModel,
                    environmentalConditions, apiKey,
                    scnIDCardCallback,
                    kycDocumentDetails, language
                )
                scanIDCard.setStepId(stepId?.toString())
                return ScanIDCardResult.Auto(scanIDCard);
            }


        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanQr(
        scanQrCallback: ScanQrCallback,
        kycDocumentDetails: List<KycDocumentDetails>,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanQrResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context,this.environmentalConditions)) {
                val scanQrManual =  ScanQrManual(
                    kycDocumentDetails,
                    apiKey,
                    language,
                    configModel,
                    environmentalConditions,
                )
                scanQrManual.setStepId(stepId?.toString())
                scanQrManual.setScanQrCallback(scanQrCallback)
                return ScanQrResult.Manual(scanQrManual);
            } else {
                val scanQr =  ScanQr(
                    kycDocumentDetails,
                    apiKey,
                    language,
                    configModel,
                    environmentalConditions,
                )
                scanQr.setStepId(stepId?.toString())
                scanQr.setScanQrCallback(scanQrCallback)
                return ScanQrResult.Auto(scanQr);
            }

        } else {
            throw Exception("Invalid Keys")
        }
    }


    fun startScanOther(
        scanPassportCallback: ScanOtherCallback,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanOtherResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context,this.environmentalConditions)) {
                val scanOtherManual = ScanOtherManual(
                    configModel,
                    environmentalConditions, apiKey,
                    language
                )
                scanOtherManual.setStepId(stepId?.toString())
                scanOtherManual.setScanOtherCallback(scanPassportCallback)
                return ScanOtherResult.Manual(scanOtherManual);
            } else {
                val scanOther = ScanOther(
                    configModel,
                    environmentalConditions, apiKey,
                    language
                )
                scanOther.setStepId(stepId?.toString())
                scanOther.setScanOtherCallback(scanPassportCallback)
                return ScanOtherResult.Auto(scanOther);
            }

        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startFaceMatch(
        faceMatchCallback: FaceMatchCallback,
        image: String,
        showCountDown: Boolean = true,
        stepId: Int? = null,
    ): FaceMatchResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context,this.environmentalConditions)) {
                val faceMatchManual = FaceMatchManual(
                    configModel,
                    environmentalConditions,
                    apiKey,
                )
                faceMatchManual.setStepId(stepId?.toString())
                faceMatchManual.setFaceMatchCallback(faceMatchCallback)
                faceMatchManual.setSecondImage(image)
                return FaceMatchResult.Manual(faceMatchManual)
            } else {
                val faceMatch = FaceMatch(
                    configModel,
                    environmentalConditions, apiKey,
                    performActiveLivenessFace,
                    showCountDown,
                )
                faceMatch.setStepId(stepId?.toString())
                faceMatch.setFaceMatchCallback(faceMatchCallback)
                faceMatch.setSecondImage(image)
                return FaceMatchResult.Auto(faceMatch)
            }

        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startContextAwareSigning(contextAwareSigningCallback: ContextAwareSigningCallback,stepId: Int? = null,): ContextAwareSigning {
        if (isKeyValid) {
            return ContextAwareSigning(
                contextAwareSigningCallback,
                tenantIdentifier!!,
                interaction!!,
                stepId!!,
                configModel!!,
                apiKey!!
            )
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startAssistedDataEntry(assistedDataEntryCallback: AssistedDataEntryCallback,stepId: Int? = null,): AssistedDataEntry {
        if (isKeyValid) {
            val assistedDataEntry =  AssistedDataEntry(
                apiKey!!,
                configModel!!
            )
            assistedDataEntry.setStepId(stepId?.toString())
            assistedDataEntry.setCallback(assistedDataEntryCallback)
            return  assistedDataEntry;
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanNfc(
        scanNfcCallback: ScanNfcCallback,
        languageCode: String = Language.NON,
        context: Context,
    ): ScanNfc {
        return ScanNfc(
            scanNfcCallback = scanNfcCallback,
            languageCode = languageCode,
            apiKey = apiKey!!,
            context = context,
            appConfiguration = configModel!!,
        )
    }


    fun startSubmitData(
        submitDataCallback: SubmitDataCallback,
        submitRequestModel: List<SubmitRequestModel>,
    ): SubmitData {
        if (isKeyValid) {
            return SubmitData(apiKey!!, submitDataCallback, submitRequestModel, configModel!!)
        } else {
            throw Exception("Invalid Keys")
        }
    }

    private fun getTemplatesByCountry() {
        val remoteService = RemoteClient.remoteIdPowerService
        val call: Call<List<Templates>> = remoteService.getTemplates()

        call.enqueue(object : Callback<List<Templates>> {
            override fun onResponse(
                call: Call<List<Templates>>,
                response: Response<List<Templates>>
            ) {
                if (response.isSuccessful) {
                    val remoteResult: List<Templates>? = response.body()
                    val filteredList = filterBySourceCountryCode(remoteResult)
                    val templatesByCountry = ArrayList<TemplatesByCountry>()
                    filteredList?.forEach { data ->
                        val item = TemplatesByCountry(
                            data.id,
                            data.sourceCountry,
                            data.sourceCountryCode,
                            data.sourceCountryFlag,
                            filterTemplatesCountryCode(remoteResult, data.sourceCountryCode)!!
                        )
                        templatesByCountry.add(item)
                    }


                    allTemplates = templatesByCountry;
                    assentifySdkCallback!!.onAssentifySdkInitSuccess(configModel!!);
                }
            }

            override fun onFailure(call: Call<List<Templates>>, t: Throwable) {
            }
        })
    }

    private fun filterBySourceCountryCode(dataList: List<Templates>?): List<Templates>? {
        val filteredList = ArrayList<Templates>()
        val uniqueSourceCountryCodes = ArrayList<String>()
        dataList?.forEach { data ->
            if (!uniqueSourceCountryCodes.contains(data.sourceCountryCode)) {
                filteredList.add(data)
                uniqueSourceCountryCodes.add(data.sourceCountryCode)
            }
        }
        return filteredList
    }

    private fun filterTemplatesCountryCode(
        dataList: List<Templates>?,
        countryCode: String
    ): List<Templates>? {
        val filteredList = ArrayList<Templates>()
        dataList?.forEach { data ->
            if (data.sourceCountryCode == countryCode) {
                filteredList.add(data)
            }
        }
        return filteredList
    }


    private fun filterToSupportedCountries(dataList: List<TemplatesByCountry>?,stepID:Int): List<TemplatesByCountry>? {
        var selectedCountries: List<String> = emptyList();
        var supportedIdCards: List<String> = emptyList();
        configModel!!.stepDefinitions.forEach { step ->
            if (step.stepId == stepID) {
                step.customization.identificationDocuments!!.forEach { docStep ->
                    if (docStep.key == "IdentificationDocument.IdCard") {
                        if (docStep.selectedCountries != null) {
                            selectedCountries = docStep.selectedCountries;
                            supportedIdCards = docStep.supportedIdCards;
                        }
                    }
                }
            }
        }
        val filteredList = ArrayList<TemplatesByCountry>()
        dataList?.forEach { data ->
            val foundCountry: String? = selectedCountries.find { it == data.sourceCountryCode }
            if (foundCountry != null && foundCountry.isNotEmpty()) {
                filteredList.add(data)
            }
        }
        if (selectedCountries.isEmpty()) {
            return dataList;
        }
        val filteredListByCards = mutableListOf<TemplatesByCountry>()

        filteredList.forEach { card ->
            val selectedTemplates = mutableListOf<Templates>()

            card.templates.forEach { cardTemplate ->
                if (supportedIdCards.contains(cardTemplate.id.toString())) {
                    selectedTemplates.add(cardTemplate)
                }
            }

            filteredListByCards.add(
                TemplatesByCountry(
                    id = card.id,
                    name = card.name,
                    sourceCountryCode = card.sourceCountryCode,
                    flag = card.flag,
                    templates = selectedTemplates
                )
            )
        }

        return filteredListByCards
    }


    fun languageTransformation(
        translatedCallback: LanguageTransformationCallback,
        language: String,
        languageTransformationData: List<LanguageTransformationModel>
    ) {
        if (isKeyValid) {
            val translated = LanguageTransformation(apiKey!!);
            translated.setCallback(translatedCallback)
            translated.languageTransformation(
                language,
                TransformationModel(languageTransformationData)
            )
        } else {
            throw Exception("Invalid Keys")
        }
    }


    fun getTemplates(stepID: Int): List<TemplatesByCountry> {
        val stepTemplates = filterToSupportedCountries(allTemplates, stepID)
        return stepTemplates ?: emptyList()
    }
    /** FLOW **/

    // 1 _ Text && Icons are white

    public fun startFlow(flowCallback: FlowCallBack, flowEnvironmentalConditions: FlowEnvironmentalConditions){
         FlowEnvironmentalConditionsObject.setFlowEnvironmentalConditions(flowEnvironmentalConditions);
         ConfigModelObject.setConfigModelObject(configModel!!);
         FlowCallbackObject.setFlowCallbackObject(flowCallback!!);
         ApiKeyObject.setApiKeyObject(apiKey!!);
         val intent = Intent(context, BlockLoaderStepsComposeActivity::class.java)
         context.startActivity(intent)
    }

}
