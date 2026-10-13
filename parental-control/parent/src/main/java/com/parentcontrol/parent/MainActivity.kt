package com.parentcontrol.parent

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.TranslateAnimation
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.security.SecureRandom

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var root: LinearLayout
    private val navy = Color.rgb(18, 29, 57)
    private val blue = Color.rgb(64, 105, 255)
    private val ink = Color.rgb(30, 41, 59)
    private val muted = Color.rgb(100, 116, 139)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor = navy; window.navigationBarColor = Color.rgb(245,247,252); window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; showLogin() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun shape(color: Int, radius: Int = 20, stroke: Int = 0): GradientDrawable =
        GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != 0) setStroke(dp(1), stroke) }
    private fun text(value: String, size: Float, color: Int, bold: Boolean = false) = TextView(this).apply {
        text = value; textSize = size; setTextColor(color)
        if (bold) setTypeface(typeface, Typeface.BOLD)
    }
    private fun base(eyebrow: String, title: String, subtitle: String) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(28)); setBackgroundColor(Color.rgb(245,247,252)) }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(root) }
        setContentView(scroll)
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(22),dp(22),dp(22),dp(24)); background = shape(navy, 26) }
        hero.addView(text("✦  $eyebrow", 12f, Color.rgb(160,183,255), true))
        hero.addView(text(title, 28f, Color.WHITE, true).apply { setPadding(0,dp(10),0,dp(8)) })
        hero.addView(text(subtitle, 14f, Color.rgb(210,218,235)))
        root.addView(hero, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(20) })
        hero.startAnimation(AlphaAnimation(0.2f, 1f).apply { duration = 450 })
    }
    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18))
        background = shape(Color.WHITE, 22, Color.rgb(229,234,244)); elevation = dp(2).toFloat()
    }.also { root.addView(it, LinearLayout.LayoutParams(-1,-2).apply { bottomMargin = dp(14) }); it.startAnimation(TranslateAnimation(0f,0f,dp(12).toFloat(),0f).apply { duration=320; fillAfter=true }) }
    private fun field(parent: LinearLayout, label: String, hint: String, password: Boolean = false): EditText {
        parent.addView(text(label, 13f, ink, true), LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7); topMargin=dp(12) })
        return EditText(this).apply {
            this.hint = hint; textSize = 15f; setTextColor(ink); setHintTextColor(Color.rgb(148,163,184))
            setPadding(dp(14),dp(13),dp(14),dp(13)); background = shape(Color.rgb(247,249,253), 14, Color.rgb(224,230,240))
            inputType = if(password) 129 else if(label.contains("Email", true)) 33 else 1
            parent.addView(this, LinearLayout.LayoutParams(-1,dp(52)))
        }
    }
    private fun button(parent: LinearLayout, label: String, primary: Boolean = true, action: () -> Unit) {
        val b = TextView(this).apply {
            text = label; textSize=15f; gravity=Gravity.CENTER; setTypeface(typeface,Typeface.BOLD)
            setTextColor(if(primary) Color.WHITE else blue); background=shape(if(primary) blue else Color.rgb(239,243,255),15)
            setPadding(dp(12),dp(15),dp(12),dp(15)); isClickable=true; isFocusable=true
            setOnClickListener { alpha=0.78f; animate().alpha(1f).setDuration(160).start(); action() }
        }
        parent.addView(b, LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10) })
    }
    private fun note(parent: LinearLayout, message: String, color: Int = muted) {
        parent.addView(text(message, 13f, color).apply { setPadding(0,dp(8),0,dp(8)) })
    }
    private fun showLogin() {
        base("FAMILY • PARENT", "Welcome back.", "Sign in to open your family dashboard.")
        val c=card()
        c.addView(text("Login",22f,ink,true))
        note(c,"Use the email and password for your existing parent account.")
        val email=field(c,"Email address","you@example.com")
        val password=field(c,"Password","Your password",true)
        button(c,"Sign in") { authenticate(email.text.toString(),password.text.toString(),false) }
        button(c,"Create a new account",false) { showSignup() }
        note(c,"New here? Create an account first.")
    }
    private fun showSignup() {
        base("FAMILY • PARENT", "Create account.", "Set up your parent account to start connecting devices.")
        val c=card()
        c.addView(text("Sign up",22f,ink,true))
        note(c,"Use a valid email address and a password of at least 6 characters.")
        val email=field(c,"Email address","you@example.com")
        val password=field(c,"Create password","At least 6 characters",true)
        val confirm=field(c,"Confirm password","Enter the password again",true)
        button(c,"Create parent account") {
            if(password.text.toString()!=confirm.text.toString()) { error("Passwords do not match","Enter the same password in both fields."); return@button }
            authenticate(email.text.toString(),password.text.toString(),true)
        }
        button(c,"Already have an account? Login",false) { showLogin() }
    }
    private fun authenticate(email: String, password: String, create: Boolean) {
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.length < 6) { error("Check your details", "Enter a valid email address and a password with at least 6 characters."); return }
        val task=if(create) auth.createUserWithEmailAndPassword(email.trim(),password) else auth.signInWithEmailAndPassword(email.trim(),password)
        task.addOnSuccessListener {
            val uid=auth.currentUser?.uid ?: return@addOnSuccessListener
            db.child("users").child(uid).updateChildren(mapOf("role" to "parent","email" to email.trim()))
                .addOnSuccessListener { showDashboard() }
                .addOnFailureListener { showDashboard(); error(if(create) "Account created, profile save blocked" else "Login succeeded, profile update blocked", explainDatabaseError(it.message ?: "Unknown database error")) }
        }.addOnFailureListener { error(if(create) "Sign-up failed" else "Login failed", explainAuthError(it.message ?: "Unknown authentication error")) }
    }
    private fun showDashboard() {
        base("PARENT SPACE","Family dashboard","Signed in as " + (auth.currentUser?.email ?: "parent"))
        val pair=card()
        pair.addView(text("01  •  CONNECT A DEVICE",12f,blue,true))
        pair.addView(text("Pair your child’s app",20f,ink,true).apply { setPadding(0,dp(8),0,dp(4)) })
        note(pair,"Generate a one-time code. Share it directly with your child; it expires in 10 minutes.")
        button(pair,"Generate pairing code") { createPairingCode() }
        val settings=card()
        settings.addView(text("02  •  FAMILY ROUTINE",12f,blue,true))
        settings.addView(text("Screen-time preferences",20f,ink,true).apply { setPadding(0,dp(8),0,dp(4)) })
        val childId=field(settings,"Child account UID","Paste the UID shown in Child App")
        val bedtime=field(settings,"Bedtime","21:00")
        val minutes=field(settings,"Daily screen-time limit (minutes)","120")
        button(settings,"Save family settings") {
            val uid=auth.currentUser?.uid ?: return@button
            val child=childId.text.toString().trim()
            val limit=minutes.text.toString().toIntOrNull()
            if(child.isBlank()) { error("Child UID missing","Pair the child app first, then enter its account UID."); return@button }
            if(limit == null || limit !in 1..1440) { error("Invalid screen-time limit","Enter a whole number from 1 to 1440 minutes."); return@button }
            val time=bedtime.text.toString().trim().ifBlank { "21:00" }
            if(!time.matches(Regex("([01]\\d|2[0-3]):[0-5]\\d"))) { error("Invalid bedtime","Use 24-hour time, for example 21:00."); return@button }
            db.child("families").child(uid).child("children").child(child).child("settings")
                .updateChildren(mapOf("bedtime" to time,"dailyLimitMinutes" to limit,"updatedAt" to System.currentTimeMillis()))
                .addOnSuccessListener { error("Saved", "Family preferences were saved to Firebase.") }
                .addOnFailureListener { error("Save failed", explainDatabaseError(it.message ?: "Unknown database error")) }
        }
        button(settings,"Refresh paired children",false) {
            val uid=auth.currentUser?.uid ?: return@button
            db.child("families").child(uid).child("children").get()
                .addOnSuccessListener { snap ->
                    val ids=snap.children.mapNotNull { it.key }
                    error("Paired devices", if(ids.isEmpty()) "No child devices are paired yet." else ids.joinToString("\\n"))
                }.addOnFailureListener { error("Could not load devices", explainDatabaseError(it.message ?: "Unknown database error")) }
        }
        button(settings,"Sign out",false) { auth.signOut(); showAuth() }
        note(root,"Privacy note: this starter saves preferences; Android enforcement and live activity monitoring are not implemented yet.")
    }
    private fun createPairingCode() {
        val parentUid=auth.currentUser?.uid ?: return
        val code=(100000+SecureRandom().nextInt(900000)).toString()
        val now=System.currentTimeMillis()
        val payload=mapOf("parentUid" to parentUid,"createdAt" to now,"expiresAt" to now+600000L,"used" to false)
        db.child("pairingCodes").child(code).setValue(payload).addOnSuccessListener {
            base("SECURE PAIRING","Your pairing code", "Share this code with your child within the next 10 minutes.")
            val c=card()
            c.addView(text("ONE-TIME CODE",12f,muted,true))
            c.addView(text(code,38f,blue,true).apply { gravity=Gravity.CENTER; setPadding(0,dp(18),0,dp(18)); letterSpacing=0.12f })
            note(c,"Only share this code with the intended family member.")
            button(c,"Back to dashboard") { showDashboard() }
        }.addOnFailureListener { error("Could not create code", explainDatabaseError(it.message ?: "Unknown database error")) }
    }
    private fun explainDatabaseError(raw: String): String {
        val s=raw.lowercase()
        return if(s.contains("permission_denied") || s.contains("permission denied")) {
            "Firebase rejected this database request (PERMISSION_DENIED).\n\nWhy it happens:\n• Realtime Database Rules do not allow this signed-in user to read/write this path.\n• The app may be using a different Firebase project/database URL.\n• The current rules may require a parent/child relationship that is not yet present.\n\nFix: Firebase Console → Realtime Database → Rules. Check rules for users/$, pairingCodes/$ and families/$; authorize only the signed-in owner and the intended paired parent/child. Do not enable public read/write.\n\nTechnical detail: $raw"
        } else "Firebase could not complete the database request. Check internet, the selected Firebase project, Authentication status, and Realtime Database Rules.\n\nTechnical detail: $raw"
    }
    private fun explainAuthError(raw: String): String {
        val s=raw.lowercase()
        val hint=when {
            s.contains("email address is badly formatted") -> "The email format looks invalid. Check the address and try again."
            s.contains("password is invalid") || s.contains("wrong-password") || s.contains("invalid-credential") -> "The password may be incorrect. Try again or use password reset."
            s.contains("email address is already in use") || s.contains("email-already-in-use") -> "This email already has an account. Choose Sign in."
            s.contains("network") -> "Network issue. Check your internet connection and retry."
            else -> "Check Firebase Console → Authentication → Sign-in method and make sure Email/Password is enabled."
        }
        return "$hint\n\nTechnical detail: $raw"
    }
    private fun error(title: String, message: String) {
        android.app.AlertDialog.Builder(this).setTitle(title).setMessage(message)
            .setPositiveButton("Got it", null).setNeutralButton("Copy details") { _, _ ->
                val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText(title, "$title\n$message"))
                Toast.makeText(this,"Error details copied",Toast.LENGTH_SHORT).show()
            }.show()
    }
}
