#include <jni.h>
#include <string>
#include <android/log.h>
#include <vector>
#include <cstring>
#include "llama.h"

#define TAG "JNI_LLAMA"

// --- VARIABLES GLOBALES ---
static llama_model *model = nullptr;
static llama_context *ctx = nullptr;
static llama_sampler *sampler = nullptr; // NUEVO: Para elegir palabras

// --- AYUDANTES (Helpers que faltaban) ---

// Función manual para convertir JString a std::string
std::string jstring2string(JNIEnv *env, jstring jStr) {
    if (!jStr) return "";
    const char *cStr = env->GetStringUTFChars(jStr, nullptr);
    std::string str(cStr);
    env->ReleaseStringUTFChars(jStr, cStr);
    return str;
}

// Función manual para limpiar el batch (reemplaza a llama_batch_clear)
void batch_clear(struct llama_batch &batch) {
    batch.n_tokens = 0;
}

// Función manual para agregar al batch (reemplaza a llama_batch_add)
void batch_add(struct llama_batch &batch, llama_token id, llama_pos pos, std::vector<llama_seq_id> seq_ids, bool logits) {
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

// 1. CARGAR MODELO
JNIEXPORT jint JNICALL
Java_com_g022_sanamovil_MainActivity_loadMedGemma(JNIEnv *env, jobject, jstring model_path) {
    // Limpieza previa
    if (model) llama_free_model(model);
    if (ctx) llama_free(ctx);
    if (sampler) llama_sampler_free(sampler);

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    __android_log_print(ANDROID_LOG_INFO, TAG, "Cargando MedGemma desde: %s", path);

    llama_backend_init();

    llama_model_params model_params = llama_model_default_params();
    model_params.use_mmap = true;

    model = llama_load_model_from_file(path, model_params);
    env->ReleaseStringUTFChars(model_path, path);

    if (!model) {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "Error fatal: MedGemma no cargó.");
        return -1;
    }

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx = 2048;
    ctx_params.n_threads = 4;
    ctx_params.n_threads_batch = 4;

    ctx = llama_new_context_with_model(model, ctx_params);
    if (!ctx) return -2;

    // NUEVO: Inicializar el "Sampler" (el que elige la mejor palabra)
    sampler = llama_sampler_init_greedy(); // Modo "Greedy" (el más rápido y preciso para medicina)

    __android_log_print(ANDROID_LOG_INFO, TAG, "¡MedGemma Listo!");
    return 0;
}

// 2. RESPONDER (Lógica corregida)
JNIEXPORT jstring JNICALL
Java_com_g022_sanamovil_MainActivity_answerPrompt(JNIEnv *env, jobject, jstring promptStr) {
    if (!model || !ctx) return env->NewStringUTF("Error: Modelo no cargado.");

    std::string prompt = jstring2string(env, promptStr);
    const llama_vocab * vocab = llama_model_get_vocab(model);

    // A. Tokenizar
    // (Calculamos el tamaño necesario primero)
    int n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), NULL, 0, true, false);
    std::vector<llama_token> tokens_list(n_tokens);
    if (n_tokens > 0) {
        n_tokens = llama_tokenize(vocab, prompt.c_str(), prompt.length(), tokens_list.data(), tokens_list.size(), true, false);
    }

    // B. Preparar Batch
    // Iniciamos batch con capacidad suficiente
    llama_batch batch = llama_batch_init(2048, 0, 1); // Max tokens en batch

    // Cargar prompt en batch usando nuestra función manual
    for (int i = 0; i < n_tokens; i++) {
        batch_add(batch, tokens_list[i], i, { 0 }, false);
    }
    // El último token debe calcular logits para predecir el siguiente
    batch.logits[batch.n_tokens - 1] = true;

    if (llama_decode(ctx, batch) != 0) {
        return env->NewStringUTF("Error: Falló decode inicial.");
    }

    // C. Generación (Loop)
    std::string response_text = "";
    int max_new_tokens = 200;

    for (int i = 0; i < max_new_tokens; i++) {
        // 1. Usar el nuevo API de Sampler para obtener el token
        llama_token new_token_id = llama_sampler_sample(sampler, ctx, -1);

        // 2. Verificar fin (EOS)
        if (llama_token_is_eog(vocab, new_token_id)) {
            break;
        }

        // 3. Convertir a texto
        char buf[256];
        int n = llama_token_to_piece(vocab, new_token_id, buf, sizeof(buf), 0, true);
        if (n > 0) {
            std::string piece(buf, n);
            response_text += piece;
        }

        // 4. Preparar siguiente iteración
        batch_clear(batch); // Usamos nuestra función manual
        batch_add(batch, new_token_id, n_tokens + i, { 0 }, true);

        if (llama_decode(ctx, batch) != 0) {
            break;
        }
    }

    llama_batch_free(batch);
    return env->NewStringUTF(response_text.c_str());
}

// 3. LIMPIEZA
JNIEXPORT void JNICALL
Java_com_g022_sanamovil_MainActivity_unloadModel(JNIEnv *env, jobject) {
    if (sampler) llama_sampler_free(sampler);
    if (ctx) llama_free(ctx);
    if (model) llama_free_model(model);
    model = nullptr;
    ctx = nullptr;
    sampler = nullptr;
}
}