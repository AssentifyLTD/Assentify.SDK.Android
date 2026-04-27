package  com.assentify.sdk

import ConfigFileManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntry
import com.assentify.sdk.AssistedDataEntry.AssistedDataEntryCallback
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.BackgroundStyle
import com.assentify.sdk.Core.Constants.BackgroundType
import com.assentify.sdk.Core.Constants.BlockLoaderKeys
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.Core.Constants.FlowEnvironmentalConditions
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.StepsNames
import com.assentify.sdk.Core.Constants.WrapUpKeys
import com.assentify.sdk.Core.Constants.getCurrentDateTime
import com.assentify.sdk.Core.FileUtils.ImageUtils
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceMatchManual
import com.assentify.sdk.FaceMatch.FaceMatchResult
import com.assentify.sdk.Flow.BlockLoader.BlockLoaderStepsComposeActivity
import com.assentify.sdk.Flow.Models.FlowCallBack
import com.assentify.sdk.Flow.Models.LocalStepModel
import com.assentify.sdk.LanguageTransformation.LanguageTransformation
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import com.assentify.sdk.LanguageTransformation.Models.LanguageTransformationModel
import com.assentify.sdk.LanguageTransformation.Models.TransformationModel
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.IdentificationDocumentsDocumentType
import com.assentify.sdk.RemoteClient.Models.SubmitRequestModel
import com.assentify.sdk.RemoteClient.Models.Templates
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry
import com.assentify.sdk.RemoteClient.Models.TenantThemeModel
import com.assentify.sdk.RemoteClient.RemoteClient
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
import com.assentify.sdk.logging.BugsnagObject
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssentifySdk(
    private var apiKey: String? = null,
    private var configFileName: String? = null,
    private val environmentalConditions: EnvironmentalConditions,
    private val assentifySdkCallback: AssentifySdkCallback,
    private var performActiveLivenessFace: Boolean? = null,
    private var context: Context,
) {

    private var isKeyValid: Boolean = false;
    private var timeStarted: String = "";
    private var configModel: ConfigModel? = null;
    private var tenantThemeModel: TenantThemeModel? = null;
    private var initContentHash: String? = null;
    var allTemplates: List<TemplatesByCountry> = emptyList();
    private lateinit var configFileManager: ConfigFileManager;


    init {
        if (configModel == null) {
            if (apiKey.isNullOrEmpty()) {
                Log.e("AssentifySdk Init Error ", "ApiKey must not be empty or null")
            }
            if (configFileName.isNullOrEmpty()) {
                Log.e("AssentifySdk Init Error ", "configFileName must not be empty or null")
            }
            if (!apiKey.isNullOrEmpty()) {
                loadLocalFile()
            }
        }

    }
    private fun loadLocalFile() {
        timeStarted = getCurrentDateTime();
        configFileManager = ConfigFileManager(context, "${configFileName}.json")
        configFileManager.initFromAssetsIfNeeded();
        configModel = configFileManager.readEngagement();
        tenantThemeModel = configFileManager.readTheme();
        initContentHash = configFileManager.readContentHash();
        getTemplatesByCountry(configFileManager.readTemplates());
        initializeCheck();
    }

    private fun initializeCheck() {
        val remoteService = RemoteClient.remoteGatewayService
        val call = remoteService.initializeCheck(
            configModel!!.instanceHash,
            ContentHashObject.getValue(configModel!!.instanceHash, context) ?: initContentHash!!,
            configModel!!.tenantIdentifier,
            configModel!!.blockIdentifier,
            configModel!!.instanceId,
            "SDK",
            apiKey!!
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                try {
                    isKeyValid = true;
                    newInstance(context);
                    val bodyString = response.body()?.string()

                    /**Has Change**/
                    val json = JSONObject(bodyString)
                     /** If there is hasChanges param this means there ara no changes **/
                    if (json.has("hasChanges")) {
                        val hasChanges = json.optBoolean("hasChanges", true)
                        val flowInstanceId = json.optString("flowInstanceId", "")
                        val instanceId = json.optString("instanceId", "")
                        val contentHash = json.optString("contentHash", "")
                        configModel!!.flowInstanceId = flowInstanceId;
                        configModel!!.instanceId = instanceId;
                        ContentHashObject.clear(configModel!!.instanceHash,context);
                        ContentHashObject.setValue(contentHash,configModel!!.instanceHash,context);
                        if (!hasChanges) {
                            assentifySdkCallback.onAssentifySdkInitSuccess(configModel!!)
                            return
                        }
                    }

                    /**Has File**/
                    ContentHashObject.clear(configModel!!.instanceHash,context);
                    clearFlow(context)
                    configFileManager.clear()
                    configFileManager.write(bodyString!!)
                    configModel = configFileManager.readEngagement()
                    tenantThemeModel = configFileManager.readTheme()
                    getTemplatesByCountry(configFileManager.readTemplates());
                    ContentHashObject.setValue(configFileManager.readContentHash(),configModel!!.instanceHash,context);
                    assentifySdkCallback!!.onAssentifySdkInitSuccess(configModel!!);

                } catch (e: Exception) {
                    isKeyValid = false;
                    assentifySdkCallback.onAssentifySdkInitError(e.message ?: "Unknown initialize error")
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                isKeyValid = false;
                assentifySdkCallback.onAssentifySdkInitError(t.message ?: "Network error")
            }
        })
    }

    fun startScanPassport(
        scanPassportCallback: ScanPassportCallback,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanPassportResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context, this.environmentalConditions)) {
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
        templatesByCountry: TemplatesByCountry,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanIDCardResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context, this.environmentalConditions)) {
                val scanIDCardManual = ScanIDCardManual(
                    configModel,
                    environmentalConditions, apiKey,
                    scnIDCardCallback,
                    templatesByCountry, language
                )
                scanIDCardManual.setStepId(stepId?.toString())
                return ScanIDCardResult.Manual(scanIDCardManual);
            } else {
                val scanIDCard = ScanIDCard(
                    configModel,
                    environmentalConditions, apiKey,
                    scnIDCardCallback,
                    templatesByCountry, language
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
        templatesByCountry: TemplatesByCountry,
        language: String = Language.NON,
        stepId: Int? = null,
    ): ScanQrResult {
        if (isKeyValid) {
            if (ImageUtils.isLowCapabilities(context, this.environmentalConditions)) {
                val scanQrManual = ScanQrManual(
                    templatesByCountry,
                    apiKey,
                    language,
                    configModel,
                    environmentalConditions,
                )
                scanQrManual.setStepId(stepId?.toString())
                scanQrManual.setScanQrCallback(scanQrCallback)
                return ScanQrResult.Manual(scanQrManual);
            } else {
                val scanQr = ScanQr(
                    templatesByCountry,
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
            if (ImageUtils.isLowCapabilities(context, this.environmentalConditions)) {
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
            if (ImageUtils.isLowCapabilities(context, this.environmentalConditions)) {
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

    fun startContextAwareSigning(
        contextAwareSigningCallback: ContextAwareSigningCallback,
        stepId: Int? = null,
    ): ContextAwareSigning {
        if (isKeyValid) {
            return ContextAwareSigning(
                contextAwareSigningCallback,
                configModel!!.tenantIdentifier,
                configModel!!.instanceHash,
                stepId!!,
                configModel!!,
                apiKey!!
            )
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startAssistedDataEntry(
        assistedDataEntryCallback: AssistedDataEntryCallback,
        stepId: Int? = null,
    ): AssistedDataEntry {
        if (isKeyValid) {
            val assistedDataEntry = AssistedDataEntry(
                apiKey!!,
                configModel!!
            )
            assistedDataEntry.setCallback(assistedDataEntryCallback)
            assistedDataEntry.setStepId(stepId?.toString())
            return assistedDataEntry;
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
        customProperties: MutableMap<String, String> = mutableMapOf()
    ): SubmitData {
        if (isKeyValid) {
            val submitList = mutableListOf<SubmitRequestModel>()
            submitList.addAll(submitRequestModel);
            val initSteps = configModel!!.stepDefinitions

            /** WrapUp **/
            val valuesWrapUp: MutableMap<String, String> = mutableMapOf()
            initSteps.forEach { item ->
                if (item.stepDefinition == StepsNames.WrapUp) {
                    item.outputProperties.forEach { property ->
                        if (property.key.contains(WrapUpKeys.TimeEnded)) {
                            valuesWrapUp.put(property.key, getCurrentDateTime())
                        }
                    }
                }
            }
            if (submitList.filter { it.stepDefinition == StepsNames.WrapUp }
                    .isEmpty()) {
                submitList.add(
                    SubmitRequestModel(
                        stepDefinition = StepsNames.WrapUp,
                        stepId = configModel!!.stepDefinitions.filter { it.stepDefinition == StepsNames.WrapUp }
                            .first().stepId,
                        extractedInformation = valuesWrapUp
                    ));
            }
            /** BlockLoader **/
            val valuesBlockLoader: MutableMap<String, String> = mutableMapOf()
            initSteps.forEach { item ->
                if (item.stepDefinition == StepsNames.BlockLoader) {
                    item.outputProperties.forEach { property ->
                        if (property.key.contains(BlockLoaderKeys.TimeStarted)) {
                            valuesBlockLoader.put(property.key, timeStarted)
                        }
                        if (property.key.contains(BlockLoaderKeys.DeviceName)) {
                            valuesBlockLoader.put(
                                property.key,
                                "${Build.MANUFACTURER} ${Build.MODEL}"
                            )
                        }
                        if (property.key.contains(BlockLoaderKeys.Application)) {
                            valuesBlockLoader.put(property.key, configModel!!.applicationId)
                        }
                        if (property.key.contains(BlockLoaderKeys.FlowName)) {
                            valuesBlockLoader.put(property.key, configModel!!.flowName)
                        }
                        if (property.key.contains(BlockLoaderKeys.InstanceHash)) {
                            valuesBlockLoader.put(property.key, configModel!!.instanceHash)
                        }
                        if (property.key.contains(BlockLoaderKeys.UserAgent)) {
                            val userAgent = System.getProperty("http.agent")
                                ?: "Android ${Build.VERSION.RELEASE}; ${Build.MODEL}"
                            valuesBlockLoader.put(property.key, userAgent)
                        }
                        if (property.key.contains(BlockLoaderKeys.InteractionID)) {
                            valuesBlockLoader.put(property.key, configModel!!.instanceId)
                        }
                    }
                    item.outputProperties.forEach { property ->
                        customProperties.forEach { customProperty ->
                            if (property.key.contains(customProperty.key)) {
                                valuesBlockLoader.put(property.key, customProperty.value.toString())
                            }
                        }
                    }
                }
            }
            if (submitList.filter { it.stepDefinition == StepsNames.BlockLoader }
                    .isEmpty()) {
                submitList.add(
                    SubmitRequestModel(
                        stepDefinition = StepsNames.BlockLoader,
                        stepId = configModel!!.stepDefinitions.filter { it.stepDefinition == StepsNames.BlockLoader }
                            .first().stepId,
                        extractedInformation = valuesBlockLoader
                    ));
            }
            /****/
            return SubmitData(apiKey!!, submitDataCallback, submitList, configModel!!)
        } else {
            throw Exception("Invalid Keys")
        }
    }

    private fun getTemplatesByCountry(data:List<Templates>?) {
        val remoteResult: List<Templates>? =data
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


    private fun filterToSupportedCountries(
        dataList: List<TemplatesByCountry>?,
        stepID: Int
    ): List<TemplatesByCountry>? {
        var selectedCountries: List<String> = emptyList();
        var supportedIdCards: List<String> = emptyList();
        configModel!!.stepDefinitions.forEach { step ->
            if (step.stepId == stepID) {
                step.customization.identificationDocuments!!.forEach { docStep ->
                    if (docStep.documentType == IdentificationDocumentsDocumentType.ID) {
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

    public fun startFlow(
        activityContext: Context,
        flowCallback: FlowCallBack,
        flowEnvironmentalConditions: FlowEnvironmentalConditions
    ) {
        if (isKeyValid) {
            if (flowEnvironmentalConditions.logoUrl.isEmpty()) {
                flowEnvironmentalConditions.logoUrl = tenantThemeModel!!.logoIcon!!;
            }
            if (flowEnvironmentalConditions.svgBackgroundImageUrl.isEmpty()) {
                flowEnvironmentalConditions.svgBackgroundImageUrl =
                    "tenantThemeModel!!.svgBackgroundImageUrl!!";
            }
            if (flowEnvironmentalConditions.textColor.isEmpty()) {
                flowEnvironmentalConditions.textColor = tenantThemeModel!!.textColor;
            }
            if (flowEnvironmentalConditions.secondaryTextColor.isEmpty()) {
                flowEnvironmentalConditions.secondaryTextColor =
                    tenantThemeModel!!.secondaryTextColor;
            }
            if (flowEnvironmentalConditions.backgroundCardColor.isEmpty()) {
                flowEnvironmentalConditions.backgroundCardColor =
                    tenantThemeModel!!.backgroundCardColor;
            }
            if (flowEnvironmentalConditions.accentColor.isEmpty()) {
                flowEnvironmentalConditions.accentColor = tenantThemeModel!!.accentColor;
            }
            if (flowEnvironmentalConditions.backgroundColor == null) {
                if (flowEnvironmentalConditions.backgroundType == BackgroundType.Color) {
                    flowEnvironmentalConditions.backgroundColor =
                        BackgroundStyle.Solid(tenantThemeModel!!.backgroundBodyColor)
                } else {
                    flowEnvironmentalConditions.backgroundColor =
                        BackgroundStyle.Solid(tenantThemeModel!!.backgroundCardColor)
                }
            }
            if (flowEnvironmentalConditions.clickColor == null) {
                flowEnvironmentalConditions.clickColor =
                    BackgroundStyle.Solid(tenantThemeModel!!.accentColor)
            }

            /** **/

            FlowCallbackObject.setFlowCallbackObject(flowCallback!!);
            ApiKeyObject.setApiKeyObject(apiKey!!);
            ContextObject.init(activityContext);
            InteractionObject.setInteractionObject(configModel!!.instanceHash);
            FlowEnvironmentalConditionsObject.setFlowEnvironmentalConditions(
                flowEnvironmentalConditions
            )

            /** Local Data Base **/
            if (ConfigModelObject.getConfigModelObject() != null) {
                configModel = ConfigModelObject.getConfigModelObject();
                BugsnagObject.initialize(activityContext, configModel!!)
                AssentifySdkObject.setAssentifySdkObject(this)
            } else {
                ConfigModelObject.setConfigModelObject(
                    configModel!!
                )
                BugsnagObject.initialize(activityContext, configModel!!)
                AssentifySdkObject.setAssentifySdkObject(this)
            }
            /****/

            val intent = Intent(activityContext, BlockLoaderStepsComposeActivity::class.java)
            activityContext.startActivity(intent)
        }


    }

    public fun clearFlow(activityContext: Context) {
        ContextObject.init(activityContext);
        InteractionObject.setInteractionObject(configModel!!.instanceHash);
        ConfigModelObject.setConfigModelObject(
            null
        )
        LocalStepsObject.setLocalSteps(
            emptyList<LocalStepModel>().toMutableList()
        )
    }

    private fun newInstance(activityContext: Context) {
        ContextObject.init(activityContext);
        InteractionObject.setInteractionObject(configModel!!.instanceHash);
        ConfigModelObject.setConfigModelObject(
            null
        )
    }

}
