cmake_minimum_required(VERSION 3.22.1)

project("sanamovil")

# Banderas de optimización estándar
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O3 -pthread")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -pthread")

set(WHISPER_SOURCES
        whisper/ggml.c
        whisper/ggml-alloc.c
        whisper/ggml-backend.c
        whisper/ggml-quants.c
        whisper/whisper.cpp
)

add_library(
        ${CMAKE_PROJECT_NAME}
        SHARED
        native-lib.cpp
        ${WHISPER_SOURCES}
)

target_include_directories(
        ${CMAKE_PROJECT_NAME} PRIVATE
        whisper
)

target_link_libraries(
        ${CMAKE_PROJECT_NAME}
        android
        log
)