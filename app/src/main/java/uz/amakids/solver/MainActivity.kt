package uz.amakids.solver

import android.app.*
import android.content.*
import android.media.projection.MediaProjectionManager
import android.os.*
import android.provider.Settings
import android.net.Uri
import android.view.*
import android.widget.*

class MainActivity : Activity() {
 private val requestCapture = 301
 private lateinit var status: TextView
 private lateinit var speed: EditText
 private val mgr by lazy { getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager }
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(32,40,32,20) }
  fun add(s:String) = TextView(this).apply { text=s; textSize=18f; root.addView(this) }
  add("AMAKIDS Solver — Android")
  add("Ekrandagi sonlar joylashgan sohani OCR orqali kuzatadi. Dastlabki belgisiz 1 hisoblanmaydi.")
  add("Kadrlar orasidagi vaqt (ms), minimum 100:")
  speed=EditText(this).apply { setText("100"); inputType=2; root.addView(this) }
  status=TextView(this).apply { text="Boshlashga tayyor"; textSize=18f; root.addView(this) }
  val start=Button(this).apply { text="EKRANNI KUZATISHNI BOSHLASH"; setOnClickListener { if(!Settings.canDrawOverlays(this@MainActivity)) { startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName"))); status.text="Ustida ko‘rsatish ruxsatini bering, keyin qayta bosing" } else startActivityForResult(mgr.createScreenCaptureIntent(),requestCapture) }; root.addView(this) }
  Button(this).apply { text="TO‘XTATISH"; setOnClickListener { stopService(Intent(this@MainActivity,CaptureService::class.java)); status.text="To‘xtatildi" }; root.addView(this) }
  add("Muhim: yangi misol boshlanishida suzuvchi oynadagi RESET tugmasini bosing. Juda tez almashadigan sonlar o'tkazib yuborilishi mumkin.")
  setContentView(ScrollView(this).apply { addView(root) })
 }
 @Deprecated("Android legacy callback") override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==requestCapture && resultCode==RESULT_OK && data!=null) {
   val ms=(speed.text.toString().toLongOrNull()?:100L).coerceIn(100L,10000L)
   val intent=Intent(this,CaptureService::class.java).putExtra("resultCode",resultCode).putExtra("resultData",data).putExtra("interval",ms)
   if(Build.VERSION.SDK_INT>=26) startForegroundService(intent) else startService(intent)
   status.text="Ishlamoqda — AMAKIDS sahifasini oching"
  }
 }
}
