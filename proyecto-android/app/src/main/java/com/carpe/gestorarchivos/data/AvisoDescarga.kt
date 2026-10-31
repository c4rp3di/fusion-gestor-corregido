package com.carpe.gestorarchivos.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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

    fun notificar(ctx: Context, f: File) {
        if (!f.isFile || f.length() == 0L) return
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        try {
            crearCanal(ctx)
            val abrir = Intent(Intent.ACTION_VIEW).apply {
                setClass(ctx, ActividadExterna::class.java)
                setDataAndType(ProveedorDocumentos.uriDocumento(ctx, f), ArchivoItem.adivinarMime(f.name))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
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
