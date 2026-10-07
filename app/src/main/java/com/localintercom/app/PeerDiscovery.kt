package com.localintercom.app
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class PeerDiscovery(private val found:(InetAddress)->Unit){
 private val running=AtomicBoolean(false)
 private var socket:DatagramSocket?=null
 private val port=40403
 private val magic="LOCAL_INTERCOM_V2"

 fun start(){
  if(!running.compareAndSet(false,true)) return
  socket=DatagramSocket(null).apply{reuseAddress=true;bind(InetSocketAddress(port));broadcast=true;soTimeout=350}
  Thread({
   val msg=magic.toByteArray()
   val buf=ByteArray(128)
   while(running.get()){
    val targets=broadcastTargets()
    for(target in targets) try{socket?.send(DatagramPacket(msg,msg.size,target,port))}catch(_:Exception){}
    val until=System.currentTimeMillis()+500
    while(running.get() && System.currentTimeMillis()<until){
     try{
      val p=DatagramPacket(buf,buf.size);socket?.receive(p)
      if(String(p.data,0,p.length)==magic && !isLocal(p.address)) found(p.address)
     }catch(_:SocketTimeoutException){break}catch(_:Exception){}
    }
    try{Thread.sleep(250)}catch(_:InterruptedException){}
   }
  },"IntercomDiscovery").start()
 }

 private fun broadcastTargets():Set<InetAddress>{
  val out=linkedSetOf<InetAddress>()
  try{
   val en=NetworkInterface.getNetworkInterfaces()
   while(en.hasMoreElements()){
    val n=en.nextElement()
    if(!n.isUp || n.isLoopback) continue
    for(i in n.interfaceAddresses) i.broadcast?.let{out.add(it)}
   }
  }catch(_:Exception){}
  try{out.add(InetAddress.getByName("255.255.255.255"))}catch(_:Exception){}
  return out
 }

 private fun isLocal(a:InetAddress):Boolean=try{
  val en=NetworkInterface.getNetworkInterfaces()
  var local=false
  while(en.hasMoreElements()&&!local){
   val n=en.nextElement()
   local=n.inetAddresses.toList().any{it==a}
  }
  local
 }catch(_:Exception){false}

 fun stop(){running.set(false);socket?.close()}
}