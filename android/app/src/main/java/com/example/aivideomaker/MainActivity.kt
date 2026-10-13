package com.example.aivideomaker

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.app.DownloadManager
import android.net.Uri
import android.os.Environment
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
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
    private lateinit var backendUrl: EditText
    private lateinit var generateVideoButton: Button
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

        root.addView(sectionTitle("3  Generate a real AI video"))
        root.addView(label("Connect your deployed backend URL. The Replicate API key stays on the server, never inside the APK.", 13f, muted, false).apply {
            setPadding(0, dp(8), 0, 0)
        })
        backendUrl = EditText(this).apply {
            hint = "https://your-backend.example.com"
            setHintTextColor(muted)
            setTextColor(white)
            textSize = 15f
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_URI
            setPadding(dp(14), dp(12), dp(14), dp(12))
            setBackgroundColor(panel)
        }
        root.addView(backendUrl, params(top = 10))
        generateVideoButton = button("Generate AI video", accent)
        root.addView(generateVideoButton, params(top = 10))
        generateVideoButton.setOnClickListener { generateRealVideo() }

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
        root.addView(label("AI video generation requires a deployed backend and a provider account with available credits. The storyboard works locally even when the backend is not configured.", 12f, muted, false).apply {
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


    private fun generateRealVideo() {
        val idea = prompt.text.toString().trim()
        val base = backendUrl.text.toString().trim().trimEnd('/')
        if (idea.length < 5) {
            prompt.error = "Enter a video idea first."
            return
        }
        if (!base.startsWith("https://")) {
            backendUrl.error = "Use your deployed HTTPS backend URL."
            toast("Deploy the backend and enter its HTTPS URL.")
            return
        }
        generateVideoButton.isEnabled = false
        generateVideoButton.text = "Starting generation…"
        resultText.text = "Connecting to the AI video service…"
        Thread {
            try {
                val createBody = JSONObject().put("prompt", idea)
                val created = apiRequest("$base/api/videos", "POST", createBody)
                val id = created.optString("id")
                var status = created.optString("status", "starting")
                var videoUrl = created.optString("videoUrl")
                val error = created.optString("error")
                if (id.isBlank() && videoUrl.isBlank()) {
                    throw IllegalStateException(if (error.isNotBlank() && error != "null") error else "Backend did not return a generation ID.")
                }
                var attempts = 0
                while (videoUrl.isBlank() && status !in listOf("succeeded", "failed", "canceled") && attempts < 180) {
                    Thread.sleep(5000)
                    val state = apiRequest("$base/api/videos/\${Uri.encode(id)}", "GET", null)
                    status = state.optString("status", status)
                    videoUrl = state.optString("videoUrl")
                    val stateError = state.optString("error")
                    runOnUiThread { resultText.text = "AI video generation status: $status\nPlease keep the app open while the status is checked." }
                    if (status == "failed" || status == "canceled") {
                        throw IllegalStateException(if (stateError.isNotBlank() && stateError != "null") stateError else "Video generation $status.")
                    }
                    attempts++
                }
                if (videoUrl.isBlank()) throw IllegalStateException("Generation is taking too long. Try again later; check your provider dashboard.")
                if (!videoUrl.startsWith("https://")) throw IllegalStateException("The provider returned a non-HTTPS video URL.")
                runOnUiThread {
                    resultText.text = "AI video is ready!\nStarting download to your Downloads folder…"
                    val request = DownloadManager.Request(Uri.parse(videoUrl))
                        .setTitle("AI Video Maker video")
                        .setDescription("Generated video")
                        .setMimeType("video/mp4")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "AI-Video-\${System.currentTimeMillis()}.mp4")
                    val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    manager.enqueue(request)
                    generateVideoButton.text = "Generate another AI video"
                    toast("Video download started. Check Downloads.")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    resultText.text = "AI video generation failed.\n\${e.message ?: "Unknown error"}\n\nYour storyboard is still available above."
                    generateVideoButton.text = "Retry AI video generation"
                    toast("Could not generate video.")
                }
            } finally {
                runOnUiThread { generateVideoButton.isEnabled = true }
            }
        }.start()
    }

    private fun apiRequest(address: String, method: String, body: JSONObject?): JSONObject {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20000
            readTimeout = 30000
            setRequestProperty("Accept", "application/json")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val json = try { JSONObject(response) } catch (_: Exception) { JSONObject() }
            if (code !in 200..299) {
                throw IllegalStateException(json.optString("error").ifBlank { "Backend returned HTTP $code." })
            }
            return json
        } finally {
            connection.disconnect()
        }
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
