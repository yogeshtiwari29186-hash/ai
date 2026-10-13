package com.parentcontrol.parent

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.ViewGroup
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.security.SecureRandom

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); showAuth() }
    private fun base(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 42, 36, 32)
            setBackgroundColor(Color.rgb(246, 248, 252))
        }
        root.addView(TextView(this).apply {
            text = title; textSize = 27f; setTextColor(Color.rgb(26, 44, 82)); setPadding(0, 0, 0, 22)
        })
        setContentView(ScrollView(this).apply { addView(root) })
    }
    private fun field(hint: String, password: Boolean = false) = EditText(this).apply {
        this.hint = hint
        if (password) inputType = 129
        textSize = 16f
        root.addView(this, ViewGroup.LayoutParams(-1, -2))
    }
    private fun button(label: String, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; setOnClickListener { action() } }, ViewGroup.LayoutParams(-1, -2))
    }
    private fun note(message: String) {
        root.addView(TextView(this).apply { text = message; textSize = 15f; setTextColor(Color.DKGRAY); setPadding(0, 12, 0, 12) })
    }
    private fun showAuth() {
        base("Parent Control")
        note("Sign in or create a parent account.")
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
            db.child("users").child(uid).updateChildren(mapOf("role" to "parent", "email" to email.trim()))
                .addOnSuccessListener { showDashboard() }.addOnFailureListener { error(it.message ?: "Could not save profile") }
        }.addOnFailureListener { error(it.message ?: "Authentication failed") }
    }
    private fun showDashboard() {
        base("Parent Dashboard")
        note("Account: " + (auth.currentUser?.email ?: ""))
        note("Pair a child using a one-time code. Give the code directly to your child.")
        button("Generate pairing code") { createPairingCode() }
        val childId = field("Paired child UID (shown in Child App)")
        val bedtime = field("Bedtime (e.g. 21:00)")
        val minutes = field("Daily screen-time limit in minutes")
        button("Save family settings") {
            val uid = auth.currentUser?.uid ?: return@button
            val limit = minutes.text.toString().toIntOrNull() ?: 120
            if (limit !in 1..1440) { error("Limit must be 1–1440 minutes."); return@button }
            if (childId.text.isBlank()) { error("Enter the child's UID after pairing."); return@button }
            val values = mapOf<String, Any>("bedtime" to bedtime.text.toString().ifBlank { "21:00" },
                "dailyLimitMinutes" to limit, "updatedAt" to System.currentTimeMillis())
            db.child("families").child(uid).child("children").child(childId.text.toString().trim()).child("settings")
                .updateChildren(values).addOnSuccessListener { Toast.makeText(this, "Settings saved to Firebase.", Toast.LENGTH_LONG).show() }
                .addOnFailureListener { error(it.message ?: "Save failed") }
        }
        button("Refresh paired children") {
            val uid = auth.currentUser?.uid ?: return@button
            db.child("families").child(uid).child("children").get().addOnSuccessListener { snap ->
                note("Paired child UIDs: " + snap.children.mapNotNull { it.key }.joinToString().ifBlank { "None yet" })
            }.addOnFailureListener { error(it.message ?: "Could not load children") }
        }
        button("Sign out") { auth.signOut(); showAuth() }
    }
    private fun createPairingCode() {
        val parentUid = auth.currentUser?.uid ?: return
        val code = (100000 + SecureRandom().nextInt(900000)).toString()
        val now = System.currentTimeMillis()
        val payload = mapOf("parentUid" to parentUid, "createdAt" to now, "expiresAt" to now + 600000L, "used" to false)
        db.child("pairingCodes").child(code).setValue(payload).addOnSuccessListener {
            base("Pairing Code")
            note("Give this code to your child within 10 minutes:")
            root.addView(TextView(this).apply { text = code; textSize = 40f; setTextColor(Color.rgb(26, 92, 170)) })
            note("Production pairing needs server-enforced one-time use and restrictive database rules.")
            button("Back to dashboard") { showDashboard() }
        }.addOnFailureListener { error(it.message ?: "Could not create code") }
    }
    private fun error(message: String) { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
}
