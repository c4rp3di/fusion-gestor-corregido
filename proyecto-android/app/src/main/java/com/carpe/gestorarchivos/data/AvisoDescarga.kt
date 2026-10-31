package com.carpe.gestorarchivos.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.carpe.gestorarchivos.ActividadExterna
import com.carpe.gestorarchivos.R
import java.io.File
import java.util.Collections

/**
 * Avisa con una notificación («Guardado · toca para abrir») cuando otra app termina de guardar
 * un archivo en el gestor (descargas del navegador, asistentes, exportaciones…).
 */
object AvisoDescarga {

    private const val CANAL = "descargas_guardadas"

    /** Archivos creados por «Guardar como…» que aún esperan a que la otra app termine de escribirlos. */
    private val pendientes: MutableSet<String> = Collections.synchronizedSet(HashSet())

    fun esperar(f: File) { pendientes.add(f.absolutePath) }

    fun estaEsperando(f: File) = pendientes.contains(f.absolutePath)

    fun terminado(f: File) { pendientes.remove(f.absolutePath) }

    private var barraActual: View? = null
    private var ultimoAviso: String? = null
    private var ultimoAvisoMs = 0L

    fun notificar(ctx: Context, f: File) {
        if (!f.isFile || f.length() == 0L) return
        val app = ctx.applicationContext
        Atajos.ultimaDescarga(app, f)
        val ahora = System.currentTimeMillis()
        if (f.absolutePath == ultimoAviso && ahora - ultimoAvisoMs < 3000) return // cierres repetidos del mismo archivo
        ultimoAviso = f.absolutePath
        ultimoAvisoMs = ahora
        // Con permiso «Mostrar sobre otras apps»: barra inferior con «Abrir» (como la de «Descargando…»)
        if (Settings.canDrawOverlays(app)) {
            Handler(Looper.getMainLooper()).post {
                if (!mostrarBarra(app, f)) notificacion(app, f)
            }
            return
        }
        notificacion(app, f)
    }

    private fun abrirIntent(ctx: Context, f: File) = Intent(Intent.ACTION_VIEW).apply {
        setClass(ctx, ActividadExterna::class.java)
        setDataAndType(ProveedorDocumentos.uriDocumento(ctx, f), ArchivoItem.adivinarMime(f.name))
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }

    private fun mostrarBarra(ctx: Context, f: File): Boolean {
        return try {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            quitarBarra(ctx)
            val d = ctx.resources.displayMetrics.density
            fun dp(v: Int) = (v * d).toInt()

            val fondo = GradientDrawable().apply {
                setColor(0xF2323232.toInt())
                cornerRadius = dp(10).toFloat()
            }
            val texto = TextView(ctx).apply {
                text = "Guardado: ${f.name}"
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 14f
                maxLines = 2
                ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
            }
            val abrir = TextView(ctx).apply {
                text = "ABRIR"
                setTextColor(0xFF8AB4F8.toInt())
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(16), dp(12), dp(8), dp(12))
                setOnClickListener {
                    quitarBarra(ctx)
                    try { ctx.startActivity(abrirIntent(ctx, f)) } catch (_: Exception) { }
                }
            }
            val cerrar = TextView(ctx).apply {
                text = "✕"
                setTextColor(0xFFBBBBBB.toInt())
                textSize = 16f
                setPadding(dp(12), dp(12), dp(8), dp(12))
                setOnClickListener { quitarBarra(ctx) }
            }
            val barra = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = fondo
                setPadding(dp(16), dp(4), dp(8), dp(4))
                addView(texto, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                addView(abrir)
                addView(cerrar)
            }
            val tipo = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipo,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.BOTTOM
                x = 0
                y = dp(72) // por encima de la barra de navegación / gestos
                horizontalMargin = 0.03f
            }
            wm.addView(barra, lp)
            barraActual = barra
            Handler(Looper.getMainLooper()).postDelayed({
                if (barraActual === barra) quitarBarra(ctx)
            }, 15000)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun quitarBarra(ctx: Context) {
        val b = barraActual ?: return
        barraActual = null
        try {
            (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(b)
        } catch (_: Exception) { }
    }

    private fun notificacion(ctx: Context, f: File) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            crearCanal(ctx)
            val abrir = abrirIntent(ctx, f)
            val id = f.absolutePath.hashCode()
            val pi = PendingIntent.getActivity(
                ctx, id, abrir, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(ctx, CANAL)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(f.name)
                .setContentText("Guardado en ${f.parentFile?.name?.ifEmpty { "Interno" } ?: "Interno"} · toca para abrir")
                .setContentIntent(pi)
                .addAction(0, "Abrir", pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            NotificationManagerCompat.from(ctx).notify(id, n)
        } catch (_: Exception) { }
    }

    private fun crearCanal(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CANAL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CANAL, "Archivos guardados", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }
}
