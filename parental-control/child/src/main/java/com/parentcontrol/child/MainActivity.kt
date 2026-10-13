package com.parentcontrol.child

import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseDatabase.getInstance().reference }
    private lateinit var root: LinearLayout
    private val navy = Color.rgb(18,29,57)
    private val teal = Color.rgb(14,165,160)
    private val ink = Color.rgb(30,41,59)
    private val muted = Color.rgb(100,116,139)
    private val canvas = Color.rgb(245,247,252)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = navy
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        if (auth.currentUser == null) showLogin() else showDashboard()
    }
    private fun dp(v:Int) = (v*resources.displayMetrics.density).toInt()
    private fun shape(color:Int,radius:Int=20,stroke:Int=0) = GradientDrawable().apply {
        setColor(color); cornerRadius=dp(radius).toFloat(); if(stroke!=0) setStroke(dp(1),stroke)
    }
    private fun text(value:String,size:Float,color:Int,bold:Boolean=false)=TextView(this).apply {
        text=value; textSize=size; setTextColor(color); if(bold) setTypeface(typeface,Typeface.BOLD)
    }
    private fun base(eyebrow:String,title:String,subtitle:String) {
        root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(20),dp(20),dp(28)); setBackgroundColor(canvas) }
        setContentView(ScrollView(this).apply { isFillViewport=true; addView(root) })
        val hero=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(22),dp(22),dp(24)); background=shape(navy,26) }
        hero.addView(text("✦  $eyebrow",12f,Color.rgb(117,232,219),true))
        hero.addView(text(title,28f,Color.WHITE,true).apply { setPadding(0,dp(10),0,dp(8)) })
        hero.addView(text(subtitle,14f,Color.rgb(210,218,235)))
        root.addView(hero,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(18) })
    }
    private fun card()=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(17),dp(17),dp(17),dp(17))
        background=shape(Color.WHITE,22,Color.rgb(229,234,244)); elevation=dp(2).toFloat()
    }.also { root.addView(it,LinearLayout.LayoutParams(-1,-2).apply { bottomMargin=dp(13) }) }
    private fun note(parent:LinearLayout,msg:String) { parent.addView(text(msg,13f,muted).apply { setPadding(0,dp(7),0,dp(7)) }) }
    private fun field(parent:LinearLayout,label:String,hint:String,password:Boolean=false):EditText {
        parent.addView(text(label,13f,ink,true),LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(10); bottomMargin=dp(6) })
        return EditText(this).apply {
            this.hint=hint; textSize=15f; setTextColor(ink); setPadding(dp(13),dp(12),dp(13),dp(12))
            background=shape(Color.rgb(247,249,253),14,Color.rgb(224,230,240))
            inputType=if(password) 129 else if(label.contains("Email",true)) 33 else 1
            parent.addView(this,LinearLayout.LayoutParams(-1,dp(52)))
        }
    }
    private fun button(parent:LinearLayout,label:String,primary:Boolean=true,action:()->Unit) {
        parent.addView(TextView(this).apply {
            text=label; textSize=14f; gravity=Gravity.CENTER; setTypeface(typeface,Typeface.BOLD)
            setTextColor(if(primary) Color.WHITE else teal); background=shape(if(primary) teal else Color.rgb(225,250,247),15)
            setPadding(dp(12),dp(14),dp(12),dp(14)); isClickable=true; isFocusable=true; setOnClickListener { action() }
        },LinearLayout.LayoutParams(-1,-2).apply { topMargin=dp(9) })
    }
    private fun showLogin() {
        base("FAMILY • CHILD","Welcome back","Sign in to connect your device with your family.")
        val c=card(); c.addView(text("Child account",21f,ink,true))
        val email=field(c,"Email address","you@example.com"); val pass=field(c,"Password","At least 6 characters",true)
        button(c,"Sign in") { authenticate(email.text.toString(),pass.text.toString(),false) }
        button(c,"Create new account",false) { showSignup() }
        note(c,"Your device stays visible to you. Permissions are requested transparently.")
    }
    private fun showSignup() {
        base("FAMILY • CHILD","Create account","Set up your child profile to begin.")
        val c=card(); val email=field(c,"Email address","you@example.com")
        val pass=field(c,"Create password","At least 6 characters",true); val confirm=field(c,"Confirm password","Enter it again",true)
        button(c,"Create child account") {
            if(pass.text.toString()!=confirm.text.toString()) { message("Passwords do not match","Enter the same password twice."); return@button }
            authenticate(email.text.toString(),pass.text.toString(),true)
        }
        button(c,"Back to login",false) { showLogin() }
    }
    private fun authenticate(email:String,password:String,create:Boolean) {
        if(!android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() || password.length<6) {
            message("Check your details","Enter a valid email and a password with at least 6 characters."); return
        }
        val task=if(create) auth.createUserWithEmailAndPassword(email.trim(),password) else auth.signInWithEmailAndPassword(email.trim(),password)
        task.addOnSuccessListener {
            val uid=auth.currentUser?.uid ?: return@addOnSuccessListener
            db.child("users").child(uid).updateChildren(mapOf("role" to "child","email" to email.trim()))
                .addOnSuccessListener { showDashboard() }
                .addOnFailureListener { showDashboard(); message("Profile sync issue","Login succeeded, but Firebase rejected the profile update. Check database rules.") }
        }.addOnFailureListener { message(if(create) "Sign-up failed" else "Login failed",it.message ?: "Please try again.") }
    }
    private fun showDashboard() {
        val uid=auth.currentUser?.uid ?: return
        base("CHILD SPACE","Your device","Family setup • permission centre • digital wellbeing")
        val device=card(); device.addView(text("DEVICE CONNECTION",12f,teal,true))
        device.addView(text(auth.currentUser?.email ?: "Signed in",19f,ink,true).apply { setPadding(0,dp(7),0,dp(4)) })
        note(device,"Device ID"); device.addView(text(uid,11f,muted).apply { setTextIsSelectable(true) })
        button(device,"Copy Device ID") {
            val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Child Device ID",uid)); Toast.makeText(this,"Device ID copied",Toast.LENGTH_SHORT).show()
        }
        val setup=card(); setup.addView(text("01  •  FIRST-TIME SETUP",12f,teal,true))
        setup.addView(text("Permission centre",21f,ink,true).apply { setPadding(0,dp(6),0,dp(3)) })
        note(setup,"Android may require separate approval for special access. If a permission is revoked, open this page again.")
        button(setup,"Enable app usage access") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
        button(setup,"Open app settings",false) { startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:$packageName"))) }
        button(setup,"Accessibility settings (optional)",false) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        val usage=card(); usage.addView(text("02  •  DIGITAL WELLBEING",12f,teal,true))
        usage.addView(text("App usage today",21f,ink,true).apply { setPadding(0,dp(6),0,dp(4)) })
        note(usage,if(hasUsageAccess()) "Usage access is enabled." else "Enable usage access in Android Settings, then return here.")
        button(usage,"Refresh usage report") { showUsageReport() }
        val family=card(); family.addView(text("03  •  FAMILY LINK",12f,teal,true))
        family.addView(text("Pair with parent",20f,ink,true).apply { setPadding(0,dp(6),0,dp(4)) })
        note(family,"Share your Device ID with your parent so they can enter it in Family Routine.")
        button(family,"Copy Device ID") {
            val cm=getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("Child Device ID",uid)); Toast.makeText(this,"Device ID copied",Toast.LENGTH_SHORT).show()
        }
        button(family,"Sign out",false) { auth.signOut(); showLogin() }
        note(root,"This version reports app usage only after you enable Usage Access. It does not secretly capture your screen.")
    }
    private fun hasUsageAccess():Boolean {
        val ops=getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode=if(android.os.Build.VERSION.SDK_INT>=29)
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),packageName)
        else @Suppress("DEPRECATION") ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),packageName)
        return mode==AppOpsManager.MODE_ALLOWED
    }
    private fun showUsageReport() {
        if(!hasUsageAccess()) { message("Usage access needed","Enable this app in Android Usage Access settings, then return and refresh."); return }
        val mgr=getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val cal=java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY,0); cal.set(java.util.Calendar.MINUTE,0); cal.set(java.util.Calendar.SECOND,0); cal.set(java.util.Calendar.MILLISECOND,0)
        val stats=mgr.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,cal.timeInMillis,System.currentTimeMillis())
            .filter { it.totalTimeInForeground>0 }.sortedByDescending { it.totalTimeInForeground }.take(12)
        val body=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(20),dp(16),dp(20),dp(16)) }
        body.addView(text("Today's app usage",21f,ink,true))
        body.addView(text(java.text.SimpleDateFormat("EEE, dd MMM",Locale.getDefault()).format(Date()),12f,muted).apply { setPadding(0,dp(4),0,dp(12)) })
        if(stats.isEmpty()) body.addView(text("No usage records available yet. Use some apps and refresh.",14f,muted))
        stats.forEach { item ->
            val name=try { packageManager.getApplicationLabel(packageManager.getApplicationInfo(item.packageName,0)).toString() } catch(_:Exception) { item.packageName }
            val mins=item.totalTimeInForeground/60000
            body.addView(LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL; setPadding(0,dp(9),0,dp(9))
                addView(text(name,14f,ink,true),LinearLayout.LayoutParams(0,-2,1f)); addView(text("$mins min",13f,teal,true))
            })
            body.addView(View(this).apply { setBackgroundColor(Color.rgb(235,239,246)) },LinearLayout.LayoutParams(-1,dp(1)))
        }
        android.app.AlertDialog.Builder(this).setView(body).setPositiveButton("Done",null).show()
    }
    private fun message(title:String,body:String) { android.app.AlertDialog.Builder(this).setTitle(title).setMessage(body).setPositiveButton("OK",null).show() }
}