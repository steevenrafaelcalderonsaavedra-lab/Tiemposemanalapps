package com.senati.mitiemposemanal1

import android.app.AlertDialog
import android.widget.*
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.senati.mitiemposemanal1.database.Categoria
import com.senati.mitiemposemanal1.database.DatabaseHelper
class CategoriasActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var lista: ListView
    private var categorias = listOf<Categoria>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categorias)
        db = DatabaseHelper(this)
        lista = findViewById(R.id.listaCategorias)

        findViewById<Button>(R.id.btnNuevaCategoria)
            .setOnClickListener {
                formulario(null)
            }

        lista.setOnItemClickListener { _, _, posicion, _ ->
            val categoria = categorias[posicion]

            AlertDialog.Builder(this)
                .setTitle(categoria.nombre)
                .setItems(arrayOf("Editar", "Eliminar")) { _, opcion ->
                    if (opcion == 0) {
                        formulario(categoria)
                    } else {
                        confirmarEliminar(categoria)
                    }
                }
                .show()
        }

        actualizarLista()
    }

    private fun actualizarLista() {
        categorias = db.listarCategorias()

        lista.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            categorias.map { it.nombre }
        )
    }

    private fun formulario(categoria: Categoria?) {
        val entrada = EditText(this)
        entrada.setSingleLine(true)
        entrada.hint = "Nombre de categoría"
        entrada.setText(categoria?.nombre ?: "")

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(entrada)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(
                if (categoria == null)
                    "Nueva categoría"
                else "Editar categoría"
            )
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialogo.setOnShowListener {
            dialogo.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {
                    val nombre = entrada.text.toString().trim()

                    if (nombre.isEmpty()) {
                        entrada.error = "Ingrese un nombre"
                        return@setOnClickListener
                    }

                    val guardado = if (categoria == null) {
                        db.guardarCategoria(nombre)
                    } else {
                        db.editarCategoria(categoria.id, nombre)
                    }

                    if (guardado) {
                        actualizarLista()
                        dialogo.dismiss()
                    } else {
                        Toast.makeText(
                            this,
                            "La categoría ya existe",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        dialogo.show()
    }

    private fun confirmarEliminar(categoria: Categoria) {
        if (db.tieneTareas(categoria.id)) {
            Toast.makeText(
                this,
                "No se puede eliminar: tiene tareas registradas",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Eliminar categoría")
            .setMessage("¿Eliminar ${categoria.nombre}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (db.eliminarCategoria(categoria.id)) {
                    actualizarLista()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar la categoría",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    override fun onDestroy() {
        db.close()
        super.onDestroy()
    }
}