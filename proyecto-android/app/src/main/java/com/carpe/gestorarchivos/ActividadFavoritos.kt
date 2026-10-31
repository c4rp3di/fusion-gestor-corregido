package com.carpe.gestorarchivos

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.widget.ImageViewCompat
import androidx.core.widget.NestedScrollView
import com.carpe.gestorarchivos.data.Almacenamientos
import com.carpe.gestorarchivos.data.ArchivoItem
import com.carpe.gestorarchivos.data.Categoria
import com.carpe.gestorarchivos.data.ProveedorDocumentos
import com.carpe.gestorarchivos.data.RepositorioFavoritos
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors

/**
 * Destino del atajo «Favoritos»: actividad transparente que muestra la lista de favoritos como hoja inferior
 * (sobre la pantalla de inicio). Carpetas primero y archivos después; dentro de cada grupo, lo último que abriste arriba.
 */
class ActividadFavoritos : AppCompatActivity() {

    private lateinit var repo: RepositorioFavoritos
    private var cPrimario = 0
    private var cTexto = 0
    private var cTextoSec = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = RepositorioFavoritos(this)
        val todos = repo.listar().filter { it.existe }
        if (todos.isEmpty()) {
            Toast.makeText(this, "No tienes favoritos todavía", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val decor = window.decorView
        cPrimario = MaterialColors.getColor(decor, com.google.android.material.R.attr.colorPrimary)
        cTexto = MaterialColors.getColor(decor, com.google.android.material.R.attr.colorOnSurface)
        cTextoSec = ColorUtils.setAlphaComponent(cTexto, 150)
        val cSuperficie = MaterialColors.getColor(decor, com.google.android.material.R.attr.colorSurface)

        // Más recientes primero (los que nunca se abrieron conservan su orden); luego carpetas antes que archivos
        val porUso = todos.sortedByDescending { repo.ultimoUso(it.ruta) }
        val carpetas = porUso.filter { it.esCarpeta }
        val archivos = porUso.filter { !it.esCarpeta }

        val columna = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(10), 0, dp(24))
        }
        columna.addView(View(this).apply {
            background = GradientDrawable().apply { setColor(ColorUtils.setAlphaComponent(cTexto, 60)); cornerRadius = dp(2).toFloat() }
        }, LinearLayout.LayoutParams(dp(36), dp(4)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = dp(14) })
        columna.addView(TextView(this).apply {
            text = "Favoritos"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(cTexto)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setPadding(dp(20), 0, dp(20), dp(6))
        })

        val hoja = BottomSheetDialog(this)
        carpetas.forEach { columna.addView(fila(it, hoja)) }
        if (carpetas.isNotEmpty() && archivos.isNotEmpty()) columna.addView(separador("Archivos"))
        archivos.forEach { columna.addView(fila(it, hoja)) }

        val raiz = NestedScrollView(this).apply {
            background = GradientDrawable().apply {
                setColor(cSuperficie)
                val r = dp(22).toFloat()
                cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
            }
            addView(columna, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        }
        hoja.setContentView(raiz)
        hoja.setOnShowListener {
            val h = hoja.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return@setOnShowListener
            h.setBackgroundColor(Color.TRANSPARENT)
            BottomSheetBehavior.from(h).apply {
                state = BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }
        hoja.setOnDismissListener { finish() }
        hoja.show()
    }

    private fun fila(f: RepositorioFavoritos.Favorito, hoja: BottomSheetDialog): View {
        val ico = ImageView(this).apply {
            setImageResource(iconoDe(f))
            val p = dp(9)
            setPadding(p, p, p, p)
            val color = if (f.esCarpeta) cPrimario else ColorUtils.setAlphaComponent(cTexto, 200)
            ImageViewCompat.setImageTintList(this, android.content.res.ColorStateList.valueOf(color))
            background = GradientDrawable().apply {
                setColor(ColorUtils.setAlphaComponent(if (f.esCarpeta) cPrimario else cTexto, if (f.esCarpeta) 38 else 20))
                cornerRadius = dp(10).toFloat()
            }
        }
        val nombre = TextView(this).apply {
            text = f.nombre.ifEmpty { f.ruta }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTextColor(cTexto)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.MIDDLE
        }
        val detalle = TextView(this).apply {
            text = ubicacion(f)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTextColor(cTextoSec)
            maxLines = 1
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        val textos = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(nombre)
            addView(detalle)
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(56)
            setPadding(dp(20), dp(8), dp(20), dp(8))
            val tv = TypedValue()
            theme.resolveAttribute(android.R.attr.selectableItemBackground, tv, true)
            setBackgroundResource(tv.resourceId)
            addView(ico, LinearLayout.LayoutParams(dp(40), dp(40)))
            addView(textos, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(14) })
            setOnClickListener {
                repo.registrarUso(f.ruta)
                abrir(f)
                hoja.dismiss()
            }
        }
    }

    private fun separador(texto: String): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(20), dp(10), dp(20), dp(2))
        addView(TextView(context).apply {
            text = texto
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
            setTextColor(ColorUtils.setAlphaComponent(cTexto, 110))
        })
        addView(View(context).apply { setBackgroundColor(ColorUtils.setAlphaComponent(cTexto, 30)) },
            LinearLayout.LayoutParams(0, 1, 1f).apply { marginStart = dp(10) })
    }

    /** Segunda línea: carpeta que contiene el favorito (o el nombre del almacenamiento si está en la raíz). */
    private fun ubicacion(f: RepositorioFavoritos.Favorito): String {
        val padre = f.archivo.parentFile ?: return ""
        Almacenamientos.volumenes(this).firstOrNull { it.raiz.absolutePath == padre.absolutePath }?.let { return it.nombre }
        return padre.name
    }

    private fun iconoDe(f: RepositorioFavoritos.Favorito): Int {
        val ext = f.nombre.substringAfterLast('.', "").lowercase()
        return when (f.categoria) {
            Categoria.CARPETA -> R.drawable.ic_fav_carpeta
            Categoria.IMAGEN -> R.drawable.ic_fav_imagen
            Categoria.VIDEO -> R.drawable.ic_fav_video
            Categoria.AUDIO -> R.drawable.ic_fav_audio
            Categoria.COMPRIMIDO -> R.drawable.ic_fav_zip
            else -> if (ext in CODIGO) R.drawable.ic_fav_codigo else R.drawable.ic_fav_archivo
        }
    }

    private fun abrir(f: RepositorioFavoritos.Favorito) {
        val intent = if (f.esCarpeta) {
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setData(Uri.fromFile(f.archivo))
        } else {
            Intent(this, ActividadExterna::class.java)
                .setAction(Intent.ACTION_VIEW)
                .setDataAndType(ProveedorDocumentos.uriDocumento(this, f.archivo), ArchivoItem.adivinarMime(f.nombre))
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try { startActivity(intent) } catch (_: Exception) { }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private companion object {
        val CODIGO = setOf("html", "htm", "css", "js", "json", "xml", "kt", "java", "py", "php", "sh", "ts", "jsx", "tsx", "yml", "yaml")
    }
}
