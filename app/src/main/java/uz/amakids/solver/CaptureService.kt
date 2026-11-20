package uz.amakids.solver

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class CaptureService: Service() {
 private val handler=Handler(Looper.getMainLooper())
 private val recognizer=TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
 private var projection:MediaProjection?=null
 private var reader:ImageReader?=null
 private var virtualDisplay:android.hardware.display.VirtualDisplay?=null
 private var windowManager:WindowManager?=null
 private var overlay:View?=null
 private var resultText:TextView?=null
 private var total=0L
 private var lastToken=""
 private var lastSeen=0L
 private var interval=100L
 private var busy=false
 private var lastCapture=0L
 private var initialIgnored=false
 private val tokenRegex=Regex("([+\\-−])\\s*(\\d{1,5})")
 override fun onBind(intent:Intent?):IBinder?=null
 override fun onCreate() { super.onCreate()
  val channel=NotificationChannel("capture","AMAKIDS kuzatuvi",NotificationManager.IMPORTANCE_LOW)
  (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
  val notification=Notification.Builder(this,"capture").setContentTitle("AMAKIDS Solver ishlamoqda").setContentText("Ekrandagi sonlar kuzatilmoqda").setSmallIcon(android.R.drawable.ic_menu_view).build()
  startForeground(1,notification)
  showOverlay()
 }
 private fun showOverlay() {
  windowManager=getSystemService(WINDOW_SERVICE) as WindowManager
  val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(12,8,12,8); setBackgroundColor(0xDD10172B.toInt()) }
  resultText=TextView(this).apply { text="Jami: 0"; textSize=19f; setTextColor(-1); layout.addView(this) }
  Button(this).apply { text="RESET"; setOnClickListener { total=0;lastToken="";lastSeen=0;initialIgnored=false;resultText?.text="Jami: 0" }; layout.addView(this) }
  val params=WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,android.graphics.PixelFormat.TRANSLUCENT).apply { gravity=Gravity.TOP or Gravity.START; x=5;y=120 }
  windowManager?.addView(layout,params);overlay=layout
 }
 override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
  if(projection!=null) return START_NOT_STICKY
  interval=intent?.getLongExtra("interval",100L)?:100L
  val code=intent?.getIntExtra("resultCode",Activity.RESULT_CANCELED)?:Activity.RESULT_CANCELED
  @Suppress("DEPRECATION") val data=if(Build.VERSION.SDK_INT>=33) intent?.getParcelableExtra("resultData",Intent::class.java) else intent?.getParcelableExtra("resultData")
  if(data==null || code!=Activity.RESULT_OK){stopSelf();return START_NOT_STICKY}
  val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
  projection=mgr.getMediaProjection(code,data)
  projection?.registerCallback(object:MediaProjection.Callback(){ override fun onStop(){handler.post { stopSelf() }} },handler)
  val metrics=resources.displayMetrics
  reader=ImageReader.newInstance(metrics.widthPixels,metrics.heightPixels,android.graphics.PixelFormat.RGBA_8888,2)
  virtualDisplay=projection?.createVirtualDisplay("AMAKIDS",metrics.widthPixels,metrics.heightPixels,metrics.densityDpi,DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,reader?.surface,null,handler)
  reader?.setOnImageAvailableListener({ ir ->
   val frame=ir.acquireLatestImage()?:return@setOnImageAvailableListener
   val now=SystemClock.elapsedRealtime()
   if(busy || now-lastCapture<interval){frame.close();return@setOnImageAvailableListener}
   lastCapture=now;busy=true
   try {
    val plane=frame.planes[0];val buffer=plane.buffer
    val rowPadding=plane.rowStride-plane.pixelStride*frame.width
    val bitmap=Bitmap.createBitmap(frame.width+rowPadding/plane.pixelStride,frame.height,Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(buffer)
    // Crop to central display region: number is normally in the center card.
    val left=(frame.width*0.12).toInt();val top=(frame.height*0.20).toInt()
    val w=(frame.width*0.76).toInt();val h=(frame.height*0.35).toInt()
    val cropped=Bitmap.createBitmap(bitmap,left,top,w,h)
    bitmap.recycle()
    recognizer.process(InputImage.fromBitmap(cropped,0)).addOnSuccessListener { result ->
     val candidates=result.textBlocks.flatMap { it.lines }.mapNotNull { line ->
      val cleaned=line.text.replace('＋','+').replace('－','-').replace('−','-').replace(' ','')
      val match=tokenRegex.find(cleaned)
      if(match!=null) Triple(match.value,match.groupValues[1],match.groupValues[2].toLongOrNull()?:0L) else null
     }
     // Choose largest visible signed number, ignoring small UI elements.
     val token=candidates.maxByOrNull { it.first.length }
     if(token!=null) {
      val normalized=token.second+token.third
      // Avoid re-counting the same number on repeated video frames.
      // Identical consecutive flashes cannot be distinguished reliably.
      if(normalized!=lastToken) {
       total+=if(token.second=="+")token.third else -token.third
       lastToken=normalized;lastSeen=SystemClock.elapsedRealtime();initialIgnored=true
       resultText?.text="Jami: $total | ${token.first}"
      }
     } else {
      // A blank transition separates two identical values when detected.
      if(SystemClock.elapsedRealtime()-lastSeen>interval*2) lastToken=""
     }
    }.addOnCompleteListener { cropped.recycle();busy=false }
   } catch(_:Exception){busy=false} finally {frame.close()}
  },handler)
  return START_NOT_STICKY
 }
 override fun onDestroy(){reader?.setOnImageAvailableListener(null,null);virtualDisplay?.release();reader?.close();projection?.stop();recognizer.close();overlay?.let {windowManager?.removeView(it)};super.onDestroy()}
}
