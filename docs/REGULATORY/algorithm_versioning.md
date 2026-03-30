# Control de Versiones del Algoritmo
**Motor de Reglas Clínicas de Sanamóvil**

## Versión Actual: v1.0.0 (Marzo 2026)
* **Estado:** Producción inicial.
* **Motor:** `ClinicalRuleEngine` (Determinista) + Generación de explicación por LLM local.
* **Cambios principales:**
    - Implementación base de 4 niveles de triage (ROJO, NARANJA, AMARILLO, VERDE).
    - Integración de banderas rojas booleanas (Respiración, Hemorragia, Consciencia, Pecho).
    - Filtrado de palabras prohibidas y restricciones de claims diagnósticos.

## Historial de Revisiones
* **v0.9.0 (Beta):** Solo LLM. Deprecado por falta de determinismo clínico.
* **v1.0.0 (Actual):** Arquitectura híbrida (Reglas Duras + Explicación Empática).

*(Nota: Cualquier modificación a la lógica del `ClinicalRuleEngine` requerirá una actualización de la versión menor/mayor y una re-ejecución del dataset de validación `dataset_v1.json`).*