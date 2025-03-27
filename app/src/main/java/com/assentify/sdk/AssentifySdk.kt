package  com.assentify.sdk

import android.app.Activity
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import android.content.Context
import android.util.Log
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.FileUtils.ReadJSONFromAsset
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.LanguageTransformation.LanguageTransformation
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
import com.assentify.sdk.ScanOther.ScanOther
import com.assentify.sdk.ScanOther.ScanOtherCallback
import com.assentify.sdk.ScanPassport.ScanPassport
import com.assentify.sdk.ScanPassport.ScanPassportCallback
import com.assentify.sdk.SubmitData.SubmitData
import com.assentify.sdk.SubmitData.SubmitDataCallback
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import com.assentify.sdk.RemoteClient.Models.decodeConfigModelFromJson
import com.assentify.sdk.ScanNFC.ScanNfc
import com.assentify.sdk.ScanNFC.ScanNfcCallback
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssentifySdk(
    private var apiKey: String? = null,
    private var tenantIdentifier: String? = null,
    private var interaction: String? = null,
    private val environmentalConditions: EnvironmentalConditions,
    private val assentifySdkCallback: AssentifySdkCallback,
    private var processMrz: Boolean? = null,
    private var storeCapturedDocument: Boolean? = null,
    private var performLivenessDocument: Boolean? = null,
    private var performLivenessFace: Boolean? = null,
    private var storeImageStream: Boolean? = null,
    private var saveCapturedVideoID: Boolean? = null,
    private var saveCapturedVideoFace: Boolean? = null,
    private var context: Context? = null,
) {

    private var isKeyValid: Boolean = false;
    private lateinit var scanPassport: ScanPassport;
    private lateinit var scanIDCard: ScanIDCard;
    private lateinit var scanOther: ScanOther;
    private lateinit var faceMatch: FaceMatch;
    private var configModel: ConfigModel? = null;
    private var stepID: Int = -1;
    private var templates: List<TemplatesByCountry> = emptyList();
    private lateinit var readJSONFromAsset: ReadJSONFromAsset;

    init {
        if (context != null) {
            readJSONFromAsset = ReadJSONFromAsset(context = context!!);
            val jsonString = readJSONFromAsset.readJSONFromAssets("assentify_config.json")
            if (jsonString.isNotEmpty()) {
                configModel = decodeConfigModelFromJson(jsonString)!!;
                interaction = configModel!!.instanceHash;
                tenantIdentifier = configModel!!.tenantIdentifier;
                apiKey = "TODO"
                isKeyValid = true;
                iniSdk();
            } else {
                assentifySdkCallback.onAssentifySdkInitError("Please Configure The assentify_config.json File ");

            }
        }
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
        GlobalScope.launch {
            configModel!!.stepDefinitions.forEach { item ->
                if (item.stepDefinition == "IdentificationDocumentCapture") {
                    if (performLivenessDocument == null) {
                        performLivenessDocument =
                            item.customization.documentLiveness;
                    }
                    if (processMrz == null) {
                        processMrz = item.customization.processMrz;
                    }
                    if (storeCapturedDocument == null) {
                        storeCapturedDocument =
                            item.customization.storeCapturedDocument;
                    }
                    if (saveCapturedVideoID == null) {
                        saveCapturedVideoID = item.customization.saveCapturedVideo;
                    }
                }//
                if (item.stepDefinition == "FaceImageAcquisition") {
                    if (performLivenessFace == null) {
                        performLivenessFace =
                            item.customization.performLivenessDetection;
                    }
                    if (storeImageStream == null) {
                        storeImageStream = item.customization.storeImageStream;
                    }
                    if (saveCapturedVideoFace == null) {
                        saveCapturedVideoFace = item.customization.saveCapturedVideo;
                    }
                }
                if (item.stepDefinition == "ContextAwareSigning") {
                    stepID = item.stepId;
                }
            }
            if (performLivenessDocument == null || processMrz == null || storeCapturedDocument == null || saveCapturedVideoID == null) {
                assentifySdkCallback.onAssentifySdkInitError("Please Configure The IdentificationDocumentCapture { performLivenessDocument ,processMrz , storeCapturedDocument , saveCapturedVideo }  ");
            }
            if (performLivenessFace == null || storeImageStream == null || saveCapturedVideoFace == null) {
                assentifySdkCallback.onAssentifySdkInitError("Please Configure The FaceImageAcquisition {  performLivenessFace , storeImageStream , saveCapturedVideo }  ");

            }

        }
    }

    fun startScanPassport(
        scanPassportCallback: ScanPassportCallback,
        language: String = Language.NON
    ): ScanPassport {
        if (isKeyValid) {
            scanPassport = ScanPassport(
                configModel,
                environmentalConditions,
                apiKey,
                processMrz,
                performLivenessDocument,
                performLivenessFace,
                saveCapturedVideoID,
                storeCapturedDocument,
                storeImageStream, language
            )
            scanPassport.setScanPassportCallback(scanPassportCallback)
            return scanPassport;
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanIDCard(
        scnIDCardCallback: IDCardCallback,
        kycDocumentDetails: List<KycDocumentDetails>, language: String = Language.NON
    ): ScanIDCard {
        if (isKeyValid) {
            scanIDCard = ScanIDCard(
                configModel,
                environmentalConditions, apiKey,
                processMrz,
                performLivenessDocument,
                performLivenessFace,
                saveCapturedVideoID,
                storeCapturedDocument,
                storeImageStream,
                scnIDCardCallback,
                kycDocumentDetails, language
            )
            return scanIDCard;
        } else {
            throw Exception("Invalid Keys")
        }
    }


    fun startScanOther(
        scanPassportCallback: ScanOtherCallback,
        language: String = Language.NON
    ): ScanOther {
        if (isKeyValid) {
            scanOther = ScanOther(
                configModel,
                environmentalConditions, apiKey,
                processMrz,
                performLivenessDocument,
                performLivenessFace,
                saveCapturedVideoID,
                storeCapturedDocument,
                storeImageStream,
                language
            )
            scanOther.setScanOtherCallback(scanPassportCallback)
            return scanOther;
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startFaceMatch(
        faceMatchCallback: FaceMatchCallback,
        image: String,
        showCountDown: Boolean = true
    ): FaceMatch {
        if (isKeyValid) {
            faceMatch = FaceMatch(
                configModel,
                environmentalConditions, apiKey,
                processMrz,
                performLivenessDocument,
                performLivenessFace,
                saveCapturedVideoFace,
                storeCapturedDocument,
                storeImageStream,
                showCountDown,
            )
            faceMatch.setFaceMatchCallback(faceMatchCallback)
            faceMatch.setSecondImage(image)
            return faceMatch;
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startContextAwareSigning(contextAwareSigningCallback: ContextAwareSigningCallback): ContextAwareSigning {
        if (isKeyValid) {
            return ContextAwareSigning(
                contextAwareSigningCallback,
                tenantIdentifier!!,
                interaction!!,
                stepID,
                configModel!!,
                apiKey!!
            )
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanNfc(
        scanNfcCallback: ScanNfcCallback,
        languageCode: String = Language.NON,
        apiKey:String = "",
    ): ScanNfc {
        return ScanNfc(
            scanNfcCallback = scanNfcCallback,
            languageCode = languageCode,
            apiKey =apiKey,
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


                    templates = filterToSupportedCountries(
                        templatesByCountry
                    )!!;

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


    private fun filterToSupportedCountries(dataList: List<TemplatesByCountry>?): List<TemplatesByCountry>? {
        var selectedCountries: List<String> = emptyList();
        var supportedIdCards: List<String> = emptyList();
        configModel!!.stepDefinitions.forEach { step ->
            if (step.stepDefinition == "IdentificationDocumentCapture") {
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


    fun getTemplates() = templates;

}
