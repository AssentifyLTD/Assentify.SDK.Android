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
import org.jmrtd.lds.icao.COMFile
import org.jmrtd.lds.icao.DG11File
import org.jmrtd.lds.icao.DG12File
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
import java.lang.ref.WeakReference
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScanNfc(
    private val scanNfcCallback: ScanNfcCallback,
    private val languageCode : String,
    private val apiKey:String,
    private val context:Context,
    private val appConfiguration: ConfigModel,
) : LanguageTransformationCallback {

    companion object {
        private const val NAME_SEPARATOR = "#"

        // Arabic letters, incl. presentation forms
        private val ARABIC_REGEX = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")

        // ICAO tags as listed in EF.COM (also the outer tag of each file)
        private const val DG11_TAG = 0x6B
        private const val DG12_TAG = 0x6C
        private const val DG13_TAG = 0x6D

        // TLV structure tags
        private const val TAG_LIST = 0x5C       // list of tags present, ignored when extracting
        private const val TEMPLATE_TAG = 0xA0   // wraps repeated names (DG11 other names, DG12 other persons)
        private const val COUNT_TAG = 0x02      // count inside the A0 template

        // DG11 field tags (ICAO) — used only by the raw fallback
        private const val DG11_NAME_OF_HOLDER = 0x5F0E
        private const val DG11_OTHER_NAME = 0x5F0F
        private const val DG11_PERSONAL_NUMBER = 0x5F10
        private const val DG11_PLACE_OF_BIRTH = 0x5F11
        private const val DG11_TELEPHONE = 0x5F12
        private const val DG11_PROFESSION = 0x5F13
        private const val DG11_TITLE = 0x5F14
        private const val DG11_PERSONAL_SUMMARY = 0x5F15
        private const val DG11_OTHER_TD_NUMBERS = 0x5F17
        private const val DG11_CUSTODY = 0x5F18
        private const val DG11_FULL_DATE_OF_BIRTH = 0x5F2B
        private const val DG11_PERMANENT_ADDRESS = 0x5F42

        // DG12 field tags (ICAO) — used only by the raw fallback
        private const val DG12_ISSUING_AUTHORITY = 0x5F19
        private const val DG12_OTHER_PERSON = 0x5F1A
        private const val DG12_ENDORSEMENTS = 0x5F1B
        private const val DG12_TAX_EXIT = 0x5F1C
        private const val DG12_DATE_OF_ISSUE = 0x5F26
        private const val DG12_PERSONALIZATION_TIME = 0x5F55
        private const val DG12_PERSONALIZATION_SERIAL = 0x5F56

        // DG13 is issuer-defined: this table is ONLY valid for Lebanese passports
        private const val LEBANON = "LBN"
        private const val LB_GIVEN_NAMES = 0x9F1A
        private const val LB_SURNAME = 0x9F1B
        private const val LB_GIVEN_NAMES_AR = 0x9F0E
        private const val LB_SURNAME_AR = 0x9F0F
        private const val LB_FATHER = 0x9F2D
        private const val LB_FATHER_AR = 0x9F1D
        private const val LB_MOTHER = 0x9F2E
        private const val LB_MOTHER_AR = 0x9F1E
        private const val LB_MOTHER_FAMILY = 0x9F34
        private const val LB_MOTHER_FAMILY_AR = 0x9F33
        private const val LB_MOTHER_FULL = 0x9F36
        private const val LB_MOTHER_FULL_AR = 0x9F35
        private const val LB_PLACE_OF_BIRTH_AR = 0x9F11
        private const val LB_NATIONALITY_AR = 0x9F12
        private const val LB_SEX_AR = 0x9F13
        private const val LB_RECORD_ID = 0x9F32
    }

    /** Everything readable from DG11 (all nullable: chips fill only what the issuer chose) */
    private data class NfcDg11Data(
        val nameOfHolder: String? = null,
        val otherNames: String? = null,          // only set when the parent split is not possible
        val fatherName: String? = null,
        val fatherNameArabic: String? = null,
        val motherName: String? = null,
        val motherNameArabic: String? = null,
        val personalNumber: String? = null,
        val fullDateOfBirth: String? = null,
        val placeOfBirth: String? = null,
        val placeOfBirthArabic: String? = null,
        val permanentAddress: String? = null,
        val telephone: String? = null,
        val profession: String? = null,
        val title: String? = null,
        val personalSummary: String? = null,
        val otherValidTDNumbers: String? = null,
        val custodyInformation: String? = null,
    )

    /** Everything text-based readable from DG12 */
    private data class NfcDg12Data(
        val issuingAuthority: String? = null,
        val dateOfIssue: String? = null,
        val namesOfOtherPersons: String? = null,
        val endorsementsAndObservations: String? = null,
        val taxOrExitRequirements: String? = null,
        val dateAndTimeOfPersonalization: String? = null,
        val personalizationSystemSerialNumber: String? = null,
    )

    /**
     * DG13 (issuer-defined). Named fields are filled only for Lebanese passports.
     * For any other issuer every non-empty tag goes to [unrecognized] as "TAG=value; ...".
     */
    private data class NfcDg13Data(
        val givenNames: String? = null,
        val surname: String? = null,
        val givenNamesArabic: String? = null,
        val surnameArabic: String? = null,
        val fatherName: String? = null,
        val fatherNameArabic: String? = null,
        val motherName: String? = null,
        val motherNameArabic: String? = null,
        val motherFamilyName: String? = null,
        val motherFamilyNameArabic: String? = null,
        val motherFullName: String? = null,
        val motherFullNameArabic: String? = null,
        val placeOfBirthArabic: String? = null,
        val nationalityArabic: String? = null,
        val sexArabic: String? = null,
        val recordId: String? = null,
        val unrecognized: String? = null,
    )

    private var passportResponseModel: PassportResponseModel? = null;

    // Activity for the debug popup (weak reference, so it is never leaked)
    private var lastActivity: WeakReference<Activity>? = null

    // null when the DG is missing, not readable, or failed to parse
    private var nfcDg11: NfcDg11Data? = null
    private var nfcDg12: NfcDg12Data? = null
    private var nfcDg13: NfcDg13Data? = null

    /** isNfcSupported **/
    fun isNfcSupported(activity: Activity): Boolean {
        lastActivity = WeakReference(activity)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        return nfcAdapter != null
    }

    /** isNfcEnabled **/
    fun isNfcEnabled(activity: Activity): Boolean {
        lastActivity = WeakReference(activity)
        val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        return nfcAdapter?.isEnabled == true
    }


    /** onActivityNewIntent **/
    fun onActivityNewIntent(intent: Intent, dataModel: PassportResponseModel) {
        passportResponseModel = dataModel;
        nfcDg11 = null
        nfcDg12 = null
        nfcDg13 = null
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

                // Mandatory groups: a failure here is a real scan failure
                val dg1In = service.getInputStream(PassportService.EF_DG1)
                dg1File = DG1File(dg1In)
                val dg2In = service.getInputStream(PassportService.EF_DG2)
                dg2File = DG2File(dg2In)

                // Optional groups: never fail the scan because of them
                val presentTags = readPresentTags(service)
                nfcDg11 = if (isPresent(presentTags, DG11_TAG)) readDg11(service) else null
                nfcDg12 = if (isPresent(presentTags, DG12_TAG)) readDg12(service) else null

                // DG13 meaning depends on the issuer, so pass the issuing state from DG1
                val issuingState = safeRead { cleanNfcText(dg1File.mrzInfo.issuingState) }
                nfcDg13 = if (isPresent(presentTags, DG13_TAG)) readDg13(service, issuingState) else null

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
                    } else {
                        // No face on the chip: still complete with the chip text data
                        replaceDataWithNfcData(mrzInfo)
                    }

                } catch (e:Exception) {
                    scanNfcCallback.onErrorNfcScan(passportResponseModel!!,"Chip Auth Not Succeeded");
                }
            } else {
                scanNfcCallback.onErrorNfcScan(passportResponseModel!!, exception.message ?: "NFC read failed");
            }
        }
    }

    /** DG11 / DG12 / DG13 helpers — none of these ever throw **/

    /** Tags listed in EF.COM, or null if EF.COM can't be read (then we just try every DG) */
    private fun readPresentTags(service: PassportService): Set<Int>? = safeRead {
        COMFile(service.getInputStream(PassportService.EF_COM)).tagList?.toSet()
    }

    private fun isPresent(presentTags: Set<Int>?, tag: Int): Boolean =
        presentTags == null || presentTags.contains(tag)

    /** Whole file as bytes, or null if missing / not readable */
    private fun readRaw(service: PassportService, fileId: Short): ByteArray? = safeRead {
        IOUtils.toByteArray(service.getInputStream(fileId))
    }?.takeIf { it.isNotEmpty() }

    private fun readDg11(service: PassportService): NfcDg11Data? {
        val raw = readRaw(service, PassportService.EF_DG11) ?: return null

        // JMRTD first; if it can't parse this chip's DG11, decode the raw TLV ourselves
        val dg11 = safeRead { DG11File(ByteArrayInputStream(raw)) }
            ?: return safeRead { decodeDg11Raw(raw) }

        val rawOtherNames = safeRead { dg11.otherNames }
        val rawPlaceOfBirth = safeRead { dg11.placeOfBirth }

        // Split if the format is recognized, otherwise keep the whole value on one key.
        // If the split itself throws, we still fall back to the whole value.
        val parents = safeRead { splitOtherNames(rawOtherNames) }
            ?: ParentNames(otherNames = safeRead { cleanNfcList(rawOtherNames) })
        val placeOfBirth = safeRead { splitPlaceOfBirth(rawPlaceOfBirth) }
            ?: LatinArabic(main = safeRead { cleanNfcList(rawPlaceOfBirth) })

        return NfcDg11Data(
            nameOfHolder = safeRead { cleanNfcText(dg11.nameOfHolder) },
            otherNames = parents.otherNames,
            fatherName = parents.fatherName,
            fatherNameArabic = parents.fatherNameArabic,
            motherName = parents.motherName,
            motherNameArabic = parents.motherNameArabic,
            personalNumber = safeRead { cleanNfcText(dg11.personalNumber) },
            fullDateOfBirth = safeRead { formatNfcDate(dg11.fullDateOfBirth) },
            placeOfBirth = placeOfBirth.main,
            placeOfBirthArabic = placeOfBirth.arabic,
            permanentAddress = safeRead { cleanNfcList(dg11.permanentAddress) },
            telephone = safeRead { cleanNfcText(dg11.telephone) },
            profession = safeRead { cleanNfcText(dg11.profession) },
            title = safeRead { cleanNfcText(dg11.title) },
            personalSummary = safeRead { cleanNfcText(dg11.personalSummary) },
            otherValidTDNumbers = safeRead { cleanNfcList(dg11.otherValidTDNumbers) },
            custodyInformation = safeRead { cleanNfcText(dg11.custodyInformation) },
        )
    }

    private fun readDg12(service: PassportService): NfcDg12Data? {
        val raw = readRaw(service, PassportService.EF_DG12) ?: return null

        // JMRTD first; if it can't parse this chip's DG12, decode the raw TLV ourselves
        val dg12 = safeRead { DG12File(ByteArrayInputStream(raw)) }
            ?: return safeRead { decodeDg12Raw(raw) }

        return NfcDg12Data(
            issuingAuthority = safeRead { cleanNfcText(dg12.issuingAuthority) },
            dateOfIssue = safeRead { formatNfcDate(dg12.dateOfIssue) },
            namesOfOtherPersons = safeRead { cleanNfcList(dg12.namesOfOtherPersons) },
            endorsementsAndObservations = safeRead { cleanNfcText(dg12.endorsementsAndObservations) },
            taxOrExitRequirements = safeRead { cleanNfcText(dg12.taxOrExitRequirements) },
            dateAndTimeOfPersonalization = safeRead { formatNfcDate(dg12.dateAndTimeOfPersonalization) },
            personalizationSystemSerialNumber = safeRead { cleanNfcText(dg12.personalizationSystemSerialNumber) },
        )
    }

    /** DG13: no library parser exists, so we always decode the raw TLV */
    private fun readDg13(service: PassportService, issuingState: String?): NfcDg13Data? {
        val raw = readRaw(service, PassportService.EF_DG13) ?: return null
        return safeRead { decodeDg13(raw, issuingState) }
    }

    /** ---------- Raw TLV decoding (BER-TLV, as described in ICAO 9303) ---------- **/

    private class Tlv(val tag: Int, val value: ByteArray)

    /**
     * Parses a sequence of TLVs. Tags are 1+ bytes, lengths are BER (short, 81 nn, 82 nn nn).
     * Stops (without throwing) at the first truncated or invalid entry.
     */
    private fun parseTlvs(bytes: ByteArray): List<Tlv> {
        val out = mutableListOf<Tlv>()
        var i = 0
        while (i < bytes.size) {
            var tag = bytes[i++].toInt() and 0xFF
            if (tag == 0x00 || tag == 0xFF) continue // padding

            if ((tag and 0x1F) == 0x1F) {
                // multi-byte tag: continue while the high bit of the next byte is set
                while (true) {
                    if (i >= bytes.size) return out
                    val next = bytes[i++].toInt() and 0xFF
                    tag = (tag shl 8) or next
                    if ((next and 0x80) == 0) break
                }
            }

            if (i >= bytes.size) break
            var length = bytes[i++].toInt() and 0xFF
            if (length >= 0x80) {
                val count = length and 0x7F
                if (count == 0 || count > 3 || i + count > bytes.size) break
                length = 0
                repeat(count) { length = (length shl 8) or (bytes[i++].toInt() and 0xFF) }
            }

            if (length < 0 || i + length > bytes.size) break
            out += Tlv(tag, bytes.copyOfRange(i, i + length))
            i += length
        }
        return out
    }

    /**
     * Fields inside the file's outer tag. The 5C tag list is skipped.
     * A0 templates (repeated names) are flattened, without their count.
     */
    private fun fileFields(raw: ByteArray, outerTag: Int): List<Tlv> {
        val outer = parseTlvs(raw).firstOrNull { it.tag == outerTag } ?: return emptyList()
        val fields = mutableListOf<Tlv>()
        parseTlvs(outer.value).forEach { field ->
            when (field.tag) {
                TAG_LIST -> Unit
                TEMPLATE_TAG -> fields += parseTlvs(field.value).filter { it.tag != COUNT_TAG }
                else -> fields += field
            }
        }
        return fields
    }

    /** UTF-8 text (never ASCII/Latin-1, which breaks Arabic). Not valid text → hex. Empty → null. */
    private fun decodeText(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val text = try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(bytes))
                .toString()
        } catch (e: Exception) {
            return toHex(bytes)
        }
        return cleanNfcText(text)
    }

    /** Dates are ASCII digits or BCD: ASCII if every byte is '0'..'9', otherwise hex-encode (BCD) */
    private fun decodeDate(bytes: ByteArray): String? {
        if (bytes.isEmpty()) return null
        val digits = if (bytes.all { it in 0x30..0x39 }) String(bytes, Charsets.US_ASCII) else toHex(bytes)
        return formatNfcDate(digits)
    }

    private fun toHex(bytes: ByteArray): String = bytes.joinToString("") { "%02X".format(it) }

    /** DG11 straight from the raw bytes — only used when JMRTD fails on this chip */
    private fun decodeDg11Raw(raw: ByteArray): NfcDg11Data? {
        val fields = fileFields(raw, DG11_TAG)
        if (fields.isEmpty()) return null

        fun text(tag: Int) = fields.firstOrNull { it.tag == tag }?.let { decodeText(it.value) }
        fun texts(tag: Int) = fields.filter { it.tag == tag }.mapNotNull { decodeText(it.value) }

        val otherNames = texts(DG11_OTHER_NAME)
        val parents = safeRead { splitOtherNames(otherNames) }
            ?: ParentNames(otherNames = cleanNfcList(otherNames))
        val placeOfBirth = safeRead { splitPlaceOfBirth(listOfNotNull(text(DG11_PLACE_OF_BIRTH))) }
            ?: LatinArabic(main = text(DG11_PLACE_OF_BIRTH))

        return NfcDg11Data(
            nameOfHolder = text(DG11_NAME_OF_HOLDER),
            otherNames = parents.otherNames,
            fatherName = parents.fatherName,
            fatherNameArabic = parents.fatherNameArabic,
            motherName = parents.motherName,
            motherNameArabic = parents.motherNameArabic,
            personalNumber = text(DG11_PERSONAL_NUMBER),
            fullDateOfBirth = fields.firstOrNull { it.tag == DG11_FULL_DATE_OF_BIRTH }?.let { decodeDate(it.value) },
            placeOfBirth = placeOfBirth.main,
            placeOfBirthArabic = placeOfBirth.arabic,
            permanentAddress = text(DG11_PERMANENT_ADDRESS),
            telephone = text(DG11_TELEPHONE),
            profession = text(DG11_PROFESSION),
            title = text(DG11_TITLE),
            personalSummary = text(DG11_PERSONAL_SUMMARY),
            otherValidTDNumbers = text(DG11_OTHER_TD_NUMBERS),
            custodyInformation = text(DG11_CUSTODY),
        )
    }

    /** DG12 straight from the raw bytes — only used when JMRTD fails on this chip */
    private fun decodeDg12Raw(raw: ByteArray): NfcDg12Data? {
        val fields = fileFields(raw, DG12_TAG)
        if (fields.isEmpty()) return null

        fun text(tag: Int) = fields.firstOrNull { it.tag == tag }?.let { decodeText(it.value) }
        fun date(tag: Int) = fields.firstOrNull { it.tag == tag }?.let { decodeDate(it.value) }

        return NfcDg12Data(
            issuingAuthority = text(DG12_ISSUING_AUTHORITY),
            dateOfIssue = date(DG12_DATE_OF_ISSUE),
            namesOfOtherPersons = cleanNfcList(fields.filter { it.tag == DG12_OTHER_PERSON }.mapNotNull { decodeText(it.value) }),
            endorsementsAndObservations = text(DG12_ENDORSEMENTS),
            taxOrExitRequirements = text(DG12_TAX_EXIT),
            dateAndTimeOfPersonalization = date(DG12_PERSONALIZATION_TIME),
            personalizationSystemSerialNumber = text(DG12_PERSONALIZATION_SERIAL),
        )
    }

    /**
     * DG13. The tag table is applied ONLY when DG1's issuing state is LBN.
     * For other issuers (or unknown tags) non-empty values are kept raw in `unrecognized`.
     */
    private fun decodeDg13(raw: ByteArray, issuingState: String?): NfcDg13Data? {
        val values = linkedMapOf<Int, String>()
        fileFields(raw, DG13_TAG).forEach { field ->
            val text = decodeText(field.value) ?: return@forEach   // skip empty fields
            if (!values.containsKey(field.tag)) values[field.tag] = text
        }
        if (values.isEmpty()) return null

        if (issuingState != LEBANON) {
            return NfcDg13Data(unrecognized = formatUnrecognized(values))
        }

        fun take(tag: Int): String? = values.remove(tag)

        val givenNames = take(LB_GIVEN_NAMES)
        val surname = take(LB_SURNAME)
        val givenNamesArabic = take(LB_GIVEN_NAMES_AR)
        val surnameArabic = take(LB_SURNAME_AR)
        val fatherName = take(LB_FATHER)
        val fatherNameArabic = take(LB_FATHER_AR)
        val motherName = take(LB_MOTHER)
        val motherNameArabic = take(LB_MOTHER_AR)
        val motherFamilyName = take(LB_MOTHER_FAMILY)
        val motherFamilyNameArabic = take(LB_MOTHER_FAMILY_AR)
        val motherFullName = take(LB_MOTHER_FULL)
        val motherFullNameArabic = take(LB_MOTHER_FULL_AR)
        val placeOfBirthArabic = take(LB_PLACE_OF_BIRTH_AR)
        val nationalityArabic = take(LB_NATIONALITY_AR)
        val sexArabic = take(LB_SEX_AR)
        val recordId = take(LB_RECORD_ID)

        return NfcDg13Data(
            givenNames = givenNames,
            surname = surname,
            givenNamesArabic = givenNamesArabic,
            surnameArabic = surnameArabic,
            fatherName = fatherName,
            fatherNameArabic = fatherNameArabic,
            motherName = motherName,
            motherNameArabic = motherNameArabic,
            motherFamilyName = motherFamilyName,
            motherFamilyNameArabic = motherFamilyNameArabic,
            motherFullName = motherFullName,
            motherFullNameArabic = motherFullNameArabic,
            placeOfBirthArabic = placeOfBirthArabic,
            nationalityArabic = nationalityArabic,
            sexArabic = sexArabic,
            recordId = recordId,
            unrecognized = formatUnrecognized(values),   // whatever is left, e.g. "9F14=-"
        )
    }

    private fun formatUnrecognized(values: Map<Int, String>): String? =
        values.entries.joinToString("; ") { "%X=%s".format(it.key, it.value) }.takeIf { it.isNotEmpty() }

    /** ---------- DG11 split helpers ---------- **/

    private data class ParentNames(
        val fatherName: String? = null,
        val fatherNameArabic: String? = null,
        val motherName: String? = null,
        val motherNameArabic: String? = null,
        val otherNames: String? = null,   // whole value, only when the split is not possible
    )

    private data class LatinArabic(
        val main: String? = null,         // Latin part, or the whole value when not split
        val arabic: String? = null,
    )

    /**
     * Other names.
     * Split ONLY when the value is exactly 4 '#'-separated parts forming two Latin/Arabic pairs:
     *   "ZAHR#زاهر#RANIA#رانيا" → father ZAHR / زاهر, mother RANIA / رانيا
     *   (Latin/Arabic order inside each pair doesn't matter; an empty part is allowed)
     * Anything else → nothing is guessed, the whole value goes to otherNames.
     */
    private fun splitOtherNames(values: List<String>?): ParentNames {
        val entries = values?.mapNotNull { cleanNfcText(it) }.orEmpty()
        if (entries.isEmpty()) return ParentNames()

        val parts = entries.joinToString(NAME_SEPARATOR)
            .split(NAME_SEPARATOR)
            .map { cleanNfcText(it) ?: "" }
        if (parts.all { it.isEmpty() }) return ParentNames()

        if (parts.size == 4) {
            val father = pickLatinArabic(parts[0], parts[1])
            val mother = pickLatinArabic(parts[2], parts[3])
            if (father != null && mother != null) {
                return ParentNames(
                    fatherName = father.main,
                    fatherNameArabic = father.arabic,
                    motherName = mother.main,
                    motherNameArabic = mother.arabic,
                )
            }
        }

        // Unknown format: keep it on one key, exactly as read
        return ParentNames(otherNames = entries.joinToString(", "))
    }

    /**
     * Place of birth.
     * Split ONLY when the value is 2 '#'-separated parts forming a Latin/Arabic pair:
     *   "BEIRUT#بيروت" → BEIRUT / بيروت
     * Anything else (no '#', ICAO "PARIS<FRANCE", 3+ parts, ...) → whole value on placeOfBirth.
     */
    private fun splitPlaceOfBirth(values: List<String>?): LatinArabic {
        // JMRTD already split on '<', glue it back first
        val whole = cleanNfcText(values?.joinToString(" ")) ?: return LatinArabic()

        val parts = whole.split(NAME_SEPARATOR).map { cleanNfcText(it) ?: "" }
        if (parts.all { it.isEmpty() }) return LatinArabic()

        if (parts.size == 2) {
            val pair = pickLatinArabic(parts[0], parts[1])
            if (pair != null && (pair.main != null || pair.arabic != null)) return pair
        }

        // Unknown format: keep it on one key
        return LatinArabic(main = whole)
    }

    /**
     * Two values are a valid pair when there is at most one Latin and at most one Arabic value.
     * Returns null when both are Latin or both are Arabic (that's not a Latin/Arabic pair).
     */
    private fun pickLatinArabic(a: String, b: String): LatinArabic? {
        val values = listOf(a, b).filter { it.isNotEmpty() }
        val arabic = values.filter { isArabic(it) }
        val latin = values.filterNot { isArabic(it) }
        if (arabic.size > 1 || latin.size > 1) return null
        return LatinArabic(main = latin.firstOrNull(), arabic = arabic.firstOrNull())
    }

    private fun isArabic(value: String): Boolean = ARABIC_REGEX.containsMatchIn(value)

    private inline fun <T> safeRead(block: () -> T?): T? = try {
        block()
    } catch (e: Exception) {
        null
    }

    /** "<" → space, collapse spaces, trim; empty → null */
    private fun cleanNfcText(value: String?): String? = value
        ?.replace("<", " ")
        ?.replace(Regex("\\s+"), " ")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

    private fun cleanNfcList(values: List<String>?): String? =
        values?.mapNotNull { cleanNfcText(it) }?.takeIf { it.isNotEmpty() }?.joinToString(", ")

    /** "NADA" + "KHOURY" → "NADA KHOURY"; both null → null */
    private fun joinNames(first: String?, second: String?): String? =
        listOfNotNull(first, second).joinToString(" ").takeIf { it.isNotEmpty() }

    /**
     * yyyyMMdd → dd/MM/yyyy, yyyyMMddHHmmss → dd/MM/yyyy HH:mm:ss.
     * Anything else (e.g. partial dates) is returned cleaned but unchanged.
     */
    private fun formatNfcDate(raw: String?): String? {
        val v = cleanNfcText(raw) ?: return null
        if (!v.all { it.isDigit() }) return v
        return when (v.length) {
            8 -> "${v.substring(6, 8)}/${v.substring(4, 6)}/${v.substring(0, 4)}"
            14 -> "${v.substring(6, 8)}/${v.substring(4, 6)}/${v.substring(0, 4)} " +
                    "${v.substring(8, 10)}:${v.substring(10, 12)}:${v.substring(12, 14)}"
            else -> v
        }
    }


    /** Upload Image **/
    private fun uploadImage(
        bitmap:Bitmap,
        mrzInfo: MRZInfo,
    ) {
        val (image, fileName) =  createTimestampedTempFile(bitmap)!!;
        val fileRequestBody = image.asRequestBody(null)
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
        val dg11 = nfcDg11
        val dg12 = nfcDg12
        val dg13 = nfcDg13

        // Best source first: DG13 (Lebanese issuer data) → DG11 → OCR value (via "?: value" below)
        val fatherName = dg13?.fatherName ?: dg11?.fatherName
        val fatherNameArabic = dg13?.fatherNameArabic ?: dg11?.fatherNameArabic
        val motherName = dg13?.motherFullName
            ?: joinNames(dg13?.motherName, dg13?.motherFamilyName)
            ?: dg11?.motherName
        val motherNameArabic = dg13?.motherFullNameArabic
            ?: joinNames(dg13?.motherNameArabic, dg13?.motherFamilyNameArabic)
            ?: dg11?.motherNameArabic
        val placeOfBirth = dg11?.placeOfBirth                              // Latin only exists in DG11
        val placeOfBirthArabic = dg13?.placeOfBirthArabic ?: dg11?.placeOfBirthArabic

        val outputProperties = HashMap<String, Any>()
        passportResponseModel!!.passportExtractedModel?.outputProperties?.forEach { (key, value) ->
            when {
                // ---------- Arabic / DG13 keys FIRST (their keys contain the base key names) ----------
                key.contains(IdentificationDocumentCaptureKeys.idFathersNameArabic) -> {
                    outputProperties[key] = fatherNameArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idMothersNameArabic) -> {
                    outputProperties[key] = motherNameArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfBirthArabic) -> {
                    outputProperties[key] = placeOfBirthArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idNameArabic) -> {
                    outputProperties[key] = dg13?.givenNamesArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idSurnameArabic) -> {
                    outputProperties[key] = dg13?.surnameArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idNationalityArabic) -> {
                    outputProperties[key] = dg13?.nationalityArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idSexArabic) -> {
                    outputProperties[key] = dg13?.sexArabic ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idRecordId) -> {
                    outputProperties[key] = dg13?.recordId ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idDg13Extra) -> {
                    outputProperties[key] = dg13?.unrecognized ?: value
                }

                // ---------- DG11 (keep these BEFORE name/surname: contains() is order-sensitive) ----------
                key.contains(IdentificationDocumentCaptureKeys.idOtherNames) -> {
                    outputProperties[key] = dg11?.otherNames ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idMothersName) -> {
                    outputProperties[key] = motherName ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idFathersName) -> {
                    outputProperties[key] = fatherName ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPersonalNumber) -> {
                    outputProperties[key] = dg11?.personalNumber ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idFullDateOfBirth) -> {
                    outputProperties[key] = dg11?.fullDateOfBirth ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPlaceOfBirth) -> {
                    outputProperties[key] = placeOfBirth ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPermanentAddress) -> {
                    outputProperties[key] = dg11?.permanentAddress ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idTelephone) -> {
                    outputProperties[key] = dg11?.telephone ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idProfession) -> {
                    outputProperties[key] = dg11?.profession ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idTitle) -> {
                    outputProperties[key] = dg11?.title ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPersonalSummary) -> {
                    outputProperties[key] = dg11?.personalSummary ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idOtherValidTDNumbers) -> {
                    outputProperties[key] = dg11?.otherValidTDNumbers ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idCustodyInformation) -> {
                    outputProperties[key] = dg11?.custodyInformation ?: value
                }

                // ---------- DG12 ----------
                key.contains(IdentificationDocumentCaptureKeys.idIssuingAuthority) -> {
                    outputProperties[key] = dg12?.issuingAuthority ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idDateOfIssue) -> {
                    outputProperties[key] = dg12?.dateOfIssue ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idNamesOfOtherPersons) -> {
                    outputProperties[key] = dg12?.namesOfOtherPersons ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idEndorsementsAndObservations) -> {
                    outputProperties[key] = dg12?.endorsementsAndObservations ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idTaxOrExitRequirements) -> {
                    outputProperties[key] = dg12?.taxOrExitRequirements ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idDateOfPersonalization) -> {
                    outputProperties[key] = dg12?.dateAndTimeOfPersonalization ?: value
                }
                key.contains(IdentificationDocumentCaptureKeys.idPersonalizationSystemSerialNumber) -> {
                    outputProperties[key] = dg12?.personalizationSystemSerialNumber ?: value
                }

                // ---------- DG1 (MRZ) ----------
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
            completeScan()
        } else {
            if(apiKey.isNotEmpty()){
                val translated = LanguageTransformation(apiKey);
                translated.setCallback(this);
                translated.languageTransformation(languageCode,
                    preparePropertiesToTranslate(languageCode, passportResponseModel!!.passportExtractedModel?.outputProperties!!))
            }else{
                completeScan()
            }
        }


    }

    /** Every successful scan ends here: show the debug toast, then notify the caller */
    private fun completeScan() {
        scanNfcCallback.onCompleteNfcScan(passportResponseModel!!)
    }

    /** Every extractedData entry that has a value, one per line; null when nothing to show */
    private fun extractedDataText(): String? = try {
        passportResponseModel?.passportExtractedModel?.extractedData
            ?.entries
            ?.filter { (_, value) ->
                val v = value?.toString()?.trim()
                !v.isNullOrEmpty() && v != "null"
            }
            ?.sortedBy { it.key }
            ?.joinToString("\n") { "${it.key}: ${it.value}" }
            ?.takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
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

        completeScan()
    }
    override fun onTranslatedError(properties: Map<String, String>?) {
        completeScan()
    }



}