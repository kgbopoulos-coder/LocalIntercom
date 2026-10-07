package com.localintercom.app
import android.app.*
import android.content.*
import android.media.AudioManager
import android.os.IBinder
import androidx.core.app.NotificationCompat
class IntercomService:Service(){
 companion object { const val ACTION_MUTE="com.localintercom.MUTE"; const val ACTION_STOP="com.localintercom.STOP"; const val CHANNEL="intercom"; @Volatile var connected=false; @Volatile var muted=false }
 private var discovery:PeerDiscovery?=null; private var engine:AudioEngine?=null
 override fun onCreate(){super.onCreate();(getSystemService(AUDIO_SERVICE) as AudioManager).mode=AudioManager.MODE_IN_COMMUNICATION
  val ch=NotificationChannel(CHANNEL,"Local Intercom",NotificationManager.IMPORTANCE_LOW);getSystemService(NotificationManager::class.java).createNotificationChannel(ch);startForeground(41,note("Αναζήτηση στο Hotspot…"));startDiscovery()}
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int { when(i?.action){ACTION_MUTE->{muted=!muted;engine?.muted=muted;refresh()};ACTION_STOP->stopSelf()};return START_STICKY }
 private fun startDiscovery(){discovery=PeerDiscovery{ip->if(engine==null){connected=true;engine=AudioEngine(ip).also{it.muted=muted;it.start()};refresh()}}.also{it.start()}}
 private fun note(text:String):Notification{
  val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val mi=PendingIntent.getService(this,1,Intent(this,IntercomService::class.java).setAction(ACTION_MUTE),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val st=PendingIntent.getService(this,2,Intent(this,IntercomService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Local Intercom").setContentText(text).setOngoing(true).setContentIntent(open).addAction(0,if(muted)"Unmute" else "Mute",mi).addAction(0,"Stop",st).build()}
 private fun refresh(){getSystemService(NotificationManager::class.java).notify(41,note(if(connected)"Connected 🟢" else "Searching…"))}
 override fun onDestroy(){connected=false;discovery?.stop();engine?.stop();super.onDestroy()}
 override fun onBind(i:Intent?):IBinder?=null
}