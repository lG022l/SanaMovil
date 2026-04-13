package com.g022.sanamovil.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ClinicalDecisionDao {

    // Inserta un nuevo registro en la bitácora
    @Insert
    suspend fun insertDecisionLog(log: ClinicalDecisionLog)

    // Recupera todo el historial ordenado desde el más reciente (para reportes/auditorías)
    @Query("SELECT * FROM clinical_decisions_log ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<ClinicalDecisionLog>
}