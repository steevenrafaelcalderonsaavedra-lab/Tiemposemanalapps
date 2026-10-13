package com.senati.mitiemposemanal1

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.senati.mitiemposemanal1.database.DatabaseHelper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReportesActivity : AppCompatActivity() {
    private lateinit var db: DatabaseHelper
    private lateinit var spSemanas: Spinner
    private lateinit var txtSinReportes: TextView
    private lateinit var contenedorReporte: LinearLayout
    private lateinit var contenedorEventos: LinearLayout

    private val formatoFecha =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")

    private val formatoFechaHora =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportes)

        db = DatabaseHelper(this)

        spSemanas = findViewById(R.id.spSemanas)
        txtSinReportes = findViewById(R.id.txtSinReportes)
        contenedorReporte =
            findViewById(R.id.contenedorReporte)
        contenedorEventos =
            findViewById(R.id.contenedorEventosReporte)

        cargarSemanas()
    }

    private fun cargarSemanas() {

        db.cerrarSemanasPendientes()

        val semanas = db.listarSemanasReporte()

        if (semanas.isEmpty()) {
            txtSinReportes.visibility = View.VISIBLE
            spSemanas.visibility = View.GONE
            contenedorReporte.removeAllViews()
            contenedorEventos.removeAllViews()
            return
        }

        txtSinReportes.visibility = View.GONE
        spSemanas.visibility = View.VISIBLE

        val nombres = semanas.map { semana ->
            "${formatearDia(semana.inicio)} – " +
                    formatearDia(semana.fin)
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombres
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        spSemanas.adapter = adapter

        spSemanas.onItemSelectedListener =
            object : android.widget.AdapterView
            .OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    mostrarReporte(semanas[position].inicio)
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }

    private fun mostrarReporte(inicio: String) {

        contenedorReporte.removeAllViews()
        contenedorEventos.removeAllViews()

        val detalles = db.listarDetallesReporte(inicio)
        val segundosSemana = 604800L

        var totalRegistrado = 0L

        detalles.forEach { detalle ->

            totalRegistrado += detalle.segundos

            agregarFila(
                detalle.categoria,
                detalle.segundos,
                segundosSemana,
                Color.parseColor("#B71C1C")
            )
        }

        val sinRegistrar =
            (segundosSemana - totalRegistrado)
                .coerceAtLeast(0L)

        agregarFila(
            "Sin registrar",
            sinRegistrar,
            segundosSemana,
            Color.GRAY
        )

        val eventos = db.listarEventosSemana(inicio)

        if (eventos.isEmpty()) {
            agregarTextoEvento(
                "No hay eventos registrados en esta semana"
            )
        }

        eventos.forEach { evento ->

            val duracion = evento.duracionSegundos

            val duracionTexto = if (duracion == null) {
                "En curso"
            } else {
                formatearDuracion(duracion)
            }

            val texto = """
                Tarea: ${evento.tarea}
                Categoría: ${evento.categoria}
                Inicio: ${formatearFechaHora(evento.inicio)}
                Fin: ${evento.fin?.let {
                formatearFechaHora(it)
            } ?: "En curso"}
                Duración: $duracionTexto
            """.trimIndent()

            agregarTextoEvento(texto)
        }
    }

    private fun agregarFila(
        nombre: String,
        segundos: Long,
        totalSemana: Long,
        color: Int
    ) {

        val porcentaje =
            segundos.toDouble() / totalSemana * 100.0

        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60

        val porcentajeTexto = String.format(
            Locale.US,
            "%.1f",
            porcentaje
        )

        val fila = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 14, 12, 18)
        }

        val texto = TextView(this).apply {
            text = "$nombre: ${horas}h ${minutos}m " +
                    "($porcentajeTexto %)"
            textSize = 15f
            setTextColor(
                Color.parseColor("#7F1D1D")
            )
        }

        val barra = ProgressBar(
            this,
            null,
            android.R.attr.progressBarStyleHorizontal
        ).apply {
            max = 1000
            progress = (
                    porcentaje.coerceIn(0.0, 100.0) * 10
                    ).toInt()

            progressTintList =
                android.content.res.ColorStateList.valueOf(
                    color
                )
        }

        fila.addView(texto)

        fila.addView(
            barra,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                24
            )
        )

        contenedorReporte.addView(fila)
    }

    private fun agregarTextoEvento(contenido: String) {

        val texto = TextView(this).apply {
            text = contenido
            textSize = 14f
            setTextColor(
                Color.parseColor("#7F1D1D")
            )
            setBackgroundColor(Color.WHITE)
            setPadding(20, 20, 20, 20)
        }

        val parametros = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = 12
        }

        contenedorEventos.addView(texto, parametros)
    }

    private fun formatearDia(fecha: String): String {
        return LocalDate.parse(fecha).format(formatoFecha)
    }

    private fun formatearFechaHora(fecha: String): String {
        return try {
            LocalDateTime.parse(fecha).format(
                formatoFechaHora
            )
        } catch (e: Exception) {
            fecha
        }
    }

    private fun formatearDuracion(segundos: Long): String {
        val horas = segundos / 3600
        val minutos = (segundos % 3600) / 60
        val resto = segundos % 60

        return "${horas}h ${minutos}m ${resto}s"
    }

    override fun onResume() {
        super.onResume()
        if (::db.isInitialized && ::spSemanas.isInitialized) {
            cargarSemanas()
        }
    }

    override fun onDestroy() {
        if (::db.isInitialized) db.close()
        super.onDestroy()
    }
}