package com.g022.sanamovil.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "clinical_decisions_log")
data class ClinicalDecisionLog(
    // ID único y aleatorio para cada caso
    @PrimaryKey val caseId: String = UUID.randomUUID().toString(),

    // Lo que el usuario dijo o seleccionó
    val inputCompleto: String,

    // El texto final que la IA o el sistema le mostró al paciente
    val outputGenerado: String,

    // Lista de reglas que se activaron (ej. "Regla Obstétrica, Regla de Fiebre")
    val reglasActivadas: String,

    // El nivel de riesgo (ROJO, NARANJA, AMARILLO, VERDE)
    val nivelRiesgoAsignado: String,

    // Versión del motor (vital para auditorías)
    val versionAlgoritmo: String = "v1.0.0",

    // Marca de tiempo exacta (Timestamp)
    val timestamp: Long = System.currentTimeMillis()
)