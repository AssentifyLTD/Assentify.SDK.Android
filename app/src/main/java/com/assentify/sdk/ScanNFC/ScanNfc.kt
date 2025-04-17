package com.assentify.sdk.ScanNFC

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import com.assentify.sdk.Core.Constants.ConstantsValues
import com.assentify.sdk.Core.Constants.FullNameKey
import com.assentify.sdk.Core.Constants.IdentificationDocumentCaptureKeys
import com.assentify.sdk.Core.Constants.Language
import com.assentify.sdk.Core.Constants.getIgnoredProperties
import com.assentify.sdk.Core.Constants.getRemainingWords
import com.assentify.sdk.Core.Constants.getSelectedWords
import com.assentify.sdk.Core.Constants.preparePropertiesToTranslate
import com.assentify.sdk.LanguageTransformation.LanguageTransformation
import com.assentify.sdk.LanguageTransformation.LanguageTransformationCallback
import com.assentify.sdk.RemoteClient.RemoteClient
import com.assentify.sdk.ScanPassport.PassportResponseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.CardService
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import org.jmrtd.BACKey
import org.jmrtd.BACKeySpec
import org.jmrtd.PassportService
import org.jmrtd.lds.CardAccessFile
import org.jmrtd.lds.ChipAuthenticationPublicKeyInfo
import org.jmrtd.lds.PACEInfo
import org.jmrtd.lds.SecurityInfo
import org.jmrtd.lds.icao.DG14File
import org.jmrtd.lds.icao.DG1File
import org.jmrtd.lds.icao.DG2File
import org.jmrtd.lds.icao.MRZInfo
import org.jmrtd.lds.iso19794.FaceImageInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import org.json.JSONObject
import java.net.URLEncoder

class ScanNfc(
    private val scanNfcCallback: ScanNfcCallback,
    private val languageCode : String,
    private val apiKey:String,
    private val context:Context,
    private val appConfiguration: ConfigModel,
) : LanguageTransformationCallback {


    private var passportResponseModel: PassportResponseModel? = null;

    /** isNfcSupported **/
    fun isNfcSupported(activity: Activity): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        return nfcAdapter != null
    }

    /** isNfcEnabled **/
    fun isNfcEnabled(activity: Activity): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        return nfcAdapter?.isEnabled == true
    }


    /** onActivityNewIntent **/
    fun onActivityNewIntent(intent: Intent, dataModel: PassportResponseModel) {
        passportResponseModel = dataModel;
        if (NfcAdapter.ACTION_TECH_DISCOVERED == intent.action) {
            val tag = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
            if (tag?.techList?.contains(ConstantsValues.NfcTechTag) == true) {
                 val bacKey: BACKeySpec = BACKey(
                      dataModel.passportExtractedModel?.identificationDocumentCapture?.documentNumber.toString(),
                      formatDateToMRZ(dataModel.passportExtractedModel?.identificationDocumentCapture?.birthDate.toString()),
                      formatDateToMRZ(dataModel.passportExtractedModel?.identificationDocumentCapture?.expiryDate.toString()),
                  )
                ReadTask(IsoDep.get(tag), bacKey).start()
                scanNfcCallback.onStartNfcScan();

            }
        }
    }
    private fun formatDateToMRZ(dateStr: String): String {
        val parts = dateStr.split("/")
        if (parts.size != 3) {
            throw IllegalArgumentException("Invalid date format. Expected DD/MM/YYYY")
        }
        val day = parts[0].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[2].takeLast(2)
        return "$year$month$day"
    }

    /** ReadTask **/
    @SuppressLint("StaticFieldLeak")
    private inner class ReadTask(private val isoDep: IsoDep, private val bacKey: BACKeySpec) {

        private lateinit var dg1File: DG1File
        private lateinit var dg2File: DG2File
        private var chipAuthSucceeded = false

        private val coroutineScope = CoroutineScope(Dispatchers.Main)

        fun start() {
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { performReadTask() }
                    onPostExecute(result)
                } catch (e: Exception) {
                    onPostExecute(e)
                }
            }
        }

        private suspend fun performReadTask(): Exception? {
            return try {
                isoDep.timeout = 10000
                val cardService = CardService.getInstance(isoDep)
                cardService.open()
                val service = PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    false,
                    false,
                )
                service.open()

                var paceSucceeded = false
                try {
                    val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                    val securityInfoCollection = cardAccessFile.securityInfos
                    for (securityInfo: SecurityInfo in securityInfoCollection) {
                        if (securityInfo is PACEInfo) {
                            service.doPACE(
                                bacKey,
                                securityInfo.objectIdentifier,
                                PACEInfo.toParameterSpec(securityInfo.parameterId),
                                null,
                            )
                            paceSucceeded = true
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        //scanNfcCallback.onErrorNfcScan(passportResponseModel!!,e.message!!);
                    }
                }
                service.sendSelectApplet(paceSucceeded)
                if (!paceSucceeded) {
                    try {
                        service.getInputStream(PassportService.EF_COM).read()
                    } catch (e: Exception) {
                        service.doBAC(bacKey)
                    }
                }
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)
                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)

                doChipAuth(service)
                null // No error
            } catch (e: Exception) {
                e // Return the exception
            }
        }

        private suspend fun doChipAuth(service: PassportService) {
            try {
                val dg14In = service.getInputStream(PassportService.EF_DG14)
                val dg14Encoded = IOUtils.toByteArray(dg14In)
                val dg14InByte = ByteArrayInputStream(dg14Encoded)
                val dg14File = DG14File(dg14InByte)
                val dg14FileSecurityInfo = dg14File.securityInfos
                for (securityInfo: SecurityInfo in dg14FileSecurityInfo) {
                    if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                        service.doEACCA(
                            securityInfo.keyId,
                            ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                            securityInfo.objectIdentifier,
                            securityInfo.subjectPublicKey,
                        )
                        chipAuthSucceeded = true
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                   // scanNfcCallback.onErrorNfcScan(passportResponseModel!!,e.message!!);
                }
            }
        }

        private fun onPostExecute(exception: Exception?) {
            if (exception == null) {
                try {
                    val mrzInfo = dg1File.mrzInfo
                    val allFaceImageInfo: MutableList<FaceImageInfo> = ArrayList()
                    dg2File.faceInfos.forEach {
                        allFaceImageInfo.addAll(it.faceImageInfos)
                    }
                    if (allFaceImageInfo.isNotEmpty()) {
                        val faceImageInfo = allFaceImageInfo.first()
                        val imageLength = faceImageInfo.imageLength
                        val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                        val buffer = ByteArray(imageLength)
                        dataInputStream.readFully(buffer, 0, imageLength)
                        val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)
                        val finalBitmap = NfcImageUtil.decodeImage(faceImageInfo.mimeType, inputStream)
                        uploadImage(finalBitmap,mrzInfo)
                    }

                } catch (e:Exception) {
                    scanNfcCallback.onErrorNfcScan(passportResponseModel!!,"Chip Auth Not Succeeded");
                }
            } else {
                scanNfcCallback.onErrorNfcScan(passportResponseModel!!,exception.message!!);
            }
        }
    }

    /** Upload Image **/
   private fun uploadImage(
        bitmap:Bitmap,
        mrzInfo: MRZInfo,
    ) {
        val (image, fileName) =  createTimestampedTempFile(bitmap)!!;
        val fileRequestBody = RequestBody.create(null,image)
        val filePart = MultipartBody.Part.createFormData(
            "asset", fileName, fileRequestBody
        )

        val path = URLEncoder.encode("${appConfiguration.tenantIdentifier}/${appConfiguration.blockIdentifier}/${appConfiguration.instanceId}/${fileName}", "UTF-8")
        val call = RemoteClient.remoteBlobStorageService.uploadImageFile(
            apiKey = apiKey,
            tenantId = appConfiguration.tenantIdentifier,
            blockId = appConfiguration.blockIdentifier,
            instanceId = appConfiguration.instanceId,
            filePath = path,
            asset = filePart,
        )

        call.enqueue(object : Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null) {
                        val responseBodyString = responseBody.string()
                        val jsonObject = JSONObject(responseBodyString)
                        val uploadedUrl = jsonObject.getString("url")
                        passportResponseModel!!.passportExtractedModel?.faces = mutableListOf<String>();
                        val faces = mutableListOf<String>()
                        faces.add(uploadedUrl);
                        passportResponseModel!!.passportExtractedModel?.faces = faces;
                        replaceDataWithNfcData(mrzInfo);
                    }
                }else{
                    replaceDataWithNfcData(mrzInfo);
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                replaceDataWithNfcData(mrzInfo);
            }
        })

    }

    private fun createTimestampedTempFile(bitmap: Bitmap): Pair<File, String>? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}.jpg"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)  // 85% quality
                fos.flush()
            }

            Pair(tempFile, fileName)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }


    /** Replace Data With Nfc Data **/
    private fun replaceDataWithNfcData(mRZInfo: MRZInfo) {
        val outputProperties = HashMap<String, Any>()
        passportResponseModel!!.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
            when {
                key.contains(IdentificationDocumentCaptureKeys.name) -> {
                    outputProperties[key] = mRZInfo.secondaryIdentifier.replace("<", "")
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.name = mRZInfo.secondaryIdentifier.replace("<", "")
                }
                key.contains(IdentificationDocumentCaptureKeys.surname) -> {
                    outputProperties[key] = mRZInfo.primaryIdentifier.replace("<", "")
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.surname = mRZInfo.primaryIdentifier.replace("<", "")
                }
                key.contains(IdentificationDocumentCaptureKeys.nationality) -> {
                    outputProperties[key] = mRZInfo.nationality
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.nationality = mRZInfo.nationality.replace("<", "")
                }
                key.contains(IdentificationDocumentCaptureKeys.documentNumber) -> {
                    outputProperties[key] = mRZInfo.documentNumber.replace("<", "")
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.documentNumber = mRZInfo.documentNumber.replace("<", "")
                }
                key.contains(IdentificationDocumentCaptureKeys.sex) -> {
                    outputProperties[key] = mRZInfo.gender.name.replace("<", "")
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.sex = mRZInfo.gender.name.replace("<", "")
                }
                else -> outputProperties[key] = value
            }
        }

        val extractedData = HashMap<String, Any>()
        outputProperties.forEach { (key, value) ->
            val newKey = key.substring(key.indexOf("IdentificationDocumentCapture_") + "IdentificationDocumentCapture_".length)
                .replace("_", " ")
            extractedData[newKey] = value
        }

        passportResponseModel!!.passportExtractedModel?.extractedData = mutableMapOf()
        passportResponseModel!!.passportExtractedModel?.transformedProperties = mutableMapOf()

        passportResponseModel!!.passportExtractedModel?.outputProperties = outputProperties
        passportResponseModel!!.passportExtractedModel?.transformedProperties =
            outputProperties.mapValues { it.value.toString() }
        passportResponseModel!!.passportExtractedModel?.extractedData = extractedData

        if (languageCode == Language.NON) {
            scanNfcCallback.onCompleteNfcScan(passportResponseModel!!)
        } else {
            if(apiKey.isNotEmpty()){
                val translated = LanguageTransformation(apiKey);
                translated.setCallback(this);
                translated.languageTransformation(languageCode,
                    preparePropertiesToTranslate(languageCode, passportResponseModel!!.passportExtractedModel?.outputProperties!!))
            }else{
                scanNfcCallback.onCompleteNfcScan(passportResponseModel!!)
            }
        }


    }

    /** Language Transformation **/
    private var nameKey: String = ""
    private var nameWordCount: Int = 0
    private var surnameKey: String = ""
    override fun onTranslatedSuccess(properties: Map<String, String>?) {
        properties?.let { props ->

            passportResponseModel!!.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
                when {
                    key.contains(IdentificationDocumentCaptureKeys.name) -> {
                        nameKey = key
                        nameWordCount = if (value.toString().trim().isEmpty()) 0 else value.toString().trim().split("\\s+".toRegex()).size
                    }
                    key.contains(IdentificationDocumentCaptureKeys.surname) -> {
                        surnameKey = key
                    }
                }
            }

            passportResponseModel!!.passportExtractedModel?.transformedProperties = mutableMapOf()
            passportResponseModel!!.passportExtractedModel?.extractedData = mutableMapOf()

            val tempTransformedProperties = mutableMapOf<String, String>()
            val tempExtractedData = mutableMapOf<String, Any>()

            props.forEach { (key, value) ->
                when (key) {
                    FullNameKey -> {
                        if (nameKey.isNotEmpty()) {
                            tempTransformedProperties[nameKey] = getSelectedWords(value.toString(), nameWordCount)
                            tempExtractedData["name"] = getSelectedWords(value.toString(), nameWordCount)
                        }
                        if (surnameKey.isNotEmpty()) {
                            tempTransformedProperties[surnameKey] = getRemainingWords(value.toString(), nameWordCount)
                            tempExtractedData["surname"] = getRemainingWords(value.toString(), nameWordCount)
                        }
                    }
                    else -> {
                        tempTransformedProperties[key] = value.toString()
                        val newKey = key.substringAfter("IdentificationDocumentCapture_").replace("_", " ")
                        tempExtractedData[newKey] = value
                    }
                }
            }

            getIgnoredProperties(passportResponseModel!!.passportExtractedModel?.outputProperties!!).forEach { (key, value) ->
                tempTransformedProperties[key] = value
                val newKey = key.substringAfter("IdentificationDocumentCapture_").replace("_", " ")
                tempExtractedData[newKey] = value
            }

            passportResponseModel!!.passportExtractedModel?.transformedProperties = tempTransformedProperties
            passportResponseModel!!.passportExtractedModel?.extractedData = tempExtractedData
        }

        scanNfcCallback.onCompleteNfcScan(passportResponseModel!!)
    }
    override fun onTranslatedError(properties: Map<String, String>?) {
        scanNfcCallback.onCompleteNfcScan(passportResponseModel!!)
    }



}