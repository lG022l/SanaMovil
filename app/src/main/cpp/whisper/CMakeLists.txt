cmake_minimum_required(VERSION 3.22.1)

project(whisper)

# 1. Definimos los archivos fuente manualmente
# (Porque no tenemos la estructura compleja original)
set(WHISPER_SOURCES
        whisper.cpp
        ggml.c
        ggml-alloc.c
        ggml-backend.c
        ggml-quants.c
)

# 2. Creamos la librería
add_library(whisper ${WHISPER_SOURCES})

# 3. Directorios de cabecera (Header files)
# Le decimos que busque .h en la carpeta actual (.)
target_include_directories(whisper PRIVATE .)

# 4. Enlazamos con Android
target_link_libraries(whisper android log)