package com.assentify.sdk

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity


class CrashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val textView = TextView(this)
        textView.textSize = 16f
        textView.setTextColor(Color.RED)
        textView.setPadding(20, 50, 20, 50)

        val error = intent.getStringExtra("error")
        textView.text = error

        setContentView(textView)
    }

}