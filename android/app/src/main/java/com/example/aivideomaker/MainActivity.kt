package com.example.aivideomaker

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var prompt: EditText
    private lateinit var formatSpinner: Spinner
    private lateinit var durationSpinner: Spinner
    private lateinit var resultText: TextView
    private lateinit var voiceButton: Button
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentPlan = ""

    private val bg = Color.rgb(12, 16, 31)
    private val panel = Color.rgb(24, 31, 53)
    private val accent = Color.rgb(116, 96, 255)
    private val white = Color.rgb(245, 247, 255)
    private val muted = Color.rgb(174, 183, 205)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)

        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(26), dp(22), dp(28))
            background = null
        }
        scroll.addView(root)

        root.addView(label("AI VIDEO MAKER", 13f, accent, true))
        root.addView(label("Turn your idea into a video plan", 27f, white, true).apply {
            setPadding(0, dp(8), 0, dp(6))
        })
        root.addView(label("Create a script, scene-by-scene visual prompts and voice narration.", 15f, muted, false))

        root.addView(sectionTitle("1  Describe your video"))
        prompt = EditText(this).apply {
            hint = "Example: A cinematic story about a village boy who builds a robot..."
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 16f
            minLines = 4
            gravity = Gravity.TOP
            setPadding(dp(15), dp(14), dp(15), dp(14))
            setBackgroundColor(panel)
        }
        root.addView(prompt, params(top = 12))

        root.addView(sectionTitle("2  Choose your format"))
        formatSpinner = spinner(listOf("YouTube Shorts / Reels (9:16)", "YouTube video (16:9)", "Square video (1:1)"))
        root.addView(formatSpinner, params(top = 10))
        durationSpinner = spinner(listOf("30 seconds", "60 seconds", "2 minutes", "5 minutes"))
        root.addView(durationSpinner, params(top = 10))

        val generate = button("Create video plan", accent)
        root.addView(generate, params(top = 18))
        generate.setOnClickListener { makePlan() }

        root.addView(sectionTitle("Your storyboard"))
        resultText = label("Your script and scene prompts will appear here.", 14f, muted, false).apply {
            setPadding(dp(14), dp(15), dp(14), dp(15))
            setBackgroundColor(panel)
        }
        root.addView(resultText, params(top = 10))

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        voiceButton = button("▶  Listen to narration", Color.rgb(42, 53, 78))
        voiceButton.isEnabled = false
        voiceButton.setOnClickListener {
            if (!ttsReady) {
                toast("Voice engine is still starting. Try again.")
            } else if (currentPlan.isNotBlank()) {
                val narration = currentPlan.substringAfter("NARRATION\n", "").substringBefore("\n\nSCENE PLAN")
                if (narration.isNotBlank()) tts?.speak(narration, TextToSpeech.QUEUE_FLUSH, null, "video_narration")
            }
        }
        actions.addView(voiceButton, params(top = 10))
        val stopVoice = button("Stop voice", Color.rgb(42, 53, 78))
        stopVoice.setOnClickListener { tts?.stop() }
        actions.addView(stopVoice, params(top = 8))
        val share = button("Share / export plan", Color.rgb(42, 53, 78))
        share.setOnClickListener {
            if (currentPlan.isBlank()) toast("Create a plan first.") else {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "My AI Video Plan")
                    putExtra(Intent.EXTRA_TEXT, currentPlan)
                }
                startActivity(Intent.createChooser(intent, "Export video plan"))
            }
        }
        actions.addView(share, params(top = 8))
        root.addView(actions)
        root.addView(label("Important: this version creates a storyboard and voice narration. It does not yet render an AI-generated video file. A real text-to-video provider/API must be connected for that.", 12f, muted, false).apply {
            setPadding(0, dp(20), 0, 0)
        })

        setContentView(scroll)
    }

    private fun makePlan() {
        val idea = prompt.text.toString().trim()
        if (idea.length < 5) {
            prompt.error = "Please describe your video idea."
            return
        }
        val format = formatSpinner.selectedItem.toString()
        val duration = durationSpinner.selectedItem.toString()
        val seconds = when (duration) {
            "30 seconds" -> 30
            "60 seconds" -> 60
            "2 minutes" -> 120
            else -> 300
        }
        val sceneCount = when {
            seconds <= 30 -> 4
            seconds <= 60 -> 6
            seconds <= 120 -> 8
            else -> 12
        }
        val opening = "Imagine a world where $idea"
        val narration = buildString {
            append(opening)
            append(". Here is how the story unfolds. ")
            append("First, we introduce the setting and the main idea. ")
            append("Then, a challenge appears and changes everything. ")
            append("Watch as each moment builds toward the big reveal. ")
            append("The final moment brings the idea together and leaves us with something to think about. ")
            append("What would you do in this situation? Share your thoughts and follow for more stories.")
        }
        val sceneLines = (1..sceneCount).joinToString("\n\n") { i ->
            val start = ((i - 1) * seconds / sceneCount)
            val end = (i * seconds / sceneCount)
            val beat = when (i) {
                1 -> "HOOK: reveal the most intriguing part of the idea immediately"
                2 -> "ESTABLISH: show the setting and main subject"
                3 -> "BUILD: introduce movement, details and a clear goal"
                4 -> "CONFLICT: show an obstacle or surprising change"
                sceneCount -> "PAYOFF: deliver a satisfying reveal and end frame"
                else -> "PROGRESSION: advance the story with a new visual detail"
            }
            "SCENE $i  •  ${timeLabel(start)}–${timeLabel(end)}\nBeat: $beat\nVisual prompt: Cinematic, high-detail footage about \"$idea\"; purposeful camera movement, expressive lighting, clear subject, realistic textures, coherent composition. Shot $i of $sceneCount.\nOn-screen text: ${if (i == 1) "Wait for it..." else if (i == sceneCount) "What do you think?" else idea.take(42)}"
        }
        currentPlan = "AI VIDEO PLAN\nIdea: $idea\nFormat: $format\nTarget duration: $duration\n\nNARRATION\n$narration\n\nSCENE PLAN\n$sceneLines\n\nEDITING NOTES\nUse quick cuts for Shorts and slower transitions for long-form. Add subtitles, keep music lower than narration, and use only visuals/audio you have rights to.\n\nSTATUS\nStoryboard prepared locally. No video file has been rendered."
        resultText.text = currentPlan
        resultText.setTextColor(white)
        voiceButton.isEnabled = true
        toast("Storyboard created.")
    }

    private fun timeLabel(seconds: Int): String = "%d:%02d".format(seconds / 60, seconds % 60)

    private fun spinner(items: List<String>): Spinner {
        return Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
    }

    private fun sectionTitle(text: String) = label(text, 17f, white, true).apply {
        setPadding(0, dp(24), 0, 0)
    }

    private fun label(text: String, size: Float, color: Int, bold: Boolean): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setLineSpacing(dp(4).toFloat(), 1f)
        }

    private fun button(text: String, color: Int): Button = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        textSize = 15f
        isAllCaps = false
        setBackgroundColor(color)
        minHeight = dp(50)
    }

    private fun params(top: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            if (top > 0) topMargin = dp(top)
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    override fun onInit(status: Int) {
        ttsReady = status == TextToSpeech.SUCCESS
        if (ttsReady) tts?.language = Locale.getDefault()
        runOnUiThread {
            if (::voiceButton.isInitialized) voiceButton.isEnabled = ttsReady && currentPlan.isNotBlank()
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
