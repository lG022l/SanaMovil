package com.g022.sanamovil.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime

class ClinicalValidationPipelineTest {

    private lateinit var ruleEngine: ClinicalRuleEngine

    @Before
    fun setup() {
        // Inicializamos tu motor determinista puro
        ruleEngine = ClinicalRuleEngine()
    }

    @Test
    fun runClinicalValidationPipeline() {
        // 1. Aquí simularíamos la lectura de tu dataset_v1.json
        // (Para esta prueba rápida, crearemos un caso manualmente directo en Kotlin)
        val testCase = StructuredSymptoms(
            intensity = 9,
            hasChestPain = true,
            radiatingPain = true,
            hasSevereBleeding = false,
            hasBreathingDifficulty = true,
            hasHighFever = false,
            isConscious = true
        )
        val expectedLevel = RiskLevel.ORANGE

        // 2. Pasamos el caso por el sistema
        val engineEval = ruleEngine.evaluateSymptoms(testCase)
        val actualLevel = engineEval.riskLevel

        // 3. Generamos el reporte en consola (Logging de Validación)
        println("=== REPORTE DE VALIDACIÓN CLÍNICA ===")
        println("Fecha de ejecución: ${LocalDateTime.now()}")
        println("Caso evaluado: Infarto Simulado")
        println("Nivel Esperado (Médico): $expectedLevel")
        println("Nivel Obtenido (Sanamovil): $actualLevel")
        println("Reglas Activadas: ${engineEval.triggeredRules}")

        // 4. Afirmación estricta: Si esto falla, el test fracasa y la app no se debe liberar
        assertEquals("El algoritmo falló en clasificar correctamente el nivel de riesgo", expectedLevel, actualLevel)

        println("✅ Prueba superada: Precisión 100% en este caso.")
    }
}