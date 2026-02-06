cmake_minimum_required(VERSION 3.22.1)

project("sanamovil")

# 1. BANDERAS DE OPTIMIZACIÓN
# C++17 es OBLIGATORIO para Llama
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -pthread")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -pthread")

# 2. CONFIGURACIÓN DE LOS MOTORES
# Apagamos tests y ejemplos para que compile rápido
set(WHISPER_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(WHISPER_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)

# 3. AGREGAR LOS SUB-PROYECTOS
# Importante: Los nombres deben coincidir con tus carpetas en 'cpp'
add_subdirectory(whisper)
add_subdirectory(llama)

# 4. TU LIBRERÍA PRINCIPAL
add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        native-lib.cpp      # Archivo 1 (Whisper)
        native-llama.cpp    # Archivo 2 (MedGemma) <- AGREGADO
)

# 5. INCLUDES (Para que encuentre los headers .h)
target_include_directories(
        ${CMAKE_PROJECT_NAME} PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}       # Para poder hacer "whisper/whisper.h"
        llama/include
        llama/src
        llama/common
        llama/ggml/include                # IMPORTANTE: Donde vive el ggml de Llama
)

# 6. ENLACE FINAL
target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        android
        log
        whisper
        llama
        ggml    # A veces necesario dependiendo de la versión
)