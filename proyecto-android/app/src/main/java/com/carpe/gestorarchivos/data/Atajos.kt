package com.carpe.gestorarchivos.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.carpe.gestorarchivos.ActividadExterna
import com.carpe.gestorarchivos.MainActivity
import com.carpe.gestorarchivos.R
import java.io.File

/** Atajos al mantener pulsado el icono de la app: Última descarga, Descargas y Recientes. */
object Atajos {

    const val EXTRA_RECIENTES = "atajo_recientes"

    const val EXTRA_FAVORITOS = "atajo_favoritos"
    /** Descargas y Recientes: no cambian, se registran al abrir la app. */
    fun registrarBase(ctx: Context) {
        try {
            val app = ctx.applicationContext
            val descargas = Intent(app, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromFile(Almacenamientos.descargas()))
            val recientes = Intent(app, MainActivity::class.java)            val favoritos = Intent(app, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .putExtra(EXTRA_FAVORITOS, true)

                .setAction(Intent.ACTION_MAIN)
                .putExtra(EXTRA_RECIENTES, true)
            ShortcutManagerCompat.pushDynamicShortcut(app, atajo(app, "descargas", "Descargas", null, R.drawable.ic_atajo_carpeta, descargas, 1))
            ShortcutManagerCompat.pushDynamicShortcut(app, atajo(app, "recientes", "Recientes", null, R.drawable.ic_atajo_reciente, recientes, 2))            ShortcutManagerCompat.pushDynamicShortcut(app, atajo(app, "favoritos", "Favoritos", null, R.drawable.ic_atajo_carpeta, favoritos, 3))

        } catch (_: Exception) { }
    }

    /** «Última descarga»: se actualiza cada vez que otra app guarda un archivo en el gestor. */
    fun ultimaDescarga(ctx: Context, f: File) {
        try {
            val app = ctx.applicationContext
            val abrir = Intent(app, ActividadExterna::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setDataAndType(ProveedorDocumentos.uriDocumento(app, f), ArchivoItem.adivinarMime(f.name))
            ShortcutManagerCompat.pushDynamicShortcut(
                app, atajo(app, "ultima", "Última descarga", f.name, R.drawable.ic_atajo_archivo, abrir, 0)
            )
        } catch (_: Exception) { }
    }

    private fun atajo(ctx: Context, id: String, corto: String, largo: String?, icono: Int, intent: Intent, rango: Int) =
        ShortcutInfoCompat.Builder(ctx, id)
            .setShortLabel(corto)
            .apply { if (largo != null) setLongLabel(largo) }
            .setIcon(IconCompat.createWithResource(ctx, icono))
            .setIntent(intent)
            .setRank(rango)
            .build()
}
