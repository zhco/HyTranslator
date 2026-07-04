#include <jni.h>
#include <string>
#include <cstring>
#include <vector>
#include "llama.h"

static llama_model      *g_model = nullptr;
static llama_context    *g_ctx   = nullptr;
static const llama_vocab *g_vocab = nullptr;
static llama_sampler    *g_smpl  = nullptr;

extern "C" JNIEXPORT jboolean JNICALL
Java_com_hytranslator_engine_HyMTEngine_nativeLoadModel(
        JNIEnv *env, jobject, jstring modelPath) {
    const char *path = env->GetStringUTFChars(modelPath, nullptr);

    llama_model_params model_params = llama_model_default_params();
    model_params.n_gpu_layers = 0; // CPU only

    g_model = llama_model_load_from_file(path, model_params);
    if (!g_model) {
        env->ReleaseStringUTFChars(modelPath, path);
        return JNI_FALSE;
    }

    g_vocab = llama_model_get_vocab(g_model);

    llama_context_params ctx_params = llama_context_default_params();
    ctx_params.n_ctx   = 2048;
    ctx_params.n_batch = 512;

    g_ctx = llama_init_from_model(g_model, ctx_params);
    g_smpl = llama_sampler_init_greedy();

    env->ReleaseStringUTFChars(modelPath, path);
    return g_ctx ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_hytranslator_engine_HyMTEngine_nativeTranslate(
        JNIEnv *env, jobject, jstring prompt) {
    if (!g_model || !g_ctx) return env->NewStringUTF("");

    const char *prompt_c = env->GetStringUTFChars(prompt, nullptr);
    std::string input(prompt_c);
    env->ReleaseStringUTFChars(prompt, prompt_c);

    // Tokenize
    std::vector<llama_token> tokens(llama_n_ctx(g_ctx));
    int n_tokens = llama_tokenize(g_vocab, input.c_str(), input.size(),
                                   tokens.data(), tokens.size(), true, false);
    if (n_tokens < 0) return env->NewStringUTF("[tokenize error]");
    tokens.resize(n_tokens);

    // Evaluate initial prompt
    llama_batch batch = llama_batch_init(tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); i++) {
        batch.token[i]    = tokens[i];
        batch.pos[i]      = i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i]   = (i == tokens.size() - 1);
    }
    if (llama_decode(g_ctx, batch) != 0) {
        llama_batch_free(batch);
        return env->NewStringUTF("[decode error]");
    }
    llama_batch_free(batch);

    // Generate
    std::string result;
    const int max_tokens = 512;
    for (int i = 0; i < max_tokens; i++) {
        llama_token new_token = llama_sampler_sample(g_smpl, g_ctx, -1);
        if (llama_vocab_is_eog(g_vocab, new_token)) break;

        char buf[8];
        int n = llama_token_to_piece(g_vocab, new_token, buf, sizeof(buf), 0, true);
        if (n > 0) result.append(buf, n);

        // Prepare next single-token batch
        llama_batch single = llama_batch_init(1, 0, 1);
        single.token[0]    = new_token;
        single.pos[0]      = n_tokens + i;
        single.n_seq_id[0] = 1;
        single.seq_id[0][0] = 0;
        single.logits[0]   = true;
        if (llama_decode(g_ctx, single) != 0) {
            llama_batch_free(single);
            break;
        }
        llama_batch_free(single);
    }

    return env->NewStringUTF(result.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_hytranslator_engine_HyMTEngine_nativeFreeModel(
        JNIEnv *, jobject) {
    if (g_smpl)  { llama_sampler_free(g_smpl); g_smpl = nullptr; }
    if (g_ctx)   { llama_free(g_ctx);          g_ctx   = nullptr; }
    if (g_model) { llama_model_free(g_model);  g_model = nullptr; }
    g_vocab = nullptr;
}
