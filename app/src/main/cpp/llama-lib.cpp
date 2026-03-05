#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include "llama.h"

#define TAG "JNI_LLAMA"

struct llama_model *g_llama_model = nullptr;
struct llama_context *g_llama_ctx = nullptr;

// Función auxiliar para agregar tokens al lote (batch) en la nueva versión de llama.cpp
void batch_add(struct llama_batch & batch, llama_token id, llama_pos pos, const std::vector<llama_seq_id> & seq_ids, bool logits) {
    batch.token   [batch.n_tokens] = id;
    batch.pos     [batch.n_tokens] = pos;
    batch.n_seq_id[batch.n_tokens] = seq_ids.size();
    for (size_t i = 0; i < seq_ids.size(); ++i) {
        batch.seq_id[batch.n_tokens][i] = seq_ids[i];
    }
    batch.logits  [batch.n_tokens] = logits;
    batch.n_tokens++;
}

extern "C" {

// 1. Carga del Modelo (Actualizado para la API v3)
JNIEXPORT jboolean JNICALL
Java_com_g022_sanamovil_MainActivity_loadLlamaModel(JNIEnv *env, jobject, jstring modelPathStr) {
    const char *model_path = env->GetStringUTFChars(modelPathStr, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Cargando Llama desde: %s", model_path);

    if (g_llama_ctx != nullptr) {
        llama_free(g_llama_ctx);
        llama_model_free(g_llama_model); // Nombre actualizado
        g_llama_ctx = nullptr;
        g_llama_model = nullptr;
    }

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 99;
    g_llama_model = llama_model_load_from_file(model_path, model_params); // Nombre actualizado

    if (g_llama_model == nullptr) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Falló al cargar Llama.");
        env->ReleaseStringUTFChars(modelPathStr, model_path);
        return JNI_FALSE;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    g_llama_ctx = llama_init_from_model(g_llama_model, ctx_params);

    env->ReleaseStringUTFChars(modelPathStr, model_path);
    return g_llama_ctx != nullptr ? JNI_TRUE : JNI_FALSE;
}

// 2. Generación REAL de Texto (Actualizado para usar llama_vocab)
JNIEXPORT jstring JNICALL
Java_com_g022_sanamovil_MainActivity_generateTextLlama(JNIEnv *env, jobject, jstring promptStr) {
    if (g_llama_ctx == nullptr) return env->NewStringUTF("Error: IA no cargada.");

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Iniciando inferencia real...");

    // En la nueva API, el vocabulario se extrae del modelo
    const struct llama_vocab * vocab = llama_model_get_vocab(g_llama_model);

    // PASO A: Convertir el texto a tokens
    std::vector<llama_token> tokens_list(strlen(prompt) + 4);
    int n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens_list.data(), tokens_list.size(), true, true);
    }
    tokens_list.resize(n_tokens);

    // PASO B: Preparar el Batch
    if (n_tokens >= 1024) {  // Ajustado al nuevo n_ctx
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("Error: El historial clínico es demasiado largo para ser procesado.");
    }

    // Batch size reducido para mejor rendimiento
    llama_batch batch = llama_batch_init(512, 0, 1);
    for (int i = 0; i < n_tokens; i++) {
        batch_add(batch, tokens_list[i], i, { 0 }, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    // PASO C: Procesar todo el prompt inicial
    if (llama_decode(g_llama_ctx, batch) != 0) {
        llama_batch_free(batch);
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("Error: Falló llama_decode");
    }

    // PASO D: Bucle de generación
    int n_cur = batch.n_tokens;
    int n_decode = 0;
    const int max_tokens = 300;  // Reducido de 600 -> respuestas más concisas y rápidas
    std::string result_text = "";

    while (n_decode < max_tokens) {
        auto * logits = llama_get_logits_ith(g_llama_ctx, batch.n_tokens - 1);
        int n_vocab = llama_vocab_n_tokens(vocab); // Nombre actualizado

        // Muestreo Greedy
        llama_token new_token_id = 0;
        float max_logit = -1e9;
        for (int i = 0; i < n_vocab; i++) {
            if (logits[i] > max_logit) {
                max_logit = logits[i];
                new_token_id = i;
            }
        }

        // Token de finalización
        if (new_token_id == llama_vocab_eos(vocab)) { // Nombre actualizado
            break;
        }

        // Convertir ID a texto
        char buf[128];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n >= 0) {
            std::string piece(buf, n);
            result_text += piece;

            if (result_text.find("<end_of_turn>") != std::string::npos) {
                break;
            }
        }

        // Limpiar el batch (llama_batch_clear ya no se usa, simplemente ponemos n_tokens a 0)
        batch.n_tokens = 0;
        batch_add(batch, new_token_id, n_cur, { 0 }, true);

        if (llama_decode(g_llama_ctx, batch) != 0) {
            break;
        }

        n_cur++;
        n_decode++;
    }

    llama_batch_free(batch);
    env->ReleaseStringUTFChars(promptStr, prompt);

    size_t pos = result_text.find("<end_of_turn>");
    if (pos != std::string::npos) {
        result_text = result_text.substr(0, pos);
    }

    return env->NewStringUTF(result_text.c_str());
}

}