package com.parentcontrol.child

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showAuth() }
    private fun base(title: String) {
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(36, 42, 36, 32); setBackgroundColor(Color.rgb(246, 248, 252)) }
        root.addView(TextView(this).apply { text = title; textSize = 27f; setTextColor(Color.rgb(26, 44, 82)); setPadding(0, 0, 0, 22) })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun field(hint: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint; if (password) inputType = 129; textSize = 16f
        root.addView(this, ViewGroup.LayoutParams(-1, -2))
    }
    private fun button(label: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; setOnClickListener { action() } }, ViewGroup.LayoutParams(-1, -2))
    }
    private fun note(message: String) {
        root.addView(TextView(this).apply { text = message; textSize = 15f; setTextColor(Color.DKGRAY); setPadding(0, 12, 0, 12) })
    }
    private fun showAuth() {
        base("Child Control")
        note("Create an account, then enter the pairing code from your parent. This app remains visible on your device.")
        val email = field("Email address")
        val password = field("Password (6+ characters)", true)
        button("Create account") { authenticate(email.text.toString(), password.text.toString(), true) }
        button("Sign in") { authenticate(email.text.toString(), password.text.toString(), false) }
    }
    private fun authenticate(email: String, password: String, create: Boolean) {
        if (email.isBlank() || password.length < 6) { error("Enter a valid email and password (6+ characters)."); return }
        val task = if (create) auth.createUserWithEmailAndPassword(email.trim(), password) else auth.signInWithEmailAndPassword(email.trim(), password)
        task.addOnSuccessListener {
            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
            db.child("users").child(uid).updateChildren(mapOf("role" to "child", "email" to email.trim()))
                .addOnSuccessListener { showDashboard() }.addOnFailureListener { error(it.message ?: "Could not save profile") }
        }.addOnFailureListener { error(it.message ?: "Authentication failed") }
    }
    private fun showDashboard() {
        base("Child Dashboard")
        val uid = auth.currentUser?.uid ?: return
        note("Your device/account ID: " + uid)
        note("Give this ID to your parent after pairing. Controls should be discussed openly.")
        val code = field("6-digit pairing code")
        button("Pair with parent") {
            val entered = code.text.toString().trim()
            if (!entered.matches(Regex("\\d{6}"))) { error("Enter the 6-digit code."); return@button }
            db.child("pairingCodes").child(entered).get().addOnSuccessListener { snap ->
                val parentUid = snap.child("parentUid").getValue(String::class.java)
                val expires = snap.child("expiresAt").getValue(Long::class.java) ?: 0L
                val used = snap.child("used").getValue(Boolean::class.java) ?: true
                if (parentUid.isNullOrBlank() || expires < System.currentTimeMillis() || used) {
                    error("Code invalid, expired, or already used."); return@addOnSuccessListener
                }
                val updates = hashMapOf<String, Any>(
                    "families/" + parentUid + "/children/" + uid + "/pairedAt" to System.currentTimeMillis(),
                    "families/" + parentUid + "/children/" + uid + "/email" to (auth.currentUser?.email ?: ""),
                    "pairingCodes/" + entered + "/used" to true
                )
                db.updateChildren(updates).addOnSuccessListener {
                    base("Paired Successfully")
                    note("Parent connection saved. Your device ID: " + uid)
                    note("This starter does not secretly access your camera, messages, or screen.")
                    button("Refresh settings") { loadSettings() }
                    button("Sign out") { auth.signOut(); showAuth() }
                }.addOnFailureListener { error(it.message ?: "Pairing failed") }
            }.addOnFailureListener { error(it.message ?: "Could not validate code") }
        }
        button("Sign out") { auth.signOut(); showAuth() }
    }
    private fun loadSettings() {
        val uid = auth.currentUser?.uid ?: return
        db.child("users").child(uid).get().addOnSuccessListener { user ->
            note("Signed in as: " + (user.child("email").getValue(String::class.java) ?: ""))
            note("This is an initial MVP. Live family settings will be connected in a follow-up.")
        }.addOnFailureListener { error(it.message ?: "Could not refresh") }
    }
    private fun error(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
