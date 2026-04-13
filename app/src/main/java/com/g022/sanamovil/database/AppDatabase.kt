package com.g022.sanamovil.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// Aquí le decimos a Android que esta base de datos contiene nuestra tabla de auditoría
@Database(entities = [ClinicalDecisionLog::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    // Conectamos el DAO que creaste hace rato
    abstract fun clinicalDecisionDao(): ClinicalDecisionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Este código asegura que solo exista UNA copia de la base de datos abierta a la vez
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sanamovil_clinical_database" // El nombre del archivo encriptado en el celular
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}