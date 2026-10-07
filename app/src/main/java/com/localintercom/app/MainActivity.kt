package com.localintercom.app
import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity:AppCompatActivity(){
 private lateinit var status:TextView;private lateinit var mute:Button;private var d:PeerDiscovery?=null;private var e:AudioEngine?=null
 override fun onCreate(x:Bundle?){super.onCreate(x);val l=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;padding(48,48,48,48)}
 val h=TextView(this).apply{text="LOCAL INTERCOM";textSize=30f};status=TextView(this).apply{text="🔴  Αναζήτηση στο Hotspot…";textSize=18f;setPadding(0,50,0,50)}
 mute=Button(this).apply{text="🎙 MUTE";isEnabled=false;setOnClickListener{e?.let{it.muted=!it.muted;text=if(it.muted)"🎙 UNMUTE" else "🎙 MUTE"}}}
 val i=TextView(this).apply{text="1. Άνοιξε Hotspot στο ένα κινητό\n2. Σύνδεσε το δεύτερο\n3. Άνοιξε την εφαρμογή και στα δύο";gravity=Gravity.CENTER}
 l.addView(h);l.addView(status);l.addView(mute);l.addView(i);setContentView(l);(getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode=AudioManager.MODE_IN_COMMUNICATION
 if(ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO),7)else go()}
 override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==7&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)go()}
 private fun go(){d=PeerDiscovery{ip->runOnUiThread{if(e==null){status.text="🟢  Συνδεδεμένο";mute.isEnabled=true;e=AudioEngine(ip).also{it.start()}}}}.also{it.start()}}
 override fun onDestroy(){d?.stop();e?.stop();super.onDestroy()}
}