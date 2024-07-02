package  com.assentify.sdk

import AssentifySdk
import AssentifySdkCallback
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import com.assentify.sdk.Core.Constants.EnvironmentalConditions
import com.assentify.sdk.RemoteClient.Models.ConfigModel
import com.assentify.sdk.RemoteClient.Models.TemplatesByCountry

class SplashScreen : AppCompatActivity(), AssentifySdkCallback {
    private lateinit var assentifySdk: AssentifySdk;
    private lateinit var  passportClick: LinearLayout
    private lateinit var  idClick: LinearLayout
    private lateinit var  otherClick: LinearLayout
    private lateinit var  loadingText: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        assentifySdk  = AssentifySdk(this);
        val environmentalConditions = EnvironmentalConditions(
            true,
            true,
            500.0f,
            00.0f,
            50.0f,
            100.0f,
            "#61A03A",
            "#FFC400",

            );


        assentifySdk.initialize("trwMw7AKPIVuNii4Y3D0EncDidbCwLQ4kVcJlnBGdY8CO3zVhydAn7sguTA3oVC9I9NdWAMM9nsDjriCN4g",
            "b096e6ea-2a81-44cb-858e-08dbcbc01489",
            "8B31D32E1F7476671B743DD6C0D4B4E0D4AA6C0386A16E4586D8DBA9B25AEFEA",
            environmentalConditions,
            this,
            true,
            true,
            false,
            true,
            true,
            true,)

        loadingText = findViewById(R.id.loadingText);
        loadingText.visibility = View.VISIBLE

        passportClick = findViewById(R.id.passportClick);
        idClick = findViewById(R.id.idClick);
        otherClick = findViewById(R.id.otherClick);


        passportClick.visibility = View.GONE
        idClick.visibility = View.GONE
        otherClick.visibility = View.GONE

        passportClick.setOnClickListener {
            val intent = Intent(this, ScanPassportActivity::class.java);
            startActivity(intent)
        }

    }

    override fun onAssentifySdkInitError(message: String) {
        Log.e("Events Here onAssentifySdkInitError", message)
    }

    override fun onAssentifySdkInitSuccess(configModel: ConfigModel) {
        Log.e("Events Here onAssentifySdkInitSuccess", configModel.toString())
        assentifySdk.getTemplates();
            passportClick.visibility = View.VISIBLE
            idClick.visibility = View.VISIBLE
            otherClick.visibility = View.VISIBLE
            loadingText.visibility = View.GONE


    }

    override fun onHasTemplates(templates: List<TemplatesByCountry>) {
        Log.e("Events Here onHasTemplates", templates.toString())
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}