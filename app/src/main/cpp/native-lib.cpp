#include <jni.h>
#include <string>
#include <android/log.h>
#include "whisper/whisper.h" // Importamos el header de whisper

#define TAG "JNI_SANAMOVIL"

// Variable global para mantener el contexto de Whisper en memoria
struct whisper_context *g_whisper_ctx = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_com_g022_sanamovil_MainActivity_loadModel(
        JNIEnv *env,
        jobject /* this */,
        jstring modelPathStr) {

    // Conversion de String de Kotlin a const char* de C++
    const char *model_path = env->GetStringUTFChars(modelPathStr, nullptr);

    if (model_path == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error al convertir ruta del modelo.");
        return JNI_FALSE;
    }

    __android_log_print(ANDROID_LOG_DEBUG, TAG, "Intentando cargar modelo desde: %s", model_path);

    // Liberacion de contexto previo
    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }

    // Inicializacion de Whispe
    struct whisper_context_params cparams = whisper_context_default_params();
    g_whisper_ctx = whisper_init_from_file_with_params(model_path, cparams);

    // Limpieza de JNI
    env->ReleaseStringUTFChars(modelPathStr, model_path);

    // Verificación
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

// Datos de audio desde Java
jsize len = env->GetArrayLength(audioData);
jfloat *audio_buffer = env->GetFloatArrayElements(audioData, 0);

__android_log_print(ANDROID_LOG_INFO, TAG, "Procesando audio de %d muestras...", len);

// Parámetros de inferencia
whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
wparams.print_progress   = false;
wparams.language         = "es"; // Forzamos español
wparams.n_threads        = 4;    // Usamos 4 hilos para velocidad

// Ejecucion de inferencia
if (whisper_full(g_whisper_ctx, wparams, audio_buffer, len) != 0) {
env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);
return env->NewStringUTF("Error: Falló la transcripción.");
}

// Recoleccion del texto generado
std::string result_text = "";
const int n_segments = whisper_full_n_segments(g_whisper_ctx);
for (int i = 0; i < n_segments; ++i) {
const char *text = whisper_full_get_segment_text(g_whisper_ctx, i);
result_text += text;
}

// Limpieza
env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);

__android_log_print(ANDROID_LOG_INFO, TAG, "Texto transcrito: %s", result_text.c_str());
return env->NewStringUTF(result_text.c_str());
}

}