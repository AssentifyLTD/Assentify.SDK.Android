package  com.assentify.sdk

import AssentifySdk
import ImageToBase64Converter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import com.assentify.sdk.Core.Constants.MotionType
import com.assentify.sdk.FaceMatch.FaceMatchCallback
import com.assentify.sdk.FaceMatch.FaceResponseModel
import com.assentify.sdk.Models.BaseResponseDataModel

class FaceMatchActivity : AppCompatActivity() , FaceMatchCallback {
    private lateinit var assentifySdk: AssentifySdk;
    private lateinit var image: String;
    private lateinit var base64Image:String;
    private lateinit var infoText:TextView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        assentifySdk  = AssentifySdk(this);
        image = "https://storagetestassentify.blob.core.windows.net/userfiles/b096e6ea-2a81-44cb-858e-08dbcbc01489/ca0162f9-8cfe-409f-91d8-9c2d42d53207/4f445a214f5a4b7fa74dc81243ccf590/b19c2053-efae-42e8-8696-177809043a9c/ReadPassport/image.jpeg"
         base64Image =
            ImageToBase64Converter().execute(image).get()
        Thread.sleep(1000)
        infoText = findViewById(R.id.infoTextTest)
        startAssentifySdk();
    }

    fun startAssentifySdk() {

       var face = assentifySdk.startFaceMatch(
            this@FaceMatchActivity,
           base64Image
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
        Log.e("onComplete",dataModel.faceExtractedModel!!.extractedData.toString())
    }

    override fun onDocumentCaptured(dataModel: BaseResponseDataModel) {
        
    }

    override fun onDocumentCropped(dataModel: BaseResponseDataModel) {
        
    }

    override fun onEnvironmentalConditionsChange(brightness: Double, motion: MotionType) {
        runOnUiThread{
            infoText.visibility = View.VISIBLE
        }
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
        Log.e("Events Here Face Page","onSend")
    }

    override fun onStatusUpdated(dataModel: BaseResponseDataModel) {
        
    }

    override fun onUpdated(dataModel: BaseResponseDataModel) {
        
    }

    override fun onUploadFailed(dataModel: BaseResponseDataModel) {
        
    }

}