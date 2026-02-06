#include <jni.h>
#include <string>
#include <android/log.h>
// SOLO incluimos Whisper aquí para evitar conflictos
#include "whisper/whisper.h"

#define TAG "JNI_WHISPER"

// Variable global solo para Whisper
struct whisper_context *g_whisper_ctx = nullptr;

extern "C" {

// 1. Cargar Modelo Whisper
JNIEXPORT jboolean JNICALL
Java_com_g022_sanamovil_MainActivity_loadModel(JNIEnv *env, jobject, jstring modelPathStr) {
    const char *model_path = env->GetStringUTFChars(modelPathStr, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Cargando Whisper desde: %s", model_path);

    if (g_whisper_ctx != nullptr) {
        whisper_free(g_whisper_ctx);
        g_whisper_ctx = nullptr;
    }

    struct whisper_context_params cparams = whisper_context_default_params();
    g_whisper_ctx = whisper_init_from_file_with_params(model_path, cparams);

    env->ReleaseStringUTFChars(modelPathStr, model_path);

    if (g_whisper_ctx == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Falló al cargar Whisper.");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

// 2. Transcribir Audio
JNIEXPORT jstring JNICALL
Java_com_g022_sanamovil_MainActivity_transcribeAudio(JNIEnv *env, jobject, jfloatArray audioData) {
    if (g_whisper_ctx == nullptr) return env->NewStringUTF("Error: Whisper no cargado.");

    jsize len = env->GetArrayLength(audioData);
    jfloat *audio_buffer = env->GetFloatArrayElements(audioData, 0);

    whisper_full_params wparams = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    wparams.print_progress = false;
    wparams.language = "es";
    wparams.n_threads = 4;

    if (whisper_full(g_whisper_ctx, wparams, audio_buffer, len) != 0) {
        env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);
        return env->NewStringUTF("Error en Transcripción");
    }

    std::string result_text = "";
    const int n_segments = whisper_full_n_segments(g_whisper_ctx);
    for (int i = 0; i < n_segments; ++i) {
        result_text += whisper_full_get_segment_text(g_whisper_ctx, i);
    }

    env->ReleaseFloatArrayElements(audioData, audio_buffer, 0);
    return env->NewStringUTF(result_text.c_str());
}

}