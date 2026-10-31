package com.carpe.gestorarchivos

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.carpe.gestorarchivos.data.ArchivoItem
import com.carpe.gestorarchivos.data.ProveedorDocumentos
import com.carpe.gestorarchivos.data.RepositorioFavoritos

/**
 * Destino del atajo «Favoritos»: actividad transparente que solo muestra la lista de favoritos
 * como ventana flotante (sobre la pantalla de inicio). Al elegir uno lo abre; fuera de la ventana se cierra.
 */
class ActividadFavoritos : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lista = RepositorioFavoritos(this).listar().filter { it.existe }
        if (lista.isEmpty()) {
            Toast.makeText(this, "No tienes favoritos todavía", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val etiquetas = lista.map {
            (if (it.esCarpeta) "📁  " else "📄  ") + it.nombre.ifEmpty { it.ruta }
        }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Favoritos")
            .setItems(etiquetas) { _, i -> abrir(lista[i]) }
            .setOnDismissListener { finish() }
            .show()
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
}
