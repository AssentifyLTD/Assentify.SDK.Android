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
import com.assentify.sdk.ScanPassport.PassportResponseModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.sf.scuba.smartcards.CardService
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
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.InputStream

class ScanNfc(
    private val scanNfcCallback: ScanNfcCallback,
    private val languageCode : String,
    private val apiKey:String,
) : LanguageTransformationCallback {


    private var passportResponseModel: PassportResponseModel? = null;
    private var finalBitmap: Bitmap? = null

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
                val bacKey: BACKeySpec = BACKey("LR2285833", "940208", "310927")
                ReadTask(IsoDep.get(tag), bacKey).start()
                scanNfcCallback.onStartNfcScan();

            }
        }
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
                        scanNfcCallback.onErrorNfcScan(passportResponseModel!!,e.message!!);
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
                    scanNfcCallback.onErrorNfcScan(passportResponseModel!!,e.message!!);
                }
            }
        }

        private fun onPostExecute(exception: Exception?) {
            if (exception == null) {
                if (chipAuthSucceeded) {
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
                        finalBitmap = NfcImageUtil.decodeImage(faceImageInfo.mimeType, inputStream)
                    }

                    replaceDataWithNfcData(mrzInfo);
                } else {
                    scanNfcCallback.onErrorNfcScan(passportResponseModel!!,"Chip Auth Not Succeeded ");
                }
            } else {
                scanNfcCallback.onErrorNfcScan(passportResponseModel!!,exception.message!!);
            }
        }
    }

    /** Replace Data With Nfc Data **/
    private fun replaceDataWithNfcData(mRZInfo: MRZInfo) {
        val outputProperties = HashMap<String, Any>()
        passportResponseModel!!.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
            when {
                key.contains(IdentificationDocumentCaptureKeys.name) -> {
                    outputProperties[key] = mRZInfo.secondaryIdentifier
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.name = mRZInfo.secondaryIdentifier
                }
                key.contains(IdentificationDocumentCaptureKeys.surname) -> {
                    outputProperties[key] = mRZInfo.primaryIdentifier
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.surname = mRZInfo.primaryIdentifier
                }
                key.contains(IdentificationDocumentCaptureKeys.nationality) -> {
                    outputProperties[key] = mRZInfo.nationality
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.nationality = mRZInfo.nationality
                }
                key.contains(IdentificationDocumentCaptureKeys.documentNumber) -> {
                    outputProperties[key] = mRZInfo.documentNumber
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.documentNumber = mRZInfo.documentNumber
                }
                key.contains(IdentificationDocumentCaptureKeys.sex) -> {
                    outputProperties[key] = mRZInfo.gender.name
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.sex = mRZInfo.gender.name
                }
                /**Date**/
                /* key.contains(IdentificationDocumentCaptureKeys.birthDate) -> {
                    outputProperties[key] = mRZInfo.dateOfBirth
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.birthDate = mRZInfo.dateOfBirth
                }
                key.contains(IdentificationDocumentCaptureKeys.expiryDate) -> {
                    outputProperties[key] = mRZInfo.dateOfExpiry
                    passportResponseModel!!.passportExtractedModel?.identificationDocumentCapture?.expiryDate = mRZInfo.dateOfExpiry
                }*/
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
            scanNfcCallback.onCompleteNfcScan(passportResponseModel!!,finalBitmap!!)
        } else {
            if(apiKey.isNotEmpty()){
                val translated = LanguageTransformation(apiKey);
                translated.setCallback(this);
                translated.languageTransformation(languageCode,
                    preparePropertiesToTranslate(languageCode, passportResponseModel!!.passportExtractedModel?.outputProperties!!))
            }else{
                scanNfcCallback.onCompleteNfcScan(passportResponseModel!!,finalBitmap!!)
            }
        }


    }


    /** Language Transformation **/
    var nameKey: String = ""
    var nameWordCount: Int = 0
    var surnameKey: String = ""
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

        scanNfcCallback.onCompleteNfcScan(passportResponseModel!!,finalBitmap!!)
    }

    override fun onTranslatedError(properties: Map<String, String>?) {
        scanNfcCallback.onCompleteNfcScan(passportResponseModel!!,finalBitmap!!)
    }


}