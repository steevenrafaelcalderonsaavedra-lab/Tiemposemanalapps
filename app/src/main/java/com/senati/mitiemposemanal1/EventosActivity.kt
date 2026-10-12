package com.senati.mitiemposemanal1

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.senati.mitiemposemanal1.database.DatabaseHelper
import com.senati.mitiemposemanal1.database.Tarea
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
class EventosActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper

    private lateinit var etBuscarTarea: EditText
    private lateinit var listaResultados: ListView
    private lateinit var txtCategoria: TextView
    private lateinit var txtEventoActivo: TextView
    private lateinit var btnIniciar: Button
    private lateinit var btnFinalizar: Button
    private lateinit var contenedorHistorial: LinearLayout

    private var tareaSeleccionada: Tarea? = null
    private var resultados = listOf<Tarea>()

    private val formatoFecha =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_eventos)

        db = DatabaseHelper(this)

        etBuscarTarea = findViewById(R.id.etBuscarTarea)
        listaResultados = findViewById(R.id.listaResultados)
        txtCategoria = findViewById(R.id.txtCategoriaSeleccionada)
        txtEventoActivo = findViewById(R.id.txtEventoActivo)
        btnIniciar = findViewById(R.id.btnIniciarEvento)
        btnFinalizar = findViewById(R.id.btnFinalizarEvento)
        contenedorHistorial = findViewById(R.id.contenedorHistorial)

        configurarBuscador()

        btnIniciar.setOnClickListener {
            iniciarEvento()
        }

        btnFinalizar.setOnClickListener {
            finalizarEvento()
        }

        actualizarPantalla()
    }

    // ==========================================
    // HU-03: BUSCAR TAREAS
    // ==========================================

    private fun configurarBuscador() {

        etBuscarTarea.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    tareaSeleccionada = null
                    txtCategoria.text = "Categoría: ninguna"

                    val texto = s.toString().trim()

                    if (texto.length >= 2) {

                        resultados = db.buscarTareas(texto)

                        val nombres = resultados.map {
                            "${it.nombre} · ${it.categoria}"
                        }

                        listaResultados.adapter =
                            ArrayAdapter(
                                this@EventosActivity,
                                android.R.layout.simple_list_item_1,
                                nombres
                            )

                        listaResultados.visibility =
                            if (nombres.isEmpty())
                                View.GONE
                            else
                                View.VISIBLE

                    } else {
                        resultados = emptyList()
                        listaResultados.visibility = View.GONE
                    }
                }

                override fun afterTextChanged(
                    s: Editable?
                ) {}
            }
        )

        listaResultados.setOnItemClickListener {
                _, _, position, _ ->

            val tarea = resultados[position]

            tareaSeleccionada = tarea

            etBuscarTarea.setText(tarea.nombre)

            // setText activa el TextWatcher y limpia la selección
            tareaSeleccionada = tarea

            txtCategoria.text =
                "Categoría: ${tarea.categoria}"

            listaResultados.visibility = View.GONE
        }
    }

    // ==========================================
    // HU-04 Y HU-05: INICIAR EVENTO
    // ==========================================


    private fun iniciarEvento() {

        // Verificar si existe un evento activo
        if (db.obtenerEventoActivo() != null) {
            Toast.makeText(
                this,
                "Primero finaliza el evento actual",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nombre = etBuscarTarea.text.toString().trim()

        if (nombre.isEmpty()) {
            etBuscarTarea.error = "Escribe una tarea"
            return
        }

        // Buscar la tarea en SQLite
        val tareaExistente = db.buscarTareaExacta(nombre)

        if (tareaExistente != null) {

            // Si existe, iniciar directamente
            registrarInicio(tareaExistente)

        } else {

            // Si no existe, mostrar categorías
            mostrarDialogoNuevaTarea(nombre)
        }
    }




    private fun mostrarDialogoNuevaTarea(nombre: String) {

        val categorias = db.listarCategorias()

        if (categorias.isEmpty()) {
            Toast.makeText(
                this,
                "No hay categorías registradas",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Contenedor principal
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(40, 20, 40, 20)
        }

        // Mensaje de la tarea nueva
        val mensaje = TextView(this).apply {
            text = "La tarea \"$nombre\" no existe.\n" +
                    "Selecciona una categoría:"
            textSize = 16f
            setTextColor(
                android.graphics.Color.parseColor("#7F1D1D")
            )
            setPadding(0, 0, 0, 20)
        }

        contenedor.addView(mensaje)

        // Grupo de categorías
        val grupoCategorias = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
        }

        // Crear una opción por cada categoría
        categorias.forEach { categoria ->

            val opcion = RadioButton(this).apply {
                id = android.view.View.generateViewId()
                text = categoria.nombre
                textSize = 17f
                setTextColor(
                    android.graphics.Color.parseColor("#7F1D1D")
                )
                buttonTintList =
                    android.content.res.ColorStateList.valueOf(
                        android.graphics.Color.parseColor("#B71C1C")
                    )

                tag = categoria.id
            }

            grupoCategorias.addView(opcion)
        }

        contenedor.addView(grupoCategorias)

        val dialogo = AlertDialog.Builder(this)
            .setTitle("Crear nueva tarea")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Crear e iniciar", null)
            .create()

        dialogo.setOnShowListener {

            val botonCrear = dialogo.getButton(
                AlertDialog.BUTTON_POSITIVE
            )

            botonCrear.setOnClickListener {

                val opcionId = grupoCategorias.checkedRadioButtonId

                if (opcionId == -1) {
                    Toast.makeText(
                        this,
                        "Selecciona una categoría",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                val opcion = grupoCategorias.findViewById<RadioButton>(
                    opcionId
                )

                val categoriaId = opcion.tag as Int

                // Guardar tarea nueva
                val nuevaTarea = db.crearTareaEvento(
                    nombre,
                    categoriaId
                )

                if (nuevaTarea != null) {

                    // Iniciar automáticamente el evento
                    registrarInicio(nuevaTarea)

                    dialogo.dismiss()

                } else {
                    Toast.makeText(
                        this,
                        "No se pudo guardar la tarea",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        dialogo.show()
    }



    private fun registrarInicio(tarea: Tarea) {

        val correcto = db.iniciarEvento(tarea.id)

        if (correcto) {

            Toast.makeText(
                this,
                "Evento iniciado correctamente",
                Toast.LENGTH_SHORT
            ).show()

            etBuscarTarea.setText("")
            tareaSeleccionada = null

            actualizarPantalla()

        } else {

            Toast.makeText(
                this,
                "No se pudo iniciar el evento",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // ==========================================
    // HU-05: FINALIZAR EVENTO
    // ==========================================

    private fun finalizarEvento() {

        AlertDialog.Builder(this)
            .setTitle("Finalizar evento")
            .setMessage(
                "¿Deseas finalizar el evento actual?"
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Finalizar") { _, _ ->

                val correcto = db.finalizarEvento()

                if (correcto) {

                    Toast.makeText(
                        this,
                        "Evento finalizado",
                        Toast.LENGTH_SHORT
                    ).show()

                    actualizarPantalla()

                } else {

                    Toast.makeText(
                        this,
                        "No se pudo finalizar",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    // ==========================================
    // ACTUALIZAR ESTADO DE LA PANTALLA
    // ==========================================

    private fun actualizarPantalla() {

        val activo = db.obtenerEventoActivo()

        if (activo != null) {

            txtEventoActivo.text =
                "EVENTO EN CURSO\n\n" +
                        "Tarea: ${activo.tarea}\n" +
                        "Categoría: ${activo.categoria}\n" +
                        "Inicio: ${formatearFecha(activo.inicio)}"

            btnIniciar.isEnabled = false
            btnFinalizar.isEnabled = true

            etBuscarTarea.isEnabled = false
            listaResultados.visibility = View.GONE

        } else {

            txtEventoActivo.text =
                "No hay eventos en curso"

            btnIniciar.isEnabled = true
            btnFinalizar.isEnabled = false

            etBuscarTarea.isEnabled = true
        }

        mostrarHistorial()
    }

    // ==========================================
    // MOSTRAR HISTORIAL DE EVENTOS
    // ==========================================

    private fun mostrarHistorial() {

        contenedorHistorial.removeAllViews()

        val eventos = db.listarEventos()

        if (eventos.isEmpty()) {

            val texto = TextView(this).apply {
                text = "Todavía no hay eventos registrados"
                textSize = 15f
                setPadding(16, 20, 16, 20)
            }

            contenedorHistorial.addView(texto)
            return
        }

        eventos.forEach { evento ->

            val texto = TextView(this).apply {

                val duracion = evento.duracionSegundos

                val duracionTexto =
                    if (duracion == null)
                        "En curso"
                    else
                        formatearDuracion(duracion)

                text =
                    "Tarea: ${evento.tarea}\n" +
                            "Categoría: ${evento.categoria}\n" +
                            "Inicio: ${formatearFecha(evento.inicio)}\n" +
                            "Fin: ${evento.fin?.let { formatearFecha(it) } ?: "En curso"}\n" +
                            "Duración: $duracionTexto"

                textSize = 14f

                setTextColor(
                    android.graphics.Color.parseColor("#7F1D1D")
                )

                setBackgroundColor(
                    android.graphics.Color.WHITE
                )

                setPadding(24, 24, 24, 24)
            }

            val parametros =
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = 16
                }

            contenedorHistorial.addView(texto, parametros)
        }
    }

    private fun formatearFecha(fecha: String): String {
        return try {
            LocalDateTime.parse(fecha).format(formatoFecha)
        } catch (e: Exception) {
            fecha
        }
    }

    private fun formatearDuracion(segundos: Long): String {

        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val segundosRestantes = segundos % 60

        return "${horas}h ${minutos}m ${segundosRestantes}s"
    }

    override fun onResume() {
        super.onResume()

        if (::db.isInitialized &&
            ::txtEventoActivo.isInitialized) {
            actualizarPantalla()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) {
            db.close()
        }
        super.onDestroy()
    }
}