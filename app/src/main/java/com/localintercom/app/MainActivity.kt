package com.localintercom.app
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView;private lateinit var mute:Button
 override fun onCreate(x:Bundle?){super.onCreate(x);val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;padding(48,48,48,48)}
 val h=TextView(this).apply{text="LOCAL INTERCOM";textSize=30f};status=TextView(this).apply{textSize=18f;setPadding(0,50,0,50)}
 mute=Button(this).apply{setOnClickListener{startService(Intent(this@MainActivity,IntercomService::class.java).setAction(IntercomService.ACTION_MUTE));update()}}
 val i=TextView(this).apply{text="Hotspot/Wi‑Fi • Full duplex • Background audio\nΜπορείς να κλειδώσεις την οθόνη αφού ξεκινήσει.";gravity=Gravity.CENTER;setPadding(0,40,0,0)}
 l.addView(h);l.addView(status);l.addView(mute);l.addView(i);setContentView(l)
 if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),7)else startIntercom()}
 override fun onResume(){super.onResume();update()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==7&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startIntercom()}
 private fun startIntercom(){ContextCompat.startForegroundService(this,Intent(this,IntercomService::class.java));update()}
 private fun update(){status.text=if(IntercomService.connected)"🟢  Συνδεδεμένο" else "🔴  Αναζήτηση στο Hotspot…";mute.text=if(IntercomService.muted)"🎙 UNMUTE" else "🎙 MUTE"}
}