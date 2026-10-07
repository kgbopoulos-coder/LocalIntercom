package com.localintercom.app
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean
class PeerDiscovery(private val found:(InetAddress)->Unit){
 private val run=AtomicBoolean(false);private var s:DatagramSocket?=null;private val port=40403
 fun start(){if(!run.compareAndSet(false,true))return;s=DatagramSocket(port).apply{broadcast=true;soTimeout=500};Thread{val msg="LOCAL_INTERCOM_V1".toByteArray();val b=ByteArray(64);while(run.get()){try{s?.send(DatagramPacket(msg,msg.size,InetAddress.getByName("255.255.255.255"),port))}catch(_:Exception){};try{val p=DatagramPacket(b,b.size);s?.receive(p);if(String(p.data,0,p.length)=="LOCAL_INTERCOM_V1")found(p.address)}catch(_:Exception){}}}.start()}
 fun stop(){run.set(false);s?.close()}
}