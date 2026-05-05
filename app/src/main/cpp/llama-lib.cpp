#include <jni.h>
#include <string>
#include <vector>
#include <android/log.h>
#include <atomic>
#include "llama.h"

#define TAG "JNI_LLAMA"

struct llama_model *g_llama_model = nullptr;
struct llama_context *g_llama_ctx = nullptr;

std::atomic<bool> g_cancel_generation(false);

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

// 0

JNIEXPORT void JNICALL
Java_com_g022_sanamovil_MainActivity_cancelLlamaGeneration(JNIEnv *env, jobject) {
    g_cancel_generation = true;
    __android_log_print(ANDROID_LOG_INFO, TAG, "Señal de cancelación recibida desde Kotlin");
}

// 1. Carga del Modelo
JNIEXPORT jboolean JNICALL
Java_com_g022_sanamovil_MainActivity_loadLlamaModel(JNIEnv *env, jobject, jstring modelPathStr) {
    const char *model_path = env->GetStringUTFChars(modelPathStr, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Cargando Llama desde: %s", model_path);

    if (g_llama_ctx != nullptr) {
        llama_free(g_llama_ctx);
        llama_model_free(g_llama_model);
        g_llama_ctx = nullptr;
        g_llama_model = nullptr;
    }

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 99;
    g_llama_model = llama_model_load_from_file(model_path, model_params);

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

// 2. Generación REAL de Texto
JNIEXPORT jstring JNICALL
Java_com_g022_sanamovil_MainActivity_generateTextLlama(JNIEnv *env, jobject, jstring promptStr) {
    if (g_llama_ctx == nullptr) return env->NewStringUTF("Error: IA no cargada.");

    g_cancel_generation = false;

    llama_free(g_llama_ctx); // Destruye la memoria de la consulta anterior
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048; // El mismo tamaño que definiste al cargar el modelo
    g_llama_ctx = llama_init_from_model(g_llama_model, ctx_params); // Crea uno en blanco

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Iniciando inferencia real...");

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
    if (n_tokens >= 1024) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return env->NewStringUTF("Error: El historial clínico es demasiado largo para ser procesado.");
    }

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

    // PASO D: Bucle de generación y configuración del SAMPLER
    int n_cur = batch.n_tokens;
    int n_decode = 0;
    const int max_tokens = 300;
    std::string result_text = "";

    // INICIALIZAR LA CADENA DE MUESTREO (SAMPLER CHAIN)
    struct llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());

    // 1. Penalización de repetición (Evita los loops y el parroting)
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(
            llama_vocab_n_tokens(vocab),
            256,   // Evalúa los últimos 256 tokens
            1.18f, // Penalización de repetición (1.18 es ideal para Llama 3)
            0.0f
            ));

    // 2. Temperatura (0.3 para triaje médico: preciso pero no monótono)
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.3f));

    // 3. Top-P
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));

    while (n_decode < max_tokens) {

        if (g_cancel_generation) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Abortando bucle de generación de forma segura.");
            break;
        }
        // Generar el nuevo token usando el sampler en lugar de Greedy
        llama_token new_token_id = llama_sampler_sample(smpl, g_llama_ctx, batch.n_tokens - 1);

        // El sampler debe aceptar el token para penalizarlo en futuras iteraciones
        llama_sampler_accept(smpl, new_token_id);

        // Token de finalización
        if (new_token_id == llama_vocab_eos(vocab)) {
            break;
        }

        // Convertir ID a texto
        char buf[128];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n >= 0) {
            std::string piece(buf, n);
            result_text += piece;

            if (result_text.find("<end_of_turn>") != std::string::npos ||
                result_text.find("<|eot_id|>") != std::string::npos) {
                break;
            }
        }

        batch.n_tokens = 0;
        batch_add(batch, new_token_id, n_cur, { 0 }, true);

        if (llama_decode(g_llama_ctx, batch) != 0) {
            break;
        }

        n_cur++;
        n_decode++;
    }

    // Liberar memoria del sampler y el batch
    llama_sampler_free(smpl);
    llama_batch_free(batch);
    env->ReleaseStringUTFChars(promptStr, prompt);

    // Limpieza de tokens de cierre si se filtraron al texto
    size_t pos = result_text.find("<end_of_turn>");
    if (pos != std::string::npos) {
        result_text = result_text.substr(0, pos);
    }
    pos = result_text.find("<|eot_id|>");
    if (pos != std::string::npos) {
        result_text = result_text.substr(0, pos);
    }

    return env->NewStringUTF(result_text.c_str());
}

// 3. Generación en Streaming (Callback a Kotlin)
JNIEXPORT void JNICALL
Java_com_g022_sanamovil_MainActivity_generateTextLlamaStream(JNIEnv *env, jobject thiz, jstring promptStr, jobject callback) {
    if (g_llama_ctx == nullptr) return;

    g_cancel_generation = false;

    llama_free(g_llama_ctx); // Destruye la memoria de la consulta anterior
    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048; // El mismo tamaño que definiste al cargar el modelo
    g_llama_ctx = llama_init_from_model(g_llama_model, ctx_params); // Crea uno en blanco

    const char *prompt = env->GetStringUTFChars(promptStr, nullptr);

    jclass callbackClass = env->GetObjectClass(callback);
    jmethodID onTokenMethod = env->GetMethodID(callbackClass, "onToken", "(Ljava/lang/String;)V");

    const struct llama_vocab * vocab = llama_model_get_vocab(g_llama_model);

    std::vector<llama_token> tokens_list(strlen(prompt) + 4);
    int n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens_list.data(), tokens_list.size(), true, true);
    if (n_tokens < 0) {
        tokens_list.resize(-n_tokens);
        n_tokens = llama_tokenize(vocab, prompt, strlen(prompt), tokens_list.data(), tokens_list.size(), true, true);
    }
    tokens_list.resize(n_tokens);

    if (n_tokens >= 1024) {
        env->ReleaseStringUTFChars(promptStr, prompt);
        return;
    }

    llama_batch batch = llama_batch_init(512, 0, 1);
    for (int i = 0; i < n_tokens; i++) {
        batch_add(batch, tokens_list[i], i, { 0 }, false);
    }
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(g_llama_ctx, batch) != 0) {
        llama_batch_free(batch);
        env->ReleaseStringUTFChars(promptStr, prompt);
        return;
    }

    int n_cur = batch.n_tokens;
    int n_decode = 0;
    const int max_tokens = 300;

    // INICIALIZAR LA CADENA DE MUESTREO PARA STREAMING
    struct llama_sampler * smpl = llama_sampler_chain_init(llama_sampler_chain_default_params());
    llama_sampler_chain_add(smpl, llama_sampler_init_penalties(
            256,   // penalty_last_n: Evalúa los últimos 256 tokens
            1.18f, // penalty_repeat: El factor de penalización (1.18 es ideal)
            0.0f,  // penalty_freq: Penalización por frecuencia (apagado)
            0.0f   // penalty_present: Penalización por presencia (apagado)
    ));
    llama_sampler_chain_add(smpl, llama_sampler_init_temp(0.3f));
    llama_sampler_chain_add(smpl, llama_sampler_init_top_p(0.9f, 1));

    llama_sampler_chain_add(smpl, llama_sampler_init_dist(1234));

    while (n_decode < max_tokens) {

        if (g_cancel_generation) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Abortando bucle de generación de forma segura.");
            break;
        }

        // Generar el nuevo token usando el sampler
        llama_token new_token_id = llama_sampler_sample(smpl, g_llama_ctx, batch.n_tokens - 1);
        llama_sampler_accept(smpl, new_token_id);

        if (new_token_id == llama_vocab_eos(vocab)) {
            break;
        }

        char buf[128];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n >= 0) {
            std::string piece(buf, n);

            // Detener generación al detectar fin de turno
            if (piece.find("<end_of_turn>") != std::string::npos ||
                piece.find("<|eot_id|>") != std::string::npos) {
                break;
            }

            // Enviar fragmento a Kotlin
            jstring jPiece = env->NewStringUTF(piece.c_str());
            env->CallVoidMethod(callback, onTokenMethod, jPiece);
            env->DeleteLocalRef(jPiece);
        }

        batch.n_tokens = 0;
        batch_add(batch, new_token_id, n_cur, { 0 }, true);

        if (llama_decode(g_llama_ctx, batch) != 0) {
            break;
        }

        n_cur++;
        n_decode++;
    }

    // Liberar memoria
    llama_sampler_free(smpl);
    llama_batch_free(batch);
    env->ReleaseStringUTFChars(promptStr, prompt);
}

}