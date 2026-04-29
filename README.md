# SanaMovil

## Descripción del Proyecto
Sanamóvil es un sistema informático de apoyo a la decisión logística. Su objetivo principal es orientar al público general sobre el nivel de urgencia para buscar atención médica presencial y sugerir el tipo de acción logística adecuada (Traslado, Consulta, Monitoreo).

El proyecto integra modelos de Inteligencia Artificial de forma local mediante **Llama.cpp** (con modelos en formato GGML/GGUF) y librerías C++ para garantizar un procesamiento rápido y privado directamente en el dispositivo del usuario, sin depender de conexiones a internet para el análisis.

## Características Principales
* **Orientación Logística:** Evaluación interactiva del nivel de urgencia.
* **Inteligencia Artificial Local:** Integración de **Llama (llama.cpp / GGML)** para inferencia local de modelos de lenguaje directamente en el dispositivo.
* **Almacenamiento Local:** Persistencia de datos segura utilizando Room Database.
* **Interfaz de Usuario Moderna:** Interfaz fluida y responsiva desarrollada 100% con Jetpack Compose y Material Design 3.

## Tecnologías Utilizadas
* **Lenguajes:** Kotlin, C++17
* **UI/UX:** Jetpack Compose, Material3, Navigation Compose
* **Base de Datos:** Room Database (v2.6.1)
* **Inteligencia Artificial:** Llama.cpp (inferencia GGML/GGUF nativa mediante JNI)
* **Arquitectura:** MVVM, Lifecycle ViewModel/Runtime Compose
* **NDK/JNI:** CMake (compilación nativa de C/C++ para arquitectura `arm64-v8a`)

## Requisitos del Sistema
* **SDK Mínimo:** API 26 (Android 8.0)
* **SDK Objetivo:** API 35
* **Arquitectura Soportada:** `arm64-v8a` (Procesadores de 64 bits modernos)
* **Entorno de Desarrollo:** Android Studio con soporte para Gradle, NDK y CMake (v3.22.1).

## ⚠️ Descargo de Responsabilidad Regulatorio (Lo que NO ES Sanamóvil)
* **NO es un sistema de diagnóstico:** No emite juicios sobre patologías específicas ni interpreta imágenes médicas.
* **NO es un recomendador de tratamiento:** No prescribe ni dosifica medicamentos o terapias.
* **NO es soporte vital:** No controla dispositivos médicos físicos ni monitorea signos vitales en tiempo real.

Este software está diseñado únicamente como una herramienta de orientación logística. En caso de una emergencia real, los usuarios deben contactar a los servicios de emergencia locales inmediatamente.
