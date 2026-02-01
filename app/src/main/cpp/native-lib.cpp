#include <jni.h>
#include <string>
#include <android/log.h>
#include "whisper/whisper.h" // Importamos el header de whisper

#define TAG "JNI_SANAMOVIL"

// Variable global para mantener el contexto de Whisper en memoria
// (En una app real, esto debería manejarse con más cuidado, pero para la demo está bien)
struct whisper_context *g_whisper_ctx = nullptr;

extern "C" {

// Esta es la función que JNI está buscando. Fíjate en el nombre largo.
JNIEXPORT jboolean JNICALL
Java_com_g022_sanamovil_MainActivity_loadModel(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPathStr) {

    // 1. Convertir el String de Kotlin a const char* de C++
    const char *model_path = env->GetStringUTFChars(modelPathStr, nullptr);

    if (model_path == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error al convertir ruta del modelo.");
        return JNI_FALSE;
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Intentando cargar modelo desde: %s", model_path);

    // 2. Liberar contexto previo si existe (para evitar fugas de memoria si recargamos)
    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }

    // 3. Inicializar Whisper desde el archivo
    struct whisper_context_params cparams = whisper_context_default_params();
    g_whisper_ctx = whisper_init_from_file_with_params(model_path, cparams);

    // 4. Liberar el string de C++ (limpieza de JNI)
    env->ReleaseStringUTFChars(modelPathStr, model_path);

    // 5. Verificar si cargó bien
    if (g_whisper_ctx == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Falló la inicialización de Whisper.");
        return JNI_FALSE;
    }

    __android_log_print(ANDROID_LOG_INFO, TAG, "¡Modelo Whisper cargado exitosamente en memoria!");
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL
Java_com_g022_sanamovil_MainActivity_transcribeAudio(
        JNIEnv *env,
jobject /* this */,
jfloatArray audioData) {

if (g_whisper_ctx == nullptr) {
return env->NewStringUTF("Error: Modelo no cargado.");
}

// 1. Obtener los datos de audio desde Java
jsize len = env->GetArrayLength(audioData);
jfloat *audio_buffer = env->GetFloatArrayElements(audioData, 0);

__android_log_print(ANDROID_LOG_INFO, TAG, "Procesando audio de %d muestras...", len);

// 2. Configurar parámetros de inferencia
whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
wparams.print_progress   = false;
wparams.language         = "es"; // Forzamos español
wparams.n_threads        = 4;    // Usamos 4 hilos para velocidad

// 3. Ejecutar la inferencia (Esto es lo que tarda unos segundos)
if (whisper_full(g_whisper_ctx, wparams, audio_buffer, len) != 0) {
env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);
return env->NewStringUTF("Error: Falló la transcripción.");
}

// 4. Recolectar el texto generado
std::string result_text = "";
const int n_segments = whisper_full_n_segments(g_whisper_ctx);
for (int i = 0; i < n_segments; ++i) {
const char *text = whisper_full_get_segment_text(g_whisper_ctx, i);
result_text += text;
}

// 5. Limpieza
env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);

__android_log_print(ANDROID_LOG_INFO, TAG, "Texto transcrito: %s", result_text.c_str());
return env->NewStringUTF(result_text.c_str());
}

} // extern "C"