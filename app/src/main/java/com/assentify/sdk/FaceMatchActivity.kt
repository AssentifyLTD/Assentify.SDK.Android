package com.assentify.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.assentify.sdk.AssentifySdk
import com.assentify.sdk.Core.Constants.BrightnessEvents
import com.assentify.sdk.Core.Constants.FaceEvents
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.Core.Constants.ZoomType
import com.assentify.sdk.FaceMatch.FaceMatch
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel
import com.assentify.sdk.ScanIDCard.IDCardCallback
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Base64
import android.widget.ImageView
import android.widget.Toast
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class FaceMatchActivity : AppCompatActivity(), FaceMatchCallback {
    private lateinit var image: String;
    private lateinit var base64Image: String;
    private lateinit var infoText: TextView;
    private lateinit var face: FaceMatch;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        image = intent.getStringExtra("image")!!
       base64Image =
            ImageToBase64Converter().execute(image).get()
        Thread.sleep(1000)
        infoText = findViewById(R.id.infoTextTest)
        infoText.visibility = View.GONE
        startAssentifySdk();
    }


    /** FACE NATCH **/
    fun startAssentifySdk() {
        val assentifySdk: AssentifySdk = AssentifySdkObject.getAssentifySdkObject();
        face = assentifySdk.startFaceMatch(
            this, // This activity implemented from from FaceMatchCallback
            base64Image, showCountDown = true // Target  Image
        );
        Thread.sleep(1000)
        var fragmentManager = supportFragmentManager
        var transaction = fragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, face)
        transaction.addToBackStack(null) // Optional: Adds the transaction to the back stack
        transaction.commit()
    }

    override fun onCardDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onClipPreparationComplete(dataModel: BaseResponseDataModel) {

    }

    override fun onComplete(dataModel: FaceResponseModel) {
        infoText.visibility = View.GONE
        Log.e("onComplete",dataModel.faceExtractedModel.toString())

        runOnUiThread {
            var  imageView1 = findViewById<ImageView>(R.id.imageView1)
            var  imageView2 = findViewById<ImageView>(R.id.imageView2)

            ImageDownloaderToBitmap { bitmap ->
                if (bitmap != null) {
                    imageView1.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Failed to download image", Toast.LENGTH_SHORT).show()
                }
            }.execute(dataModel.faceExtractedModel!!.baseImageFace)

            infoText.visibility = View.VISIBLE

            ImageDownloaderToBitmap { bitmap ->
                if (bitmap != null) {
                    imageView2.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(this, "Failed to download image", Toast.LENGTH_SHORT).show()
                }
            }.execute(dataModel.faceExtractedModel!!.secondImageFace)
        }


    }

    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {

    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {

    }




    override fun onError(dataModel: BaseResponseDataModel) {

    }

    override fun onFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onFaceExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onLivenessUpdate(dataModel: BaseResponseDataModel) {

    }

    override fun onMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onMrzExtracted(dataModel: BaseResponseDataModel) {

    }

    override fun onNoFaceDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onNoMrzDetected(dataModel: BaseResponseDataModel) {

    }

    override fun onQualityCheckAvailable(dataModel: BaseResponseDataModel) {

    }

    override fun onRetry(dataModel: BaseResponseDataModel) {

    }

    override fun onSend() {
        infoText.visibility = View.VISIBLE
        infoText.text = "Sending ..."
        Log.e("Events Here Face Page", "onSend")
    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {

    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {

    }

    override fun onEnvironmentalConditionsChange(
        brightnessEvents: BrightnessEvents,
        motion: MotionType,
        faceEvents: FaceEvents,
        zoom: ZoomType
    ) {
    }
}

class ImageToBase64Converter() : AsyncTask<String, Void, String>() {

    override fun doInBackground(vararg params: String): String? {
        val imageUrl = params[0]
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true

            // Add the X-Api-Key header
            connection.setRequestProperty("X-Api-Key", "6YfQqRbSfTinDuyk7TfFAAdDL55t4EBId4Il2vOFQycl4KydnWsGkA76UNLI3GcrHW3nqoz1gvoewxqsJA")

            connection.connect()
            val input: InputStream = connection.inputStream
            val bitmap: Bitmap = BitmapFactory.decodeStream(input)

            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(base64Image: String?) {
        // Handle the base64 image result here if needed
    }
}

class ImageDownloaderToBitmap(
    private val callback: (Bitmap?) -> Unit
) : AsyncTask<String, Void, Bitmap?>() {

    override fun doInBackground(vararg params: String): Bitmap? {
        val imageUrl = params[0]
        return try {
            val url = URL(imageUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true

            // Add API Key header if needed
            connection.setRequestProperty("X-Api-Key", "6YfQqRbSfTinDuyk7TfFAAdDL55t4EBId4Il2vOFQycl4KydnWsGkA76UNLI3GcrHW3nqoz1gvoewxqsJA")

            connection.connect()
            val input: InputStream = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onPostExecute(result: Bitmap?) {
        callback(result)
    }
}


