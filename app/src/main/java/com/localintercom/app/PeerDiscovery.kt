package com.localintercom.app
import java.net.*
import java.util.concurrent.atomic.AtomicBoolean

class PeerDiscovery(private val found:(InetAddress)->Unit){
 private val running=AtomicBoolean(false)
 private var server:ServerSocket?=null
 private val tcpPort=40405
 private val hello="LOCAL_INTERCOM_TCP_V1"

 fun start(){
  if(!running.compareAndSet(false,true))return
  Thread({acceptLoop()},"IntercomAccept").start()
  Thread({connectLoop()},"IntercomConnect").start()
 }

 // Every phone listens. The hotspot client also tries the subnet gateway first.
 // Whichever TCP connection succeeds gives us the peer IP reliably; UDP is then used only for audio.
 private fun acceptLoop(){
  try{
   server=ServerSocket().apply{reuseAddress=true;bind(InetSocketAddress(tcpPort))}
   while(running.get()){
    try{
     val c=server?.accept()?:break
     c.soTimeout=1200
     val line=c.getInputStream().bufferedReader().readLine()
     if(line==hello){
      c.getOutputStream().bufferedWriter().apply{write(hello);newLine();flush()}
      found(c.inetAddress)
     }
     c.close()
    }catch(_:Exception){}
   }
  }catch(_:Exception){}
 }

 private fun connectLoop(){
  while(running.get()){
   val targets=targets()
   for(ip in targets){
    if(!running.get())break
    try{
     val c=Socket()
     c.connect(InetSocketAddress(ip,tcpPort),90)
     c.soTimeout=700
     c.getOutputStream().bufferedWriter().apply{write(hello);newLine();flush()}
     val reply=c.getInputStream().bufferedReader().readLine()
     if(reply==hello){found(c.inetAddress);c.close();Thread.sleep(1500);break}
     c.close()
    }catch(_:Exception){}
   }
   try{Thread.sleep(700)}catch(_:InterruptedException){}
  }
 }

 private fun targets():List<InetAddress>{
  val out=LinkedHashSet<InetAddress>()
  val mine=HashSet<String>()
  try{
   val en=NetworkInterface.getNetworkInterfaces()
   while(en.hasMoreElements()){
    val ni=en.nextElement()
    if(!ni.isUp||ni.isLoopback)continue
    val addrs=ni.inetAddresses
    while(addrs.hasMoreElements()){
     val a=addrs.nextElement()
     if(a is Inet4Address&&!a.isLoopbackAddress){
      mine.add(a.hostAddress?:"")
      val b=a.address
      val prefix=(b[0].toInt() and 255).toString()+"."+(b[1].toInt() and 255)+"."+(b[2].toInt() and 255)+"."
      // Hotspot hosts are commonly .1; try likely host addresses first, then the /24.
      for(i in listOf(1,254,2))try{out.add(InetAddress.getByName(prefix+i))}catch(_:Exception){}
      for(i in 1..254)try{out.add(InetAddress.getByName(prefix+i))}catch(_:Exception){}
     }
    }
   }
  }catch(_:Exception){}
  return out.filter{!mine.contains(it.hostAddress)}
 }

 fun stop(){running.set(false);try{server?.close()}catch(_:Exception){}}
}