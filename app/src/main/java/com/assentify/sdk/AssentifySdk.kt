import android.content.Context
import android.util.Log
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.SentryKeys
import com.assentify.sdk.Core.Constants.SentryManager
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.KycDocumentDetails
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
import io.sentry.SentryLevel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssentifySdk(
    context: Context
) {

    private lateinit var apiKey: String;
    private lateinit var configModel: ConfigModel;
    private lateinit var tenantIdentifier: String;
    private lateinit var interaction: String;
    private lateinit var environmentalConditions: EnvironmentalConditions;
    private lateinit var assentifySdkCallback: AssentifySdkCallback;
    private var processMrz: Boolean? = null;
    private var storeCapturedDocument: Boolean? = null;
    private var performLivenessDetection: Boolean? = null;
    private var storeImageStream: Boolean? = null;
    private var saveCapturedVideoID: Boolean? = null;
    private var saveCapturedVideoFace: Boolean? = null;
    private var stepID: Int = -1;

    private var assentifySdkPreferencesManager: AssentifySdkPreferencesManager
    private var assentifyPreferencesData: AssentifyPreferencesData? = null;


    init {
        assentifySdkPreferencesManager = AssentifySdkPreferencesManager(context)
        assentifyPreferencesData = assentifySdkPreferencesManager.getAssentifyPreferencesData();
        if(assentifyPreferencesData != null){
            this.apiKey = assentifyPreferencesData!!.apiKey;
            this.configModel = assentifyPreferencesData!!.configModel!!;
            this.tenantIdentifier = assentifyPreferencesData!!.tenantIdentifier;
            this.interaction = assentifyPreferencesData!!.interaction;
            this.environmentalConditions = assentifyPreferencesData!!.environmentalConditions!!;
            this.processMrz = assentifyPreferencesData!!.processMrz;
            this.storeCapturedDocument = assentifyPreferencesData!!.storeCapturedDocument;
            this.performLivenessDetection = assentifyPreferencesData!!.performLivenessDetection;
            this.storeImageStream = assentifyPreferencesData!!.storeImageStream;
            this.saveCapturedVideoID = assentifyPreferencesData!!.saveCapturedVideoID;
            this.saveCapturedVideoFace = assentifyPreferencesData!!.saveCapturedVideoFace;
        }
    }

    fun initialize(
        apiKey: String,
        tenantIdentifier: String,
        interaction: String,
        environmentalConditions: EnvironmentalConditions?,
        assentifySdkCallback: AssentifySdkCallback?,
        processMrz: Boolean? = null,
        storeCapturedDocument: Boolean? = null,
        performLivenessDetection: Boolean? = null,
        storeImageStream: Boolean? = null,
        saveCapturedVideoID: Boolean? = null,
        saveCapturedVideoFace: Boolean? = null,
    ) {
        this.apiKey = apiKey;
        this.tenantIdentifier = tenantIdentifier;
        this.interaction = interaction;
        this.environmentalConditions = environmentalConditions!!;
        this.assentifySdkCallback = assentifySdkCallback!!;
        this.processMrz = processMrz!!;
        this.storeCapturedDocument = storeCapturedDocument!!;
        this.performLivenessDetection = performLivenessDetection!!;
        this.storeImageStream = storeImageStream!!;
        this.saveCapturedVideoID = saveCapturedVideoID!!;
        this.saveCapturedVideoFace = saveCapturedVideoFace!!;
        validateKey()
    }

    private fun validateKey() {
        val remoteService = remoteAuthenticationService
        val call = remoteService.validateKey(apiKey, tenantIdentifier, "SDK")
        call.enqueue(object : Callback<ValidateKeyModel> {
            override fun onResponse(
                call: Call<ValidateKeyModel>,
                response: Response<ValidateKeyModel>
            ) {
                if (response.isSuccessful) {
                    if (response.body() != null && response.body()!!.statusCode == 200 && response.body()!!.isSuccessful) {
                        getStart()
                    }
                } else {
                    assentifySdkCallback!!.onAssentifySdkInitError("Invalid Keys");
                    SentryManager.registerEvent(
                        SentryKeys.KeyValidation + ":" + "Invalid Keys ",
                        SentryLevel.ERROR
                    )

                }
            }

            override fun onFailure(call: Call<ValidateKeyModel>, t: Throwable) {
                SentryManager.registerEvent(
                    SentryKeys.KeyValidation + ":" + t.message,
                    SentryLevel.ERROR
                )
                assentifySdkCallback!!.onAssentifySdkInitError("Invalid Keys");
            }
        })
    }

    private fun getStart() {
        val remoteService = RemoteClient.remoteApiService
        val call = remoteService.getStart(interaction)
        call.enqueue(object : Callback<ConfigModel> {
            override fun onResponse(
                call: Call<ConfigModel>,
                response: Response<ConfigModel>
            ) {
                if (response.isSuccessful) {
                    configModel = response.body()!!
                    assentifySdkPreferencesManager.saveAssentifyPreferencesData(
                        apiKey,
                        configModel,
                        tenantIdentifier,
                        interaction,
                        environmentalConditions,
                        processMrz,
                        storeCapturedDocument,
                        performLivenessDetection,
                        storeImageStream,
                        saveCapturedVideoID,
                        saveCapturedVideoFace
                    );
                    assentifySdkCallback!!.onAssentifySdkInitSuccess(configModel!!);
                    GlobalScope.launch {
                        configModel.stepDefinitions.forEach { item ->
                            if (item.stepDefinition == "IdentificationDocumentCapture") {
                                if (processMrz == null) {
                                    processMrz = item.customization.ProcessMrz;
                                }
                                if (storeCapturedDocument == null) {
                                    storeCapturedDocument =
                                        item.customization.StoreCapturedDocument;
                                }
                                if (saveCapturedVideoID == null) {
                                    saveCapturedVideoID = item.customization.SaveCapturedVideo;
                                }
                            }//
                            if (item.stepDefinition == "FaceImageAcquisition") {
                                if (performLivenessDetection == null) {
                                    performLivenessDetection =
                                        item.customization.PerformLivenessDetection;
                                }
                                if (storeImageStream == null) {
                                    storeImageStream = item.customization.StoreImageStream;
                                }
                                if (saveCapturedVideoFace == null) {
                                    saveCapturedVideoFace = item.customization.SaveCapturedVideo;
                                }
                            }
                            if (item.stepDefinition == "ContextAwareSigning") {
                                stepID = item.stepId;
                            }
                        }
                        if (processMrz == null || storeCapturedDocument == null || saveCapturedVideoID == null) {
                            assentifySdkCallback!!.onAssentifySdkInitError("Please Configure The IdentificationDocumentCapture { processMrz , storeCapturedDocument , saveCapturedVideo }  ");
                            SentryManager.registerEvent(
                                SentryKeys.Initialized + ":" + "Please Configure The IdentificationDocumentCapture { processMrz , storeCapturedDocument , saveCapturedVideo }  ",
                                SentryLevel.ERROR
                            )
                        }
                        if (performLivenessDetection == null || storeImageStream == null || saveCapturedVideoFace == null) {
                            assentifySdkCallback!!.onAssentifySdkInitError("Please Configure The FaceImageAcquisition { performLivenessDetection , storeImageStream , saveCapturedVideo }  ");
                            SentryManager.registerEvent(
                                SentryKeys.Initialized + ":" + "Please Configure The FaceImageAcquisition { performLivenessDetection , storeImageStream , saveCapturedVideo }  ",
                                SentryLevel.ERROR
                            )

                        }

                    }
                }

            }

            override fun onFailure(call: Call<ConfigModel>, t: Throwable) {
                SentryManager.registerEvent(
                    SentryKeys.Initialized + ":" + t.message,
                    SentryLevel.ERROR
                )
                assentifySdkCallback!!.onAssentifySdkInitError(t.message!!);
            }
        })
    }


    fun startScanPassport(scanPassportCallback: ScanPassportCallback): ScanPassport {
        val scanPassport = ScanPassport(
            configModel,
            environmentalConditions,
            apiKey,
            processMrz,
            performLivenessDetection,
            saveCapturedVideoID,
            storeCapturedDocument,
            storeImageStream
        )
        scanPassport.setScanPassportCallback(scanPassportCallback)
        return scanPassport;
    }

    fun startScanIDCard(
        scnIDCardCallback: IDCardCallback,
        kycDocumentDetails: List<KycDocumentDetails>
    ): ScanIDCard {
        val scanIDCard = ScanIDCard(
            configModel,
            environmentalConditions, apiKey,
            processMrz,
            performLivenessDetection,
            saveCapturedVideoID,
            storeCapturedDocument,
            storeImageStream,
            scnIDCardCallback,
            kycDocumentDetails
        )
        return scanIDCard;

    }


    fun startScanOther(scanPassportCallback: ScanOtherCallback): ScanOther {
        val scanOther = ScanOther(
            configModel,
            environmentalConditions, apiKey,
            processMrz,
            performLivenessDetection,
            saveCapturedVideoID,
            storeCapturedDocument,
            storeImageStream
        )
        scanOther.setScanOtherCallback(scanPassportCallback)
        return scanOther;

    }

    fun startFaceMatch(faceMatchCallback: FaceMatchCallback, image: String): FaceMatch {
        val faceMatch = FaceMatch(
            configModel,
            environmentalConditions, apiKey,
            processMrz,
            performLivenessDetection,
            saveCapturedVideoFace,
            storeCapturedDocument,
            storeImageStream
        )
        faceMatch.setFaceMatchCallback(faceMatchCallback)
        faceMatch.setSecondImage(image)
        return faceMatch;

    }

    fun startContextAwareSigning(contextAwareSigningCallback: ContextAwareSigningCallback): ContextAwareSigning {
        return ContextAwareSigning(
            contextAwareSigningCallback,
            tenantIdentifier,
            interaction,
            stepID,
            configModel!!,
            apiKey
        )

    }


    fun startSubmitData(
        submitDataCallback: SubmitDataCallback,
        submitRequestModel: List<SubmitRequestModel>,
    ): SubmitData {
        return SubmitData(apiKey, submitDataCallback, submitRequestModel, configModel!!)
    }

    fun getTemplates() {
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
                            data.sourceCountry,
                            data.sourceCountryCode,
                            data.sourceCountryFlag,
                            filterTemplatesCountryCode(remoteResult, data.sourceCountryCode)!!
                        )
                        templatesByCountry.add(item)
                    }


                    assentifySdkCallback!!.onHasTemplates(
                        filterToSupportedCountries(
                            templatesByCountry
                        )!!
                    );
                }
            }

            override fun onFailure(call: Call<List<Templates>>, t: Throwable) {
                SentryManager.registerEvent(
                    SentryKeys.HasTemplates + ":" + t.message,
                    SentryLevel.ERROR
                )
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
        configModel.stepDefinitions.forEach { step ->
            if (step.stepDefinition == "IdentificationDocumentCapture") {
                step.customization.identificationDocuments!!.forEach { docStep ->
                    if (docStep.key == "IdentificationDocument.IdCard") {
                        if (docStep.selectedCountries != null) {
                            selectedCountries = docStep.selectedCountries;
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
        return filteredList
    }


}
