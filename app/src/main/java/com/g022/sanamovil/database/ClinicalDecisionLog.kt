package com.g022.sanamovil.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clinical_decisions_log")
data class ClinicalDecisionLog(
    // --- CAMPOS EXISTENTES (Casos y Triage) ---
    @PrimaryKey val caseId: String = UUID.randomUUID().toString(),
    val inputCompleto: String,
    val outputGenerado: String,
    val reglasActivadas: String,
    val nivelRiesgoAsignado: String,
    val versionAlgoritmo: String = "v1.0.0",
    val timestamp: Long = System.currentTimeMillis(),

    // --- NUEVOS CAMPOS (Ampliación) ---

    // 1. Consentimientos
    // Guarda si el paciente aceptó los términos antes de usar la IA/Triage
    @ColumnInfo(name = "consentimiento_aceptado")
    val consentimientoAceptado: Boolean = false,

    // 2. Métricas básicas
    // Ej: Cuánto tardó el modelo LLM o el motor de reglas en dar la respuesta (en milisegundos)
    @ColumnInfo(name = "tiempo_procesamiento_ms")
    val tiempoProcesamientoMs: Long = 0L,

    // 3. Metadatos del Caso
    // Para identificar de qué comunidad o dispositivo viene cuando el supervisor junte los datos
    @ColumnInfo(name = "id_dispositivo_origen")
    val idDispositivoOrigen: String = "DISPOSITIVO_DESCONOCIDO",

    // (Opcional) Si necesitas un identificador del paciente que no comprometa su privacidad
    @ColumnInfo(name = "paciente_hash_anonimo")
    val pacienteHashAnonimo: String = "ANONIMO"
)