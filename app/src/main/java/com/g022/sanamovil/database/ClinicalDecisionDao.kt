package com.g022.sanamovil.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ClinicalDecisionDao {

    @Insert
    suspend fun insertDecisionLog(log: ClinicalDecisionLog): Long

    // Recupera todo el historial ordenado desde el más reciente (para reportes/auditorías)
    @Query("SELECT * FROM clinical_decisions_log ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(): List<ClinicalDecisionLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(logs: List<ClinicalDecisionLog>): List<Long>
}