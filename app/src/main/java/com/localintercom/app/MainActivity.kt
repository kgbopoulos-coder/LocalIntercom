package com.localintercom.app
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.view.Gravity
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView
 private lateinit var mute:Button
 private val handler=Handler(Looper.getMainLooper())
 private val ticker=object:Runnable{override fun run(){update();handler.postDelayed(this,750)}}

 override fun onCreate(state:Bundle?){
  super.onCreate(state)
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(48,48,48,48)}
  box.addView(TextView(this).apply{text="LOCAL INTERCOM";textSize=30f})
  status=TextView(this).apply{textSize=18f;setPadding(0,50,0,50)}
  mute=Button(this).apply{setOnClickListener{safeService(IntercomService.ACTION_MUTE)}}
  val stop=Button(this).apply{text="STOP INTERCOM";setOnClickListener{safeService(IntercomService.ACTION_STOP)}}
  box.addView(status);box.addView(mute);box.addView(stop)
  box.addView(TextView(this).apply{text="Hotspot / Wi-Fi • Full duplex • Background audio";gravity=Gravity.CENTER;setPadding(0,40,0,0)})
  setContentView(box)
  onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){override fun handleOnBackPressed(){moveTaskToBack(true)}})
  requestPermissionsSafely()
 }
 private fun requestPermissionsSafely(){
  val list=mutableListOf<String>()
  if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)list+=Manifest.permission.RECORD_AUDIO
  if(Build.VERSION.SDK_INT>=33&&ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)list+=Manifest.permission.POST_NOTIFICATIONS
  if(Build.VERSION.SDK_INT>=31&&ContextCompat.checkSelfPermission(this,Manifest.permission.BLUETOOTH_CONNECT)!=PackageManager.PERMISSION_GRANTED)list+=Manifest.permission.BLUETOOTH_CONNECT
  if(list.isEmpty())startIntercom() else ActivityCompat.requestPermissions(this,list.toTypedArray(),7)
 }
 override fun onRequestPermissionsResult(code:Int,p:Array<out String>,g:IntArray){
  super.onRequestPermissionsResult(code,p,g)
  if(code==7&&ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startIntercom()
 }
 private fun startIntercom(){try{ContextCompat.startForegroundService(this,Intent(this,IntercomService::class.java))}catch(_:Exception){status.text="🔴 Δεν ξεκίνησε η υπηρεσία"}}
 private fun safeService(action:String){try{startService(Intent(this,IntercomService::class.java).setAction(action))}catch(_:Exception){}}
 override fun onResume(){super.onResume();handler.post(ticker)}
 override fun onPause(){handler.removeCallbacks(ticker);super.onPause()}
 private fun update(){
  status.text=when{IntercomService.connected->"🟢  Συνδεδεμένο";IntercomService.running->"🟠  Αναζήτηση στο Hotspot…";else->"🔴  Έτοιμο"}
  mute.isEnabled=IntercomService.running
  mute.text=if(IntercomService.muted)"🎙 UNMUTE" else "🎙 MUTE"
 }
}