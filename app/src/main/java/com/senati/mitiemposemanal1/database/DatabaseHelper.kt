package com.senati.mitiemposemanal1.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Categoria(
    val id: Int,
    val nombre: String
)

data class Tarea(
    val id: Int,
    val nombre: String,
    val categoriaId: Int,
    val categoria: String
)

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "MiTiempo.db", null, 1) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE categorias (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL COLLATE NOCASE UNIQUE
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE tareas (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL COLLATE NOCASE UNIQUE,
                categoria_id INTEGER NOT NULL,
                FOREIGN KEY(categoria_id)
                    REFERENCES categorias(id)
                    ON DELETE RESTRICT
            )
        """.trimIndent())

        listOf("Estudio", "Trabajo", "Descanso", "Ocio")
            .forEach { nombre ->
                val valores = ContentValues().apply {
                    put("nombre", nombre)
                }
                db.insertOrThrow("categorias", null, valores)
            }
    }

    override fun onUpgrade(
        db: SQLiteDatabase,
        oldVersion: Int,
        newVersion: Int
    ) {
        // Agregar migraciones cuando cambie el esquema.
    }

    fun listarCategorias(): List<Categoria> {
        val lista = mutableListOf<Categoria>()
        readableDatabase.rawQuery(
            "SELECT id, nombre FROM categorias ORDER BY id",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(Categoria(
                    cursor.getInt(0),
                    cursor.getString(1)
                ))
            }
        }
        return lista
    }

    fun guardarCategoria(nombre: String): Boolean {
        val valor = nombre.trim()
        if (valor.isEmpty()) return false

        val datos = ContentValues().apply {
            put("nombre", valor)
        }
        return writableDatabase.insertWithOnConflict(
            "categorias", null, datos,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun editarCategoria(id: Int, nombre: String): Boolean {
        val valor = nombre.trim()
        if (valor.isEmpty()) return false

        val datos = ContentValues().apply {
            put("nombre", valor)
        }
        return writableDatabase.updateWithOnConflict(
            "categorias", datos, "id=?",
            arrayOf(id.toString()),
            SQLiteDatabase.CONFLICT_IGNORE
        ) > 0
    }

    fun eliminarCategoria(id: Int): Boolean {
        if (tieneTareas(id)) return false
        return writableDatabase.delete(
            "categorias", "id=?",
            arrayOf(id.toString())
        ) > 0
    }

    fun tieneTareas(id: Int): Boolean {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM tareas WHERE categoria_id=?",
            arrayOf(id.toString())
        ).use { cursor ->
            return cursor.moveToFirst() &&
                    cursor.getInt(0) > 0
        }
    }

    fun listarTareas(): List<Tarea> {
        val lista = mutableListOf<Tarea>()
        readableDatabase.rawQuery("""
            SELECT t.id, t.nombre, t.categoria_id, c.nombre
            FROM tareas t
            INNER JOIN categorias c
                ON t.categoria_id = c.id
            ORDER BY t.id DESC
        """.trimIndent(), null).use { cursor ->
            while (cursor.moveToNext()) {
                lista.add(Tarea(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getString(3)
                ))
            }
        }
        return lista
    }

    fun guardarTarea(
        nombre: String,
        categoriaId: Int
    ): Boolean {
        val valor = nombre.trim()
        if (valor.isEmpty()) return false

        val datos = ContentValues().apply {
            put("nombre", valor)
            put("categoria_id", categoriaId)
        }
        return writableDatabase.insertWithOnConflict(
            "tareas", null, datos,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }

    fun editarTarea(
        id: Int,
        nombre: String,
        categoriaId: Int
    ): Boolean {
        val valor = nombre.trim()
        if (valor.isEmpty()) return false

        val datos = ContentValues().apply {
            put("nombre", valor)
            put("categoria_id", categoriaId)
        }
        return writableDatabase.updateWithOnConflict(
            "tareas", datos, "id=?",
            arrayOf(id.toString()),
            SQLiteDatabase.CONFLICT_IGNORE
        ) > 0
    }

    fun eliminarTarea(id: Int): Boolean {
        return writableDatabase.delete(
            "tareas", "id=?",
            arrayOf(id.toString())
        ) > 0
    }
}