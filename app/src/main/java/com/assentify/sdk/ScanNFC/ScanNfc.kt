package com.assentify.sdk.ScanNFC

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Build
import android.util.Log
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
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.RemoteClient
import com.assentify.sdk.ScanPassport.PassportResponseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.CardService
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
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
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanNfc(
    private val scanNfcCallback: ScanNfcCallback,
    private val languageCode: String,
    private val apiKey: String,
    private val context: Context,
    private val appConfiguration: ConfigModel,
) : LanguageTransformationCallback {

    companion object {
        private const val TAG = "AssentifyScanNfc"
    }

    private var passportResponseModel: PassportResponseModel? = null

    /** Helper: readable description of an exception (message can be null) **/
    private fun describe(e: Throwable): String =
        "${e.javaClass.simpleName}: ${e.message ?: "<no message>"}"

    /** isNfcSupported **/
    fun isNfcSupported(activity: Activity): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        if (nfcAdapter == null) {
            Log.w(TAG, "isNfcSupported: NFC adapter is null — device does not support NFC")
        }
        return nfcAdapter != null
    }

    /** isNfcEnabled **/
    fun isNfcEnabled(activity: Activity): Boolean {
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        val enabled = nfcAdapter?.isEnabled == true
        if (!enabled) {
            Log.w(TAG, "isNfcEnabled: NFC is disabled or unavailable (adapter=${nfcAdapter != null})")
        }
        return enabled
    }

    /** onActivityNewIntent **/
    fun onActivityNewIntent(intent: Intent, dataModel: PassportResponseModel) {
        passportResponseModel = dataModel
        Log.d(TAG, "onActivityNewIntent: action=${intent.action}")

        if (NfcAdapter.ACTION_TECH_DISCOVERED != intent.action) {
            Log.w(TAG, "onActivityNewIntent: ignored intent, expected ACTION_TECH_DISCOVERED but got ${intent.action}")
            return
        }

        val tag = try {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S_V2) {
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onActivityNewIntent: failed to read EXTRA_TAG from intent — ${describe(e)}", e)
            null
        }

        if (tag == null) {
            Log.e(TAG, "onActivityNewIntent: NFC tag is null")
            return
        }

        val techList = tag.techList
        Log.d(TAG, "onActivityNewIntent: tag techList=${techList?.joinToString()}")
        if (techList?.contains(ConstantsValues.NfcTechTag) != true) {
            Log.e(TAG, "onActivityNewIntent: tag does not support ${ConstantsValues.NfcTechTag}, techList=${techList?.joinToString()}")
            return
        }

        val capture = dataModel.passportExtractedModel?.identificationDocumentCapture
        if (capture == null) {
            Log.e(TAG, "onActivityNewIntent: identificationDocumentCapture is null — cannot build BAC key")
        }
        val documentNumber = capture?.documentNumber.toString()
        val birthDate = capture?.birthDate.toString()
        val expiryDate = capture?.expiryDate.toString()
        Log.d(TAG, "onActivityNewIntent: BAC input docNumberLength=${documentNumber.length}, birthDate='$birthDate', expiryDate='$expiryDate'")

        if (documentNumber.isBlank() || documentNumber == "null") {
            Log.e(TAG, "onActivityNewIntent: document number is missing — BAC/PACE will fail")
        }

        val bacKey: BACKeySpec = try {
            BACKey(
                documentNumber,
                formatDateToMRZ(birthDate, "birthDate"),
                formatDateToMRZ(expiryDate, "expiryDate"),
            )
        } catch (e: Exception) {
            Log.e(TAG, "onActivityNewIntent: failed to create BAC key — ${describe(e)}", e)
            scanNfcCallback.onErrorNfcScan(dataModel, e.message ?: "Failed to create BAC key")
            return
        }

        try {
            val isoDep = IsoDep.get(tag)
            if (isoDep == null) {
                Log.e(TAG, "onActivityNewIntent: IsoDep.get(tag) returned null")
                scanNfcCallback.onErrorNfcScan(dataModel, "IsoDep not available for this tag")
                return
            }
            ReadTask(isoDep, bacKey).start()
            scanNfcCallback.onStartNfcScan()
        } catch (e: Exception) {
            Log.e(TAG, "onActivityNewIntent: failed to start read task — ${describe(e)}", e)
            scanNfcCallback.onErrorNfcScan(dataModel, e.message ?: "Failed to start NFC read")
        }
    }

    private fun formatDateToMRZ(dateStr: String, fieldName: String = "date"): String {
        val parts = dateStr.split("/")
        if (parts.size != 3) {
            Log.e(TAG, "formatDateToMRZ: invalid $fieldName '$dateStr' — expected DD/MM/YYYY")
            throw IllegalArgumentException("Invalid $fieldName format '$dateStr'. Expected DD/MM/YYYY")
        }
        val day = parts[0].padStart(2, '0')
        val month = parts[1].padStart(2, '0')
        val year = parts[2].takeLast(2)
        val result = "$year$month$day"
        if (result.length != 6 || !result.all { it.isDigit() }) {
            Log.w(TAG, "formatDateToMRZ: $fieldName '$dateStr' produced suspicious MRZ value '$result'")
        }
        return result
    }

    /** ReadTask **/
    @SuppressLint("StaticFieldLeak")
    private inner class ReadTask(private val isoDep: IsoDep, private val bacKey: BACKeySpec) {

        private lateinit var dg1File: DG1File
        private lateinit var dg2File: DG2File
        private var chipAuthSucceeded = false

        private val coroutineScope = CoroutineScope(Dispatchers.Main)

        fun start() {
            Log.d(TAG, "ReadTask.start: launching NFC read")
            coroutineScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) { performReadTask() }
                    onPostExecute(result)
                } catch (e: Exception) {
                    Log.e(TAG, "ReadTask.start: unexpected exception in coroutine — ${describe(e)}", e)
                    onPostExecute(e)
                }
            }
        }

        private suspend fun performReadTask(): Exception? {
            var step = "init"
            return try {
                step = "set IsoDep timeout"
                isoDep.timeout = 10000

                step = "open CardService"
                val cardService = CardService.getInstance(isoDep)
                cardService.open()
                Log.d(TAG, "performReadTask: CardService opened")

                step = "open PassportService"
                val service = PassportService(
                    cardService,
                    PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                    PassportService.DEFAULT_MAX_BLOCKSIZE,
                    false,
                    false,
                )
                service.open()
                Log.d(TAG, "performReadTask: PassportService opened")

                var paceSucceeded = false
                try {
                    step = "read EF_CARD_ACCESS"
                    val cardAccessFile = CardAccessFile(service.getInputStream(PassportService.EF_CARD_ACCESS))
                    val securityInfoCollection = cardAccessFile.securityInfos
                    Log.d(TAG, "performReadTask: EF_CARD_ACCESS has ${securityInfoCollection.size} security infos")
                    for (securityInfo: SecurityInfo in securityInfoCollection) {
                        if (securityInfo is PACEInfo) {
                            step = "doPACE"
                            Log.d(TAG, "performReadTask: attempting PACE oid=${securityInfo.objectIdentifier}, paramId=${securityInfo.parameterId}")
                            service.doPACE(
                                bacKey,
                                securityInfo.objectIdentifier,
                                PACEInfo.toParameterSpec(securityInfo.parameterId),
                                null,
                            )
                            paceSucceeded = true
                            Log.d(TAG, "performReadTask: PACE succeeded")
                        }
                    }
                    if (!paceSucceeded) {
                        Log.w(TAG, "performReadTask: no PACEInfo found in EF_CARD_ACCESS, will fall back to BAC")
                    }
                } catch (e: Exception) {
                    // Expected on chips without PACE — logged, not fatal
                    Log.w(TAG, "performReadTask: PACE failed at step '$step', falling back to BAC — ${describe(e)}", e)
                }

                step = "sendSelectApplet(paceSucceeded=$paceSucceeded)"
                service.sendSelectApplet(paceSucceeded)

                if (!paceSucceeded) {
                    try {
                        step = "read EF_COM (BAC probe)"
                        service.getInputStream(PassportService.EF_COM).read()
                        Log.d(TAG, "performReadTask: EF_COM readable without BAC")
                    } catch (e: Exception) {
                        Log.d(TAG, "performReadTask: EF_COM not readable (${describe(e)}), performing BAC")
                        step = "doBAC"
                        service.doBAC(bacKey)
                        Log.d(TAG, "performReadTask: BAC succeeded")
                    }
                }

                step = "read DG1"
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)
                Log.d(TAG, "performReadTask: DG1 read OK")

                step = "read DG2"
                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)
                Log.d(TAG, "performReadTask: DG2 read OK")

                step = "chip authentication"
                doChipAuth(service)
                Log.d(TAG, "performReadTask: finished, chipAuthSucceeded=$chipAuthSucceeded")
                null // No error
            } catch (e: Exception) {
                Log.e(TAG, "performReadTask: FAILED at step '$step' — ${describe(e)}", e)
                e
            } finally {
                try {
                    if (isoDep.isConnected) isoDep.close()
                } catch (e: Exception) {
                    Log.w(TAG, "performReadTask: failed to close IsoDep — ${describe(e)}", e)
                }
            }
        }

        private suspend fun doChipAuth(service: PassportService) {
            try {
                val dg14In = service.getInputStream(PassportService.EF_DG14)
                val dg14Encoded = IOUtils.toByteArray(dg14In)
                val dg14InByte = ByteArrayInputStream(dg14Encoded)
                val dg14File = DG14File(dg14InByte)
                val dg14FileSecurityInfo = dg14File.securityInfos
                Log.d(TAG, "doChipAuth: DG14 has ${dg14FileSecurityInfo.size} security infos")
                for (securityInfo: SecurityInfo in dg14FileSecurityInfo) {
                    if (securityInfo is ChipAuthenticationPublicKeyInfo) {
                        Log.d(TAG, "doChipAuth: attempting EAC-CA keyId=${securityInfo.keyId}, oid=${securityInfo.objectIdentifier}")
                        service.doEACCA(
                            securityInfo.keyId,
                            ChipAuthenticationPublicKeyInfo.ID_CA_ECDH_AES_CBC_CMAC_256,
                            securityInfo.objectIdentifier,
                            securityInfo.subjectPublicKey,
                        )
                        chipAuthSucceeded = true
                        Log.d(TAG, "doChipAuth: chip authentication succeeded")
                    }
                }
                if (!chipAuthSucceeded) {
                    Log.w(TAG, "doChipAuth: no ChipAuthenticationPublicKeyInfo found in DG14")
                }
            } catch (e: Exception) {
                // Many documents have no DG14 — logged, not fatal
                Log.w(TAG, "doChipAuth: chip authentication failed or DG14 missing — ${describe(e)}", e)
            }
        }

        private fun onPostExecute(exception: Exception?) {
            val model = passportResponseModel
            if (model == null) {
                Log.e(TAG, "onPostExecute: passportResponseModel is null — cannot report result")
                return
            }

            if (exception != null) {
                Log.e(TAG, "onPostExecute: NFC read failed — ${describe(exception)}", exception)
                scanNfcCallback.onErrorNfcScan(model, exception.message ?: exception.javaClass.simpleName)
                return
            }

            try {
                if (!::dg1File.isInitialized) {
                    Log.e(TAG, "onPostExecute: dg1File was never initialized")
                }
                if (!::dg2File.isInitialized) {
                    Log.e(TAG, "onPostExecute: dg2File was never initialized")
                }

                val mrzInfo = dg1File.mrzInfo
                Log.d(TAG, "onPostExecute: MRZ parsed, documentCode=${mrzInfo.documentCode}, nationality=${mrzInfo.nationality}")

                val allFaceImageInfo: MutableList<FaceImageInfo> = ArrayList()
                dg2File.faceInfos.forEach {
                    allFaceImageInfo.addAll(it.faceImageInfos)
                }
                Log.d(TAG, "onPostExecute: found ${allFaceImageInfo.size} face image(s) in DG2")

                if (allFaceImageInfo.isEmpty()) {
                    // Previously this path ended silently with no callback at all.
                    Log.w(TAG, "onPostExecute: DG2 contains no face images — continuing with MRZ data only")
                    replaceDataWithNfcData(mrzInfo)
                    return
                }

                val faceImageInfo = allFaceImageInfo.first()
                val imageLength = faceImageInfo.imageLength
                Log.d(TAG, "onPostExecute: face image mimeType=${faceImageInfo.mimeType}, length=$imageLength")
                val dataInputStream = DataInputStream(faceImageInfo.imageInputStream)
                val buffer = ByteArray(imageLength)
                dataInputStream.readFully(buffer, 0, imageLength)
                val inputStream: InputStream = ByteArrayInputStream(buffer, 0, imageLength)

                val finalBitmap = NfcImageUtil.decodeImage(faceImageInfo.mimeType, inputStream)
                if (finalBitmap == null) {
                    Log.e(TAG, "onPostExecute: NfcImageUtil.decodeImage returned null for mimeType=${faceImageInfo.mimeType}")
                    replaceDataWithNfcData(mrzInfo)
                    return
                }
                Log.d(TAG, "onPostExecute: face bitmap decoded ${finalBitmap.width}x${finalBitmap.height}")
                uploadImage(finalBitmap, mrzInfo)
            } catch (e: Exception) {
                Log.e(TAG, "onPostExecute: failed processing DG1/DG2 — ${describe(e)}", e)
                scanNfcCallback.onErrorNfcScan(model, "Chip Auth Not Succeeded")
            }
        }
    }

    /** Upload Image **/
    private fun uploadImage(
        bitmap: Bitmap,
        mrzInfo: MRZInfo,
    ) {
        val tempFile = createTimestampedTempFile(bitmap)
        if (tempFile == null) {
            Log.e(TAG, "uploadImage: could not create temp file — skipping upload")
            replaceDataWithNfcData(mrzInfo)
            return
        }
        val (image, fileName) = tempFile

        try {
            val fileRequestBody = image.asRequestBody(null)
            val filePart = MultipartBody.Part.createFormData("asset", fileName, fileRequestBody)

            val path = URLEncoder.encode(
                "${appConfiguration.tenantIdentifier}/${appConfiguration.blockIdentifier}/${appConfiguration.instanceId}/${fileName}",
                "UTF-8"
            )
            Log.d(TAG, "uploadImage: uploading $fileName (${image.length()} bytes)")

            val call = RemoteClient.remoteBlobStorageService.uploadImageFile(
                apiKey = apiKey,
                tenantId = appConfiguration.tenantIdentifier,
                blockId = appConfiguration.blockIdentifier,
                instanceId = appConfiguration.instanceId,
                filePath = path,
                asset = filePart,
            )

            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (!response.isSuccessful) {
                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            "<unreadable: ${describe(e)}>"
                        }
                        Log.e(TAG, "uploadImage: HTTP ${response.code()} ${response.message()} — body=$errorBody")
                        replaceDataWithNfcData(mrzInfo)
                        return
                    }

                    try {
                        val responseBody = response.body()
                        if (responseBody == null) {
                            Log.e(TAG, "uploadImage: successful response but body is null")
                            replaceDataWithNfcData(mrzInfo)
                            return
                        }
                        val responseBodyString = responseBody.string()
                        val jsonObject = JSONObject(responseBodyString)
                        if (!jsonObject.has("url")) {
                            Log.e(TAG, "uploadImage: response has no 'url' field — body=$responseBodyString")
                        }
                        val uploadedUrl = jsonObject.getString("url")
                        Log.d(TAG, "uploadImage: uploaded successfully, url=$uploadedUrl")

                        val model = passportResponseModel
                        if (model?.passportExtractedModel == null) {
                            Log.e(TAG, "uploadImage: passportExtractedModel is null — cannot store face URL")
                        }
                        model?.passportExtractedModel?.faces = mutableListOf(uploadedUrl)
                    } catch (e: Exception) {
                        Log.e(TAG, "uploadImage: failed to parse upload response — ${describe(e)}", e)
                    } finally {
                        deleteTempFile(image)
                    }
                    replaceDataWithNfcData(mrzInfo)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e(TAG, "uploadImage: network failure — ${describe(t)}", t)
                    deleteTempFile(image)
                    replaceDataWithNfcData(mrzInfo)
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "uploadImage: failed to build/enqueue upload request — ${describe(e)}", e)
            deleteTempFile(image)
            replaceDataWithNfcData(mrzInfo)
        }
    }

    private fun deleteTempFile(file: File) {
        try {
            if (file.exists() && !file.delete()) {
                Log.w(TAG, "deleteTempFile: could not delete ${file.absolutePath}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "deleteTempFile: error deleting ${file.absolutePath} — ${describe(e)}", e)
        }
    }

    private fun createTimestampedTempFile(bitmap: Bitmap): Pair<File, String>? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_${timeStamp}.jpg"
            val tempFile = File(context.cacheDir, fileName)
            FileOutputStream(tempFile).use { fos ->
                val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                if (!compressed) {
                    Log.e(TAG, "createTimestampedTempFile: bitmap.compress returned false")
                }
                fos.flush()
            }
            Pair(tempFile, fileName)
        } catch (e: IOException) {
            Log.e(TAG, "createTimestampedTempFile: IO error — ${describe(e)}", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "createTimestampedTempFile: unexpected error — ${describe(e)}", e)
            null
        }
    }

    /** Replace Data With Nfc Data **/
    private fun replaceDataWithNfcData(mRZInfo: MRZInfo) {
        val model = passportResponseModel
        if (model == null) {
            Log.e(TAG, "replaceDataWithNfcData: passportResponseModel is null — aborting")
            return
        }

        try {
            val extracted = model.passportExtractedModel
            if (extracted == null) {
                Log.e(TAG, "replaceDataWithNfcData: passportExtractedModel is null")
            }
            if (extracted?.outputProperties == null) {
                Log.w(TAG, "replaceDataWithNfcData: outputProperties is null — nothing to merge")
            }

            val outputProperties = HashMap<String, Any>()
            extracted?.outputProperties?.forEach { (key, value) ->
                when {
                    key.contains(IdentificationDocumentCaptureKeys.name) -> {
                        outputProperties[key] = mRZInfo.secondaryIdentifier.replace("<", "")
                        extracted.identificationDocumentCapture?.name = mRZInfo.secondaryIdentifier.replace("<", "")
                    }
                    key.contains(IdentificationDocumentCaptureKeys.surname) -> {
                        outputProperties[key] = mRZInfo.primaryIdentifier.replace("<", "")
                        extracted.identificationDocumentCapture?.surname = mRZInfo.primaryIdentifier.replace("<", "")
                    }
                    key.contains(IdentificationDocumentCaptureKeys.nationality) -> {
                        outputProperties[key] = mRZInfo.nationality
                        extracted.identificationDocumentCapture?.nationality = mRZInfo.nationality.replace("<", "")
                    }
                    key.contains(IdentificationDocumentCaptureKeys.documentNumber) -> {
                        outputProperties[key] = mRZInfo.documentNumber.replace("<", "")
                        extracted.identificationDocumentCapture?.documentNumber = mRZInfo.documentNumber.replace("<", "")
                    }
                    key.contains(IdentificationDocumentCaptureKeys.sex) -> {
                        outputProperties[key] = mRZInfo.gender.name.replace("<", "")
                        extracted.identificationDocumentCapture?.sex = mRZInfo.gender.name.replace("<", "")
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

            extracted?.extractedData = mutableMapOf()
            extracted?.transformedProperties = mutableMapOf()
            extracted?.outputProperties = outputProperties
            extracted?.transformedProperties = outputProperties.mapValues { it.value.toString() }
            extracted?.extractedData = extractedData
            Log.d(TAG, "replaceDataWithNfcData: merged ${outputProperties.size} properties")
        } catch (e: Exception) {
            Log.e(TAG, "replaceDataWithNfcData: failed to merge NFC data — ${describe(e)}", e)
            scanNfcCallback.onErrorNfcScan(model, e.message ?: "Failed to merge NFC data")
            return
        }

        if (languageCode == Language.NON) {
            Log.d(TAG, "replaceDataWithNfcData: no translation requested, completing")
            scanNfcCallback.onCompleteNfcScan(model)
            return
        }

        if (apiKey.isEmpty()) {
            Log.w(TAG, "replaceDataWithNfcData: translation to '$languageCode' requested but apiKey is empty — skipping")
            scanNfcCallback.onCompleteNfcScan(model)
            return
        }

        try {
            val props = model.passportExtractedModel?.outputProperties
            if (props == null) {
                Log.e(TAG, "replaceDataWithNfcData: outputProperties null before translation — skipping")
                scanNfcCallback.onCompleteNfcScan(model)
                return
            }
            Log.d(TAG, "replaceDataWithNfcData: requesting translation to '$languageCode'")
            val translated = LanguageTransformation(apiKey)
            translated.setCallback(this)
            translated.languageTransformation(languageCode, preparePropertiesToTranslate(languageCode, props))
        } catch (e: Exception) {
            Log.e(TAG, "replaceDataWithNfcData: failed to start translation — ${describe(e)}", e)
            scanNfcCallback.onCompleteNfcScan(model)
        }
    }

    /** Language Transformation **/
    private var nameKey: String = ""
    private var nameWordCount: Int = 0
    private var surnameKey: String = ""

    override fun onTranslatedSuccess(properties: Map<String, String>?) {
        val model = passportResponseModel
        if (model == null) {
            Log.e(TAG, "onTranslatedSuccess: passportResponseModel is null — aborting")
            return
        }
        if (properties == null) {
            Log.w(TAG, "onTranslatedSuccess: translated properties are null — keeping original values")
        }

        try {
            properties?.let { props ->
                Log.d(TAG, "onTranslatedSuccess: received ${props.size} translated properties")

                model.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
                    when {
                        key.contains(IdentificationDocumentCaptureKeys.name) -> {
                            nameKey = key
                            nameWordCount = if (value.toString().trim().isEmpty()) 0
                            else value.toString().trim().split("\\s+".toRegex()).size
                        }
                        key.contains(IdentificationDocumentCaptureKeys.surname) -> {
                            surnameKey = key
                        }
                    }
                }

                model.passportExtractedModel?.transformedProperties = mutableMapOf()
                model.passportExtractedModel?.extractedData = mutableMapOf()

                val tempTransformedProperties = mutableMapOf<String, String>()
                val tempExtractedData = mutableMapOf<String, Any>()

                props.forEach { (key, value) ->
                    when (key) {
                        FullNameKey -> {
                            if (nameKey.isEmpty() && surnameKey.isEmpty()) {
                                Log.w(TAG, "onTranslatedSuccess: got full name translation but no name/surname keys exist")
                            }
                            if (nameKey.isNotEmpty()) {
                                tempTransformedProperties[nameKey] = getSelectedWords(value, nameWordCount)
                                tempExtractedData["name"] = getSelectedWords(value, nameWordCount)
                            }
                            if (surnameKey.isNotEmpty()) {
                                tempTransformedProperties[surnameKey] = getRemainingWords(value, nameWordCount)
                                tempExtractedData["surname"] = getRemainingWords(value, nameWordCount)
                            }
                        }
                        else -> {
                            tempTransformedProperties[key] = value
                            val newKey = key.substringAfter("IdentificationDocumentCapture_").replace("_", " ")
                            tempExtractedData[newKey] = value
                        }
                    }
                }

                val outputProps = model.passportExtractedModel?.outputProperties
                if (outputProps == null) {
                    Log.e(TAG, "onTranslatedSuccess: outputProperties is null — ignored properties not restored")
                } else {
                    getIgnoredProperties(outputProps).forEach { (key, value) ->
                        tempTransformedProperties[key] = value
                        val newKey = key.substringAfter("IdentificationDocumentCapture_").replace("_", " ")
                        tempExtractedData[newKey] = value
                    }
                }

                model.passportExtractedModel?.transformedProperties = tempTransformedProperties
                model.passportExtractedModel?.extractedData = tempExtractedData
            }
        } catch (e: Exception) {
            Log.e(TAG, "onTranslatedSuccess: failed applying translation — ${describe(e)}", e)
        }

        scanNfcCallback.onCompleteNfcScan(model)
    }

    override fun onTranslatedError(properties: Map<String, String>?) {
        Log.e(TAG, "onTranslatedError: translation to '$languageCode' failed, properties=${properties?.keys}")
        val model = passportResponseModel
        if (model == null) {
            Log.e(TAG, "onTranslatedError: passportResponseModel is null — cannot complete")
            return
        }
        scanNfcCallback.onCompleteNfcScan(model)
    }
}