#include <jni.h>
#include <string>
#include <android/log.h>
// SOLO incluimos Llama aquí
#include "llama.h"

#define TAG "JNI_LLAMA"

// Variables globales solo para Llama
static llama_model *model = nullptr;
static llama_context *ctx = nullptr;

extern "C" {

// 3. Cargar MedGemma
JNIEXPORT jint JNICALL
Java_com_g022_sanamovil_MainActivity_loadMedGemma(JNIEnv *env, jobject, jstring model_path) {
    const char *path = env->GetStringUTFChars(model_path, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Cargando MedGemma desde: %s", path);

    // Inicializar backend
    llama_backend_init();

    // Parámetros de carga
    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true; // Importante para la RAM

    // Cargar modelo
    model = llama_load_model_from_file(path, model_params);

    if (model == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error fatal: MedGemma no cargó.");
        env->ReleaseStringUTFChars(model_path, path);
        return -1;
    }

    // Crear contexto (Memoria a corto plazo)
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 1024; // Tamaño de contexto (tokens)

    ctx = llama_new_context_with_model(model, ctx_params);

    if (ctx == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error: No se pudo crear contexto Llama.");
        llama_free_model(model);
        env->ReleaseStringUTFChars(model_path, path);
        return -2;
    }

    __android_log_print(ANDROID_LOG_INFO, TAG, "¡MedGemma Listo!");
    env->ReleaseStringUTFChars(model_path, path);
    return 0; // Éxito
}

}