package com.g022.sanamovil.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 1. CAMBIAR LA VERSIÓN DE 1 a 2
@Database(entities = [ClinicalDecisionLog::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun clinicalDecisionDao(): ClinicalDecisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 2. CREAR LA REGLA DE MIGRACIÓN
        // Le decimos a SQLite qué comandos ejecutar para actualizar la tabla sin borrar los datos viejos
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // En SQLite, los booleanos se guardan como INTEGER (0 = false, 1 = true)
                db.execSQL("ALTER TABLE clinical_decisions_log ADD COLUMN consentimiento_aceptado INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clinical_decisions_log ADD COLUMN tiempo_procesamiento_ms INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE clinical_decisions_log ADD COLUMN id_dispositivo_origen TEXT NOT NULL DEFAULT 'DISPOSITIVO_DESCONOCIDO'")
                db.execSQL("ALTER TABLE clinical_decisions_log ADD COLUMN paciente_hash_anonimo TEXT NOT NULL DEFAULT 'ANONIMO'")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sanamovil_clinical_database"
                )
                    // 3. AGREGAR LA MIGRACIÓN AL BUILDER
                    .addMigrations(MIGRATION_1_2)

                    // NOTA: Si estás en fase de pruebas puras y no te importa borrar el historial
                    // viejo en tu dispositivo de prueba, puedes comentar ".addMigrations" y usar:
                    // .fallbackToDestructiveMigration()

                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}