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

# 3. AGREGAR LOS SUB-PROYECTOS
# Importante: Los nombres coinciden con tus carpetas en 'cpp'
add_subdirectory(whisper)
add_subdirectory(llama.cpp)

# ==========================================
# 4. LIBRERÍA 1: WHISPER (sanamovil)
# ==========================================
add_library(
        sanamovil
        SHARED
        native-lib.cpp      # Archivo dedicado EXCLUSIVAMENTE a Whisper
)

target_include_directories(
        sanamovil PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}
        ${CMAKE_CURRENT_SOURCE_DIR}/whisper
)

target_link_libraries(
        sanamovil
        android
        log
        whisper
)

# ==========================================
# 5. LIBRERÍA 2: LLAMA (sana_llama)
# ==========================================
add_library(
        sana_llama
        SHARED
        llama-lib.cpp      # Archivo dedicado EXCLUSIVAMENTE a Llama
)

target_include_directories(
        sana_llama PRIVATE
        ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/include       # Para encontrar llama.h
        ${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp/ggml/include  # Para encontrar ggml.h
)

target_link_libraries(
        sana_llama
        android
        log
        llama              # El target interno de llama.cpp se llama 'llama'
)