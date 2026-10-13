package com.senati.mitiemposemanal1

import android.content.Intent
import android.widget.Button
import android.widget.TextView
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.senati.mitiemposemanal1.database.DatabaseHelper
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Inicializar base de datos SQLite
        val db = DatabaseHelper(this)
        db.writableDatabase.close()

        // Mostrar nombre y versión
        val txtVersion =
            findViewById<TextView>(R.id.txtVersion)

        txtVersion.text =
            "Steeven Calderon · v0.1.0 (1)"

        // Botón categorías
        val btnCategorias =
            findViewById<Button>(R.id.btnCategorias)

        btnCategorias.setOnClickListener {
            val intent = Intent(
                this,
                CategoriasActivity::class.java
            )
            startActivity(intent)
        }

        // Botón tareas
        val btnTareas =
            findViewById<Button>(R.id.btnTareas)

        btnTareas.setOnClickListener {
            val intent = Intent(
                this,
                TareasActivity::class.java
            )
            startActivity(intent)
        }
        findViewById<Button>(R.id.btnEventos).setOnClickListener {
            startActivity(
                Intent(this, EventosActivity::class.java)
            )
        }
        findViewById<Button>(R.id.btnReportes).setOnClickListener {
            startActivity(
                Intent(this, ReportesActivity::class.java)
            )
        }
    }

}