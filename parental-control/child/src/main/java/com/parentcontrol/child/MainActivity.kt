package com.parentcontrol.child

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

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var root: LinearLayout
    private val navy=Color.rgb(18,29,57)
    private val teal=Color.rgb(14,165,160)
    private val ink=Color.rgb(30,41,59)
    private val muted=Color.rgb(100,116,139)

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=navy; window.navigationBarColor=Color.rgb(245,247,252); window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR; showLogin() }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,radius:Int=20,stroke:Int=0)=GradientDrawable().apply { setColor(color); cornerRadius=dp(radius).toFloat(); if(stroke!=0) setStroke(dp(1),stroke) }
    private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply { text=value; textSize=size; setTextColor(color); if(bold) setTypeface(typeface,Typeface.BOLD) }
    private fun base(eyebrow:String,title:String,subtitle:String) {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(22),dp(22),dp(28)); setBackgroundColor(Color.rgb(245,247,252)) }
        setContentView(ScrollView(this).apply { isFillViewport=true; addView(root) })
        val hero=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(22),dp(22),dp(24)); background=shape(navy,26) }
        hero.addView(text("✦  $eyebrow",12f,Color.rgb(117,232,219),true))
        hero.addView(text(title,28f,Color.WHITE,true).apply { setPadding(0,dp(10),0,dp(8)) })
        hero.addView(text(subtitle,14f,Color.rgb(210,218,235)))
        root.addView(hero,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(20) })
        hero.startAnimation(AlphaAnimation(0.2f,1f).apply { duration=450 })
    }
    private fun card()=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(18),dp(18),dp(18),dp(18)); background=shape(Color.WHITE,22,Color.rgb(229,234,244)); elevation=dp(2).toFloat()
    }.also { root.addView(it,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(14) }); it.startAnimation(TranslateAnimation(0f,0f,dp(12).toFloat(),0f).apply { duration=320; fillAfter=true }) }
    private fun field(parent:LinearLayout,label:String,hint:String,password:Boolean=false):EditText {
        parent.addView(text(label,13f,ink,true),LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(7); topMargin=dp(12) })
        return EditText(this).apply {
            this.hint=hint; textSize=15f; setTextColor(ink); setHintTextColor(Color.rgb(148,163,184)); setPadding(dp(14),dp(13),dp(14),dp(13))
            background=shape(Color.rgb(247,249,253),14,Color.rgb(224,230,240)); inputType=if(password) 129 else if(label.contains("Email",true)) 33 else 1
            parent.addView(this,LinearLayout.LayoutParams(-1,dp(52)))
        }
    }
    private fun button(parent:LinearLayout,label:String,primary:Boolean=true,action:()->Unit) {
        parent.addView(TextView(this).apply {
            text=label; textSize=15f; gravity=Gravity.CENTER; setTypeface(typeface,Typeface.BOLD); setTextColor(if(primary) Color.WHITE else teal)
            background=shape(if(primary) teal else Color.rgb(225,250,247),15); setPadding(dp(12),dp(15),dp(12),dp(15)); isClickable=true; isFocusable=true
            setOnClickListener { animate().alpha(0.75f).setDuration(70).withEndAction { animate().alpha(1f).setDuration(120).start(); action() }.start() }
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10) })
    }
    private fun note(parent:LinearLayout,message:String,color:Int=muted) { parent.addView(text(message,13f,color).apply { setPadding(0,dp(8),0,dp(8)) }) }
    private fun showLogin() {
        base("FAMILY • CHILD","Welcome back.","Sign in to view your device and pairing options.")
        val c=card()
        c.addView(text("Login",22f,ink,true))
        note(c,"Use your existing child account details.")
        val email=field(c,"Email address","you@example.com")
        val password=field(c,"Password","Your password",true)
        button(c,"Sign in") { authenticate(email.text.toString(),password.text.toString(),false) }
        button(c,"Create a new account",false) { showSignup() }
        note(c,"New here? Create an account first.")
    }
    private fun showSignup() {
        base("FAMILY • CHILD","Create account.","Create your account before entering a parent pairing code.")
        val c=card()
        c.addView(text("Sign up",22f,ink,true))
        note(c,"This app remains visible on your device. No hidden monitoring.")
        val email=field(c,"Email address","you@example.com")
        val password=field(c,"Create password","At least 6 characters",true)
        val confirm=field(c,"Confirm password","Enter the password again",true)
        button(c,"Create child account") {
            if(password.text.toString()!=confirm.text.toString()) { error("Passwords do not match","Enter the same password in both fields."); return@button }
            authenticate(email.text.toString(),password.text.toString(),true)
        }
        button(c,"Already have an account? Login",false) { showLogin() }
    }
    private fun authenticate(email:String,password:String,create:Boolean) {
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.length<6) { error("Check your details","Enter a valid email and a password with at least 6 characters."); return }
        val task=if(create) auth.createUserWithEmailAndPassword(email.trim(),password) else auth.signInWithEmailAndPassword(email.trim(),password)
        task.addOnSuccessListener {
            val uid=auth.currentUser?.uid ?: return@addOnSuccessListener
            db.child("users").child(uid).updateChildren(mapOf("role" to "child","email" to email.trim()))
                .addOnSuccessListener { showDashboard() }
                .addOnFailureListener { showDashboard(); error(if(create) "Account created, profile save blocked" else "Login succeeded, profile update blocked",explainDatabaseError(it.message ?: "Unknown database error")) }
        }.addOnFailureListener { error(if(create) "Sign-up failed" else "Login failed",explainAuthError(it.message ?: "Unknown authentication error")) }
    }
    private fun showDashboard() {
        val uid=auth.currentUser?.uid ?: return
        base("CHILD SPACE","Your device","Pairing is visible and controlled by you.")
        val device=card()
        device.addView(text("YOUR DEVICE ID",12f,teal,true))
        device.addView(text(uid,12f,ink).apply { setPadding(0,dp(8),0,dp(8)); setTextIsSelectable(true) })
        note(device,"Share this ID with your parent if the setup instructions ask for it.")
        val setup=card()
        setup.addView(text("CONNECT TO YOUR FAMILY",12f,teal,true))
        setup.addView(text("Share your Device ID",20f,ink,true).apply { setPadding(0,dp(8),0,dp(4)) })
        note(setup,"Copy the Device ID above and share it directly with your parent. In the Parent App, they can enter it under Family Routine to save preferences. This free MVP does not use 6-digit codes.")
        button(setup,"Copy Device ID") {
            val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
            cm.setPrimaryClip(android.content.ClipData.newPlainText("Child Device ID",uid))
            Toast.makeText(this,"Device ID copied",Toast.LENGTH_SHORT).show()
        }
        button(setup,"Sign out",false) { auth.signOut(); showLogin() }
    }
    private fun loadSettings() {
        val uid=auth.currentUser?.uid ?: return
        db.child("users").child(uid).get().addOnSuccessListener { user ->
            error("Account status","Signed in as: "+(user.child("email").getValue(String::class.java) ?: auth.currentUser?.email.orEmpty())+"\n\nThis starter currently stores account and pairing information. Reading live parent settings will be connected after database access rules are configured.")
        }.addOnFailureListener { error("Could not refresh account",explainDatabaseError(it.message ?: "Unknown database error")) }
    }
    private fun explainDatabaseError(raw:String):String {
        val s=raw.lowercase()
        return if(s.contains("permission_denied") || s.contains("permission denied")) {
            "Firebase rejected this database request (PERMISSION_DENIED).\n\nLikely causes:\n• Realtime Database Rules deny this read/write path.\n• Pairing code is not readable by a signed-in child.\n• The app is pointed at a different Firebase project/database.\n\nFix: Firebase Console → Realtime Database → Rules. Review users/\$, pairingCodes/\$ and families/\$ rules. Allow only the intended signed-in parent/child access; never enable public read/write.\n\nTechnical detail: $raw"
        } else "Firebase could not complete the request. Check internet, signed-in status, Firebase project/database URL, and Realtime Database Rules.\n\nTechnical detail: $raw"
    }
    private fun explainAuthError(raw:String):String {
        val s=raw.lowercase()
        val hint=when {
            s.contains("email address is badly formatted") -> "The email format looks invalid."
            s.contains("wrong-password") || s.contains("invalid-credential") || s.contains("password is invalid") -> "Password may be incorrect. Try again."
            s.contains("email-already-in-use") || s.contains("already in use") -> "This email already has an account. Use Sign in."
            s.contains("network") -> "Network issue. Check your internet connection."
            else -> "Check Firebase Console → Authentication → Sign-in method and enable Email/Password."
        }
        return "$hint\n\nTechnical detail: $raw"
    }
    private fun error(title:String,message:String) {
        android.app.AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton("Got it",null)
            .setNeutralButton("Copy details") { _,_ ->
                val cm=getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                cm.setPrimaryClip(android.content.ClipData.newPlainText(title,"$title\n$message"))
                Toast.makeText(this,"Error details copied",Toast.LENGTH_SHORT).show()
            }.show()
    }
}
