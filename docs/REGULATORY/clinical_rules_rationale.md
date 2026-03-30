# Racionalidad de Reglas Clínicas (Clinical Rationale)
**Motor Determinista - Sanamóvil**

Este documento detalla la base bibliográfica y el raciocinio clínico detrás de las reglas duras (hard-rules) implementadas en el sistema.

## Regla 1: Nivel ROJO (Emergencia Inmediata)
* **Variables:** Dificultad respiratoria severa, Hemorragia severa, Inconsciencia.
* **Justificación:** Corresponde al Nivel 1/2 del sistema Triage Manchester (MTS) y ESI (Emergency Severity Index). El compromiso de la vía aérea o estado hemodinámico son criterios universales de resucitación inmediata.

## Regla 2: Nivel NARANJA (Muy Urgente)
* **Variables:** Dolor de pecho + Irradiación.
* **Justificación:** Guiado por protocolos de la American Heart Association (AHA) para Síndrome Coronario Agudo. Requiere evaluación y ECG en < 10 minutos.

## Regla 3: Nivel AMARILLO (Urgente)
* **Variables:** Dolor severo (EVA >= 7), Fiebre alta sostenida.
* **Justificación:** Basado en criterios de SIRS (Síndrome de Respuesta Inflamatoria Sistémica). Requiere valoración en < 60 minutos.

## Regla 4: Nivel VERDE / AZUL (Rutina)
* **Variables:** Ausencia de banderas rojas, dolor leve.
* **Justificación:** Paciente estable. Puede esperar valoración en consulta externa programada.