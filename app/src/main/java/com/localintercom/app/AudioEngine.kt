package com.localintercom.app
import android.media.*
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean
class AudioEngine(private val peer:InetAddress,private val port:Int=40404){
 private val run=AtomicBoolean(false); private var sock:DatagramSocket?=null; @Volatile var muted=false; private val rate=16000; @Volatile var volumeGain=1.6f
 fun start(){if(!run.compareAndSet(false,true))return;sock=DatagramSocket(port).apply{soTimeout=1000};Thread{rx()}.start();Thread{tx()}.start()}
 fun stop(){run.set(false);sock?.close()}
 private fun tx(){val m=AudioRecord.getMinBufferSize(rate,16,2);val r=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,rate,16,2,maxOf(m,2048));val b=ByteArray(640);r.startRecording();try{while(run.get()){val n=r.read(b,0,b.size);if(n>0&&!muted)sock?.send(DatagramPacket(b,n,peer,port))}}finally{r.release()}}
 private fun rx(){val m=AudioTrack.getMinBufferSize(rate,4,2);val t=AudioTrack(AudioManager.STREAM_VOICE_CALL,rate,4,2,maxOf(m,2048),AudioTrack.MODE_STREAM);val b=ByteArray(2048);t.play();try{while(run.get())try{val p=DatagramPacket(b,b.size);sock?.receive(p);if(p.address==peer){ var i=0; val gain=volumeGain; while(i+1<p.length){val sample=((p.data[i].toInt() and 255) or (p.data[i+1].toInt() shl 8)).toShort().toInt();val v=(sample*gain).toInt().coerceIn(-32768,32767);p.data[i]=(v and 255).toByte();p.data[i+1]=(v shr 8).toByte();i+=2};t.write(p.data,0,p.length)}}catch(_:SocketTimeoutException){}}finally{t.release()}}
}