# Análisis de Riesgos (FMEA)
**Matriz de Fallos y Efectos de Sanamóvil**

## 1. Riesgo: Falso Negativo (Subtriaje)
* **Modo de fallo:** El sistema clasifica como "Rutina" una verdadera emergencia médica.
* **Efecto:** Retraso en atención médica crítica. Daño severo al paciente.
* **Mitigación (Guardrails):** - El sistema implementa un `ClinicalRuleEngine` determinista que prevalece sobre el LLM.
    - Cualquier mención de "dificultad respiratoria" o "dolor de pecho irradiado" fuerza el nivel más alto de riesgo, bloqueando cualquier evaluación de baja prioridad por parte de la IA.
    - El Aviso Legal siempre instruye llamar a emergencias si el usuario siente que su vida corre peligro.

## 2. Riesgo: Falso Positivo (Sobretriaje)
* **Modo de fallo:** El sistema clasifica como "Emergencia" un caso leve.
* **Efecto:** Saturación innecesaria de los servicios de urgencias. Ansiedad en el usuario.
* **Mitigación:** - Clínicamente aceptable en sistemas de pre-triaje automatizado. Se prefiere pecar de precavidos (fail-safe) que arriesgar la vida del usuario.