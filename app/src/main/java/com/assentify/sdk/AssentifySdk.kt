import android.util.Log
import com.assentify.sdk.CheckEnvironment.ContextAwareSigning
import com.assentify.sdk.ContextAware.ContextAwareSigningCallback
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AssentifySdk(
    private val apiKey: String,
    private val tenantIdentifier: String,
    private val interaction: String,
    private val environmentalConditions: EnvironmentalConditions?,
    private val assentifySdkCallback: AssentifySdkCallback?,
    private var processMrz: Boolean? = null,
    private var storeCapturedDocument: Boolean? = null,
    private var performLivenessDetection: Boolean? = null,
    private var storeImageStream: Boolean? = null,
    private var saveCapturedVideoID: Boolean? = null,
    private var saveCapturedVideoFace: Boolean? = null,
) {

    private var isKeyValid: Boolean = false;
    private lateinit var scanPassport: ScanPassport;
    private lateinit var scanIDCard: ScanIDCard;
    private lateinit var scanOther: ScanOther;
    private lateinit var faceMatch: FaceMatch;
    private lateinit var configModel: ConfigModel;
    private var stepID: Int = -1;


    init {
        require(apiKey.isNotBlank() && apiKey.isNotEmpty()) { "ApiKey must not be blank or null" }
        require(interaction.isNotBlank() && interaction.isNotEmpty()) { "Interaction must not be blank or null" }
        require(tenantIdentifier.isNotBlank() && apiKey.isNotEmpty()) { "TenantIdentifier must not be blank or null" }
        require(environmentalConditions != null) { "EnvironmentalConditions must not be  null" }
        require(assentifySdkCallback != null) { "AssentifySdkCallback must not be  null" }
        validateKey()
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
                    assentifySdkCallback!!.onAssentifySdkInitSuccess(configModel!!.stepDefinitions);
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
                        }
                        if (performLivenessDetection == null || storeImageStream == null || saveCapturedVideoFace == null) {
                            assentifySdkCallback!!.onAssentifySdkInitError("Please Configure The FaceImageAcquisition { performLivenessDetection , storeImageStream , saveCapturedVideo }  ");
                        }

                    }
                }

            }

            override fun onFailure(call: Call<ConfigModel>, t: Throwable) {
                assentifySdkCallback!!.onAssentifySdkInitError(t.message!!);
            }
        })
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
                        isKeyValid = true;
                        getStart()
                    }
                } else {
                    isKeyValid = false;
                    assentifySdkCallback!!.onAssentifySdkInitError("Invalid Keys");
                }
            }

            override fun onFailure(call: Call<ValidateKeyModel>, t: Throwable) {
                isKeyValid = false;
                assentifySdkCallback!!.onAssentifySdkInitError("Invalid Keys");
            }
        })
    }

    fun startScanPassport(scanPassportCallback: ScanPassportCallback): ScanPassport {
        if (isKeyValid) {
            scanPassport = ScanPassport(
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
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startScanIDCard(scnIDCardCallback: IDCardCallback,kycDocumentDetails : List<KycDocumentDetails>): ScanIDCard {
        if (isKeyValid) {
            scanIDCard = ScanIDCard(
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
        } else {
            throw Exception("Invalid Keys")
        }
    }



    fun startScanOther(scanPassportCallback: ScanOtherCallback): ScanOther {
        if (isKeyValid) {
            scanOther = ScanOther(
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
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startFaceMatch(faceMatchCallback: FaceMatchCallback, image: String): FaceMatch {
        if (isKeyValid) {
            faceMatch = FaceMatch(
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
        } else {
            throw Exception("Invalid Keys")
        }
    }

    fun startContextAwareSigning(contextAwareSigningCallback: ContextAwareSigningCallback): ContextAwareSigning {
        if (isKeyValid) {
            return ContextAwareSigning(
                contextAwareSigningCallback,
                tenantIdentifier,
                interaction,
                stepID,
                configModel!!,
                apiKey
            )
        } else {
            throw Exception("Invalid Keys")
        }
    }


    fun startSubmitData(
        submitDataCallback: SubmitDataCallback,
        submitRequestModel: List<SubmitRequestModel>,
    ): SubmitData {
        if (isKeyValid) {
            return SubmitData(apiKey, submitDataCallback, submitRequestModel, configModel!!)
        } else {
            throw Exception("Invalid Keys")
        }
    }

  fun getTemplates() {
    val remoteService = RemoteClient.remoteIdPowerService
    val call: Call<List<Templates>> = remoteService.getTemplates()

    call.enqueue(object : Callback<List<Templates>> {
      override fun onResponse(call: Call<List<Templates>>, response: Response<List<Templates>>) {
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


          assentifySdkCallback!!.onHasTemplates(filterToSupportedCountries(templatesByCountry)!!);
        }
      }

      override fun onFailure(call: Call<List<Templates>>, t: Throwable) {
        // Handle failure if needed
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

  private fun filterTemplatesCountryCode(dataList: List<Templates>?, countryCode: String): List<Templates>? {
    val filteredList = ArrayList<Templates>()
    dataList?.forEach { data ->
      if (data.sourceCountryCode == countryCode) {
        filteredList.add(data)
      }
    }
    return filteredList
  }


  private fun filterToSupportedCountries (dataList: List<TemplatesByCountry>?) : List<TemplatesByCountry>? {
    var selectedCountries:List<String> = emptyList();
       configModel.stepDefinitions.forEach{step ->
      if(step.stepDefinition=="IdentificationDocumentCapture"){
        step.customization.identificationDocuments!!.forEach{docStep ->
          if(docStep.key == "IdentificationDocument.IdCard"){
            if(docStep.selectedCountries != null){
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
    if(selectedCountries.isEmpty()){
      return dataList;
    }
    return  filteredList
  }


}
