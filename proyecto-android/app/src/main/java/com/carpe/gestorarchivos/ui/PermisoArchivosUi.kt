package com.carpe.gestorarchivos.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.carpe.gestorarchivos.data.Permisos

/** Petición del permiso de archivos, compartida por MainActivity y ActividadExterna. */
object PermisoArchivosUi {

    /**
     * Ofrece activar «Mostrar sobre otras apps» (barra «Guardado · ABRIR»). El aviso no se cierra al tocar fuera
     * y se repite hasta que el usuario responda; «Ahora no» lo silencia.
     */
    fun ofrecerBarraInferior(actividad: AppCompatActivity) {
        val ajustes = com.carpe.gestorarchivos.data.Ajustes(actividad)
        if (ajustes.preguntadoSuperposicion || Settings.canDrawOverlays(actividad)) return
        AlertDialog.Builder(actividad)
            .setTitle("Aviso al terminar de guardar")
            .setMessage(
                "Para mostrar una barra «Guardado · ABRIR» en la parte inferior al terminar una descarga, " +
                    "activa «Permitir sobre otras apps» para esta app.\n\nSin ello recibirás una notificación."
            )
            .setCancelable(false)
            .setPositiveButton("Activar") { _, _ ->
                try {
                    actividad.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${actividad.packageName}"))
                    )
                } catch (_: Exception) { }
            }
            .setNegativeButton("Ahora no") { _, _ -> ajustes.preguntadoSuperposicion = true }
            .show()
    }

    fun pedirSiHaceFalta(actividad: AppCompatActivity, legacy: ActivityResultLauncher<Array<String>>) {
        if (Permisos.tiene(actividad)) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AlertDialog.Builder(actividad)
                .setTitle("Permiso necesario")
                .setMessage(
                    "Para funcionar como gestor de archivos, la app necesita acceso a todos los archivos del dispositivo.\n\n" +
                        "En la siguiente pantalla, activa el interruptor «Permitir acceso a la administración de todos los archivos»."
                )
                .setPositiveButton("Abrir ajustes") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = Uri.parse("package:${actividad.packageName}")
                    try {
                        actividad.startActivity(intent)
                    } catch (_: Exception) {
                        actividad.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
                .setNegativeButton("Ahora no", null)
                .show()
        } else {
            // Android 6–10: permiso de tiempo de ejecución clásico
            legacy.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }
    }
}
