package com.senati.mitiemposemanal1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.widget.*
import android.app.AlertDialog
import com.senati.mitiemposemanal1.database.DatabaseHelper
import com.senati.mitiemposemanal1.database.Tarea
import kotlin.collections.get
import kotlin.compareTo

class TareasActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var lista: ListView
    private var tareas = listOf<Tarea>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tareas)
        db = DatabaseHelper(this)
        lista = findViewById(R.id.listaTareas)

        findViewById<Button>(R.id.btnNuevaTarea)
            .setOnClickListener {
                formulario(null)
            }

        lista.setOnItemClickListener { _, _, posicion, _ ->
            val tarea = tareas[posicion]

            AlertDialog.Builder(this)
                .setTitle(tarea.nombre)
                .setItems(arrayOf("Editar", "Eliminar")) { _, opcion ->
                    if (opcion == 0) {
                        formulario(tarea)
                    } else {
                        confirmarEliminar(tarea)
                    }
                }
                .show()
        }

        actualizarLista()
    }

    private fun actualizarLista() {
        tareas = db.listarTareas()

        lista.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            tareas.map { "${it.nombre} · ${it.categoria}" }
        )
    }

    private fun formulario(tarea: Tarea?) {
        val categorias = db.listarCategorias()

        if (categorias.isEmpty()) {
            Toast.makeText(
                this,
                "Primero registra una categoría",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val entrada = EditText(this).apply {
            hint = "Nombre de la tarea"
            setSingleLine(true)
            setText(tarea?.nombre ?: "")
        }

        val selector = Spinner(this)
        selector.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            categorias.map { it.nombre }
        )

        if (tarea != null) {
            val indice = categorias.indexOfFirst {
                it.id == tarea.categoriaId
            }
            if (indice >= 0) selector.setSelection(indice)
        }

        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
            addView(entrada)
            addView(selector)
        }

        val dialogo = AlertDialog.Builder(this)
            .setTitle(
                if (tarea == null) "Nueva tarea"
                else "Editar tarea"
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
                        entrada.error = "Ingrese el nombre"
                        return@setOnClickListener
                    }

                    val categoriaId =
                        categorias[selector.selectedItemPosition].id

                    val guardado = if (tarea == null) {
                        db.guardarTarea(nombre, categoriaId)
                    } else {
                        db.editarTarea(
                            tarea.id,
                            nombre,
                            categoriaId
                        )
                    }

                    if (guardado) {
                        actualizarLista()
                        dialogo.dismiss()
                    } else {
                        Toast.makeText(
                            this,
                            "La tarea ya existe o no se pudo guardar",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        dialogo.show()
    }

    private fun confirmarEliminar(tarea: Tarea) {
        AlertDialog.Builder(this)
            .setTitle("Eliminar tarea")
            .setMessage("¿Eliminar ${tarea.nombre}?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->
                if (db.eliminarTarea(tarea.id)) {
                    actualizarLista()
                } else {
                    Toast.makeText(
                        this,
                        "No se pudo eliminar",
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