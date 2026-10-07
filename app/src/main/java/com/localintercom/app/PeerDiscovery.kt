package com.localintercom.app
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class PeerDiscovery(private val found:(InetAddress)->Unit){
 private val running=AtomicBoolean(false)
 private var socket:DatagramSocket?=null
 private val port=40403
 private val magic="LOCAL_INTERCOM_V3"
 private val local=mutableSetOf<String>()

 fun start(){
  if(!running.compareAndSet(false,true))return
  collectLocal()
  socket=DatagramSocket(null).apply{
   reuseAddress=true
   bind(InetSocketAddress(port))
   broadcast=true
   soTimeout=250
  }
  Thread({loop()},"IntercomDiscovery").start()
 }

 private fun loop(){
  val buf=ByteArray(128)
  while(running.get()){
   sendDiscovery()
   val end=System.currentTimeMillis()+700
   while(running.get()&&System.currentTimeMillis()<end){
    try{
     val p=DatagramPacket(buf,buf.size);socket?.receive(p)
     val text=String(p.data,0,p.length)
     if(text.startsWith(magic)&&!local.contains(p.address.hostAddress)){
      // Always answer directly so hotspot client isolation/broadcast quirks do not matter after first sighting.
      try{val reply=magic.toByteArray();socket?.send(DatagramPacket(reply,reply.size,p.address,port))}catch(_:Exception){}
      found(p.address)
     }
    }catch(_:SocketTimeoutException){break}catch(_:Exception){}
   }
   try{Thread.sleep(250)}catch(_:InterruptedException){}
  }
 }

 private fun sendDiscovery(){
  val data=magic.toByteArray()
  val targets=linkedSetOf<InetAddress>()
  try{
   val en=NetworkInterface.getNetworkInterfaces()
   while(en.hasMoreElements()){
    val ni=en.nextElement()
    if(!ni.isUp||ni.isLoopback)continue
    for(ia in ni.interfaceAddresses){
     ia.broadcast?.let{targets.add(it)}
     val a=ia.address
     if(a is Inet4Address&&!a.isLoopbackAddress){
      val b=a.address
      val prefix=(b[0].toInt() and 255).toString()+"."+(b[1].toInt() and 255)+"."+(b[2].toInt() and 255)+"."
      // Android hotspots commonly use /24. Probe likely peer addresses without manual IP.
      for(i in 1..254)if(prefix+i!=a.hostAddress)try{targets.add(InetAddress.getByName(prefix+i))}catch(_:Exception){}
     }
    }
   }
  }catch(_:Exception){}
  try{targets.add(InetAddress.getByName("255.255.255.255"))}catch(_:Exception){}
  for(t in targets)try{socket?.send(DatagramPacket(data,data.size,t,port))}catch(_:Exception){}
 }

 private fun collectLocal(){
  local.clear()
  try{
   val en=NetworkInterface.getNetworkInterfaces()
   while(en.hasMoreElements()){
    val ni=en.nextElement()
    if(!ni.isUp)continue
    val ips=ni.inetAddresses
    while(ips.hasMoreElements())ips.nextElement().hostAddress?.let{local.add(it.substringBefore("%"))}
   }
  }catch(_:Exception){}
 }

 fun stop(){running.set(false);try{socket?.close()}catch(_:Exception){}}
}