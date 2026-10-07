package com.localintercom.app
import android.media.*
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean
class AudioEngine(private val peer:InetAddress,private val port:Int=40404){
 private val run=AtomicBoolean(false); private var sock:DatagramSocket?=null; @Volatile var muted=false; private val rate=16000
 fun start(){if(!run.compareAndSet(false,true))return;sock=DatagramSocket(port).apply{soTimeout=1000};Thread{rx()}.start();Thread{tx()}.start()}
 fun stop(){run.set(false);sock?.close()}
 private fun tx(){val m=AudioRecord.getMinBufferSize(rate,16,2);val r=AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION,rate,16,2,maxOf(m,2048));val b=ByteArray(640);r.startRecording();try{while(run.get()){val n=r.read(b,0,b.size);if(n>0&&!muted)sock?.send(DatagramPacket(b,n,peer,port))}}finally{r.release()}}
 private fun rx(){val m=AudioTrack.getMinBufferSize(rate,4,2);val t=AudioTrack(AudioManager.STREAM_VOICE_CALL,rate,4,2,maxOf(m,2048),AudioTrack.MODE_STREAM);val b=ByteArray(2048);t.play();try{while(run.get())try{val p=DatagramPacket(b,b.size);sock?.receive(p);t.write(p.data,0,p.length)}catch(_:SocketTimeoutException){}}finally{t.release()}}
}