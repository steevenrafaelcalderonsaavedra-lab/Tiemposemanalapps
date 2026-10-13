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
data class Evento(
    val id: Int,
    val tareaId: Int,
    val tarea: String,
    val categoria: String,
    val inicio: String,
    val fin: String?,
    val duracionSegundos: Long?
)
data class ResumenCategoria(
    val categoriaId: Int,
    val categoria: String,
    val segundos: Long
)
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "MiTiempo.db", null, 2) {

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
        crearTablaEventos(db)

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
        if (oldVersion < 3) {
            crearTablaEventos(db)
        }
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

    private fun crearTablaEventos(db: SQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS eventos (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tarea_id INTEGER NOT NULL,
                inicio TEXT NOT NULL,
                fin TEXT,
                duracion_segundos INTEGER,
                FOREIGN KEY(tarea_id)
                    REFERENCES tareas(id)
                    ON DELETE RESTRICT
            )
        """.trimIndent())

        // Solo se permite un evento sin finalizar
        db.execSQL("""
            CREATE UNIQUE INDEX IF NOT EXISTS
                idx_evento_activo
            ON eventos ((1))
            WHERE fin IS NULL
        """.trimIndent())
    }


    // ==========================================
    // HU-03: BUSCAR TAREAS POR NOMBRE
    // ==========================================

    fun buscarTareas(texto: String): List<Tarea> {

        val lista = mutableListOf<Tarea>()

        if (texto.trim().length < 2) {
            return lista
        }

        val busqueda = "%${texto.trim()}%"

        readableDatabase.rawQuery("""
            SELECT
                t.id,
                t.nombre,
                t.categoria_id,
                c.nombre
            FROM tareas t
            INNER JOIN categorias c
                ON t.categoria_id = c.id
            WHERE t.nombre LIKE ? COLLATE NOCASE
            ORDER BY t.nombre
        """.trimIndent(), arrayOf(busqueda)).use { cursor ->

            while (cursor.moveToNext()) {
                lista.add(
                    Tarea(
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getInt(2),
                        cursor.getString(3)
                    )
                )
            }
        }

        return lista
    }


    // ==========================================
    // BUSCAR TAREA EXACTA
    // ==========================================

    fun buscarTareaExacta(nombre: String): Tarea? {

        readableDatabase.rawQuery("""
            SELECT
                t.id,
                t.nombre,
                t.categoria_id,
                c.nombre
            FROM tareas t
            INNER JOIN categorias c
                ON t.categoria_id = c.id
            WHERE t.nombre = ? COLLATE NOCASE
            LIMIT 1
        """.trimIndent(), arrayOf(nombre.trim())).use { cursor ->

            if (cursor.moveToFirst()) {
                return Tarea(
                    cursor.getInt(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getString(3)
                )
            }
        }

        return null
    }


    // ==========================================
    // HU-04: CREAR TAREA NUEVA
    // ==========================================

    fun crearTareaEvento(
        nombre: String,
        categoriaId: Int
    ): Tarea? {

        if (!guardarTarea(nombre, categoriaId)) {
            return null
        }

        return buscarTareaExacta(nombre)
    }


    // ==========================================
    // HU-05: INICIAR EVENTO
    // ==========================================

    fun iniciarEvento(tareaId: Int): Boolean {

        if (obtenerEventoActivo() != null) {
            return false
        }

        val inicio = java.time.LocalDateTime.now()
            .toString()

        val datos = ContentValues().apply {
            put("tarea_id", tareaId)
            put("inicio", inicio)
            putNull("fin")
            putNull("duracion_segundos")
        }

        return writableDatabase.insertWithOnConflict(
            "eventos",
            null,
            datos,
            SQLiteDatabase.CONFLICT_IGNORE
        ) != -1L
    }


    // ==========================================
    // OBTENER EVENTO ACTIVO
    // ==========================================

    fun obtenerEventoActivo(): Evento? {

        readableDatabase.rawQuery("""
            SELECT
                e.id,
                e.tarea_id,
                t.nombre,
                c.nombre,
                e.inicio,
                e.fin,
                e.duracion_segundos
            FROM eventos e
            INNER JOIN tareas t
                ON e.tarea_id = t.id
            INNER JOIN categorias c
                ON t.categoria_id = c.id
            WHERE e.fin IS NULL
            LIMIT 1
        """.trimIndent(), null).use { cursor ->

            if (cursor.moveToFirst()) {
                return Evento(
                    cursor.getInt(0),
                    cursor.getInt(1),
                    cursor.getString(2),
                    cursor.getString(3),
                    cursor.getString(4),
                    null,
                    null
                )
            }
        }

        return null
    }


    // ==========================================
    // HU-05: FINALIZAR EVENTO
    // ==========================================

    fun finalizarEvento(): Boolean {

        val evento = obtenerEventoActivo()
            ?: return false

        val fin = java.time.LocalDateTime.now()

        val inicio = java.time.LocalDateTime.parse(
            evento.inicio
        )

        val duracion = java.time.Duration.between(
            inicio,
            fin
        ).seconds.coerceAtLeast(0)

        val datos = ContentValues().apply {
            put("fin", fin.toString())
            put("duracion_segundos", duracion)
        }

        return writableDatabase.update(
            "eventos",
            datos,
            "id=? AND fin IS NULL",
            arrayOf(evento.id.toString())
        ) > 0
    }


    // ==========================================
    // LISTAR HISTORIAL DE EVENTOS
    // ==========================================

    fun listarEventos(): List<Evento> {

        val lista = mutableListOf<Evento>()

        readableDatabase.rawQuery("""
            SELECT
                e.id,
                e.tarea_id,
                t.nombre,
                c.nombre,
                e.inicio,
                e.fin,
                e.duracion_segundos
            FROM eventos e
            INNER JOIN tareas t
                ON e.tarea_id = t.id
            INNER JOIN categorias c
                ON t.categoria_id = c.id
            ORDER BY e.id DESC
        """.trimIndent(), null).use { cursor ->

            while (cursor.moveToNext()) {

                lista.add(
                    Evento(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        if (cursor.isNull(5)) null
                        else cursor.getString(5),
                        if (cursor.isNull(6)) null
                        else cursor.getLong(6)
                    )
                )
            }
        }

        return lista
    }
    fun eliminarTarea(id: Int): Boolean {

        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM eventos WHERE tarea_id=?",
            arrayOf(id.toString())
        ).use { cursor ->

            if (cursor.moveToFirst() &&
                cursor.getInt(0) > 0) {
                return false
            }
        }

        return writableDatabase.delete(
            "tareas",
            "id=?",
            arrayOf(id.toString())
        ) > 0
    }

    fun obtenerResumenSemanal(
        inicioSemana: String,
        inicioSemanaSiguiente: String
    ): List<ResumenCategoria> {

        val lista = mutableListOf<ResumenCategoria>()

        val sql = """
        SELECT
            c.id,
            c.nombre,
            COALESCE(
                SUM(e.duracion_segundos),
                0
            ) AS total_segundos
        FROM categorias c
        LEFT JOIN tareas t
            ON t.categoria_id = c.id
        LEFT JOIN eventos e
            ON e.tarea_id = t.id
            AND e.fin IS NOT NULL
            AND e.inicio >= ?
            AND e.inicio < ?
        GROUP BY c.id, c.nombre
        ORDER BY c.id
    """.trimIndent()

        readableDatabase.rawQuery(
            sql,
            arrayOf(inicioSemana, inicioSemanaSiguiente)
        ).use { cursor ->

            while (cursor.moveToNext()) {

                lista.add(
                    ResumenCategoria(
                        categoriaId = cursor.getInt(0),
                        categoria = cursor.getString(1),
                        segundos = cursor.getLong(2)
                    )
                )
            }
        }

        return lista
    }

}