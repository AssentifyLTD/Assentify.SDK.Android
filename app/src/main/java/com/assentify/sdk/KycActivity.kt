package com.assentify.sdk

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class KycActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_kych)


        listView = findViewById(R.id.templateListView)

        val templatesList = KysModel.getKys().get(0).templates;

        val names = templatesList.map { it.kycDocumentType }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, names)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val selected = templatesList[position]
            KysModel.setSelected(selected)
            val intent = Intent(this, ScanIDActivity::class.java);
            startActivity(intent)
        }
    }
}
