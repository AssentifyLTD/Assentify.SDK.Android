package  com.assentify.sdk

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView

class NavToFace : AppCompatActivity() {
    private lateinit var image: String;
    private lateinit var faceMatch: LinearLayout;
    private lateinit var infoTextTest: TextView;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_to_face)
        image = intent.getStringExtra("image")!!
        faceMatch = findViewById(R.id.faceMatch)
        infoTextTest = findViewById(R.id.infoText)
        val extractedMap = ExtractedModel.getExtractedModel()
        val formattedText = extractedMap.entries.joinToString("\n") { (key, value) ->
            "$key: $value"
        }
        infoTextTest.text = formattedText

        faceMatch.setOnClickListener {
            val intent = Intent(this, FaceMatchActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            intent.putExtra("image", image)
            startActivity(intent)
        }


    }
}