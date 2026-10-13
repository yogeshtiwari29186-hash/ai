package com.example.aivideomaker

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(Color.rgb(13, 18, 35))
        }
        val title = TextView(this).apply {
            text = "AI Video Maker"
            textSize = 28f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val subtitle = TextView(this).apply {
            text = "Local-first prototype • No external AI API"
            textSize = 14f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        val prompt = EditText(this).apply {
            hint = "Describe your video idea..."
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setPadding(16, 16, 16, 16)
            minLines = 3
            gravity = Gravity.TOP
        }
        val status = TextView(this).apply {
            text = "Video engine not connected yet."
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 24, 0, 24)
        }
        val button = Button(this).apply {
            text = "Prepare video prompt"
            setOnClickListener {
                val idea = prompt.text.toString().trim()
                status.text = if (idea.isBlank()) {
                    "Please enter a video idea."
                } else {
                    "Prompt prepared:\n$idea\n\nNo video was generated yet. Next step: connect a compatible local video model."
                }
            }
        }
        root.addView(title)
        root.addView(subtitle)
        root.addView(prompt, LinearLayout.LayoutParams(-1, -2).apply { topMargin = 28 })
        root.addView(button)
        root.addView(status)
        setContentView(root)
    }
}
