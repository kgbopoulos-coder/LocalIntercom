package com.localintercom.app
import android.app.*
import android.content.*
import android.media.*
import android.os.*
import androidx.core.app.NotificationCompat

class IntercomService:Service(){
 companion object {
  const val ACTION_VOLUME="com.localintercom.VOLUME"; const val EXTRA_VOLUME="volume"; const val ACTION_MUTE="com.localintercom.MUTE"; const val ACTION_STOP="com.localintercom.STOP"; const val CHANNEL="intercom"
  @Volatile var volumePercent=160; @Volatile var running=false; @Volatile var connected=false; @Volatile var muted=false
 }
 private var discovery:PeerDiscovery?=null; private var engine:AudioEngine?=null
 private lateinit var audio:AudioManager
 private var audioCallback:AudioDeviceCallback?=null

 override fun onCreate(){
  super.onCreate();running=true
  volumePercent=getSharedPreferences("intercom",MODE_PRIVATE).getInt("volume",160).coerceIn(0,250)
  audio=getSystemService(AUDIO_SERVICE) as AudioManager
  audio.mode=AudioManager.MODE_IN_COMMUNICATION
  registerAudioRouting()
  routeCommunicationAudio()
  getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,"Local Intercom",NotificationManager.IMPORTANCE_LOW))
  startForeground(41,note("Αναζήτηση στο Hotspot…"))
  try{startDiscovery()}catch(_:Exception){connected=false;refresh()}
 }
 override fun onStartCommand(i:Intent?,f:Int,id:Int):Int{
  when(i?.action){
   ACTION_MUTE->{muted=!muted;engine?.muted=muted;refresh()}
   ACTION_VOLUME->{volumePercent=i.getIntExtra(EXTRA_VOLUME,160).coerceIn(0,250);getSharedPreferences("intercom",MODE_PRIVATE).edit().putInt("volume",volumePercent).apply();engine?.volumeGain=volumePercent/100f}
   ACTION_STOP->{stopSelf();return START_NOT_STICKY}
  }
  return START_STICKY
 }
 private fun registerAudioRouting(){
  audioCallback=object:AudioDeviceCallback(){
   override fun onAudioDevicesAdded(added:Array<out AudioDeviceInfo>){routeCommunicationAudio()}
   override fun onAudioDevicesRemoved(removed:Array<out AudioDeviceInfo>){routeCommunicationAudio()}
  }
  try{audio.registerAudioDeviceCallback(audioCallback,null)}catch(_:Exception){}
 }
 private fun routeCommunicationAudio(){
  if(Build.VERSION.SDK_INT>=31){
   try{
    val devices=audio.availableCommunicationDevices
    val preferred=devices.firstOrNull{it.type==AudioDeviceInfo.TYPE_BLE_HEADSET}
     ?:devices.firstOrNull{it.type==AudioDeviceInfo.TYPE_BLUETOOTH_SCO}
     ?:devices.firstOrNull{it.type==AudioDeviceInfo.TYPE_WIRED_HEADSET}
     ?:devices.firstOrNull{it.type==AudioDeviceInfo.TYPE_USB_HEADSET}
    if(preferred!=null && audio.communicationDevice?.id!=preferred.id) audio.setCommunicationDevice(preferred)
   }catch(_:SecurityException){
    // Bluetooth permission is optional; Android keeps the current/default route.
   }catch(_:Exception){}
  }
 }
 private fun startDiscovery(){
  try{discovery?.stop()}catch(_:Exception){}
  discovery=PeerDiscovery{ip->
   if(engine==null){
    try{
     routeCommunicationAudio()
     engine=AudioEngine(ip).also{it.muted=muted;it.volumeGain=volumePercent/100f;it.start()}
     connected=true;refresh()
    }catch(_:Exception){connected=false;engine?.stop();engine=null;refresh()}
   }
  }
  try{discovery?.start()}catch(_:Exception){discovery=null;connected=false;refresh()}
 }
 private fun note(text:String):Notification{
  val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val mi=PendingIntent.getService(this,1,Intent(this,IntercomService::class.java).setAction(ACTION_MUTE),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  val st=PendingIntent.getService(this,2,Intent(this,IntercomService::class.java).setAction(ACTION_STOP),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  return NotificationCompat.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_btn_speak_now).setContentTitle("Local Intercom").setContentText(text).setOngoing(true).setContentIntent(open).addAction(0,if(muted)"Unmute" else "Mute",mi).addAction(0,"Stop",st).build()
 }
 private fun refresh(){getSystemService(NotificationManager::class.java).notify(41,note(if(connected)"Connected 🟢" else "Searching…"))}
 override fun onDestroy(){
  running=false;connected=false;discovery?.stop();engine?.stop()
  try{audioCallback?.let{audio.unregisterAudioDeviceCallback(it)}}catch(_:Exception){}
  if(Build.VERSION.SDK_INT>=31)try{audio.clearCommunicationDevice()}catch(_:Exception){}
  audio.mode=AudioManager.MODE_NORMAL
  super.onDestroy()
 }
 override fun onBind(i:Intent?):IBinder?=null
}