package com.hytranslator.translator

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.codeshipping.llamakotlin.LlamaModel
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class DownloadState {
    data object Idle : DownloadState()
    data object Checking : DownloadState()
    data class Downloading(val progress: Int, val speed: String = "") : DownloadState()
    data class Error(val message: String) : DownloadState()
    data object Complete : DownloadState()
}

class HyMTEngine(private val context: Context) {

    companion object {
        private const val TAG = "HyMTEngine"
        private const val MODEL_DIR = "models/hy-mt"
        private const val MODEL_FILE = "Hy-MT1.5-1.8B-STQ1_0.gguf"
        private const val MS_URL = "https://modelscope.cn/models/AngelSlim/Hy-MT1.5-1.8B-1.25bit-GGUF/resolve/master/"
        private const val HF_URL = "https://huggingface.co/AngelSlim/Hy-MT1.5-1.8B-1.25bit-GGUF/resolve/main/"
    }

    private var model: LlamaModel? = null
    private val mutex = Mutex()
    private var isReady = false

    private val modelFile: File get() = File(context.filesDir, "$MODEL_DIR/$MODEL_FILE")

    fun isModelDownloaded(): Boolean = modelFile.exists() && modelFile.length() > 10_000_000

    suspend fun downloadModel(onProgress: (DownloadState) -> Unit) = withContext(Dispatchers.IO) {
        onProgress(DownloadState.Checking)
        try {
            modelFile.parentFile?.mkdirs()
            var ok = tryDl(MS_URL + MODEL_FILE, modelFile, onProgress)
            if (!ok) ok = tryDl(HF_URL + MODEL_FILE, modelFile, onProgress)
            if (!ok) throw Exception("download failed")
            if (modelFile.exists() && modelFile.length() > 10_000_000)
                onProgress(DownloadState.Complete)
            else onProgress(DownloadState.Error("file incomplete"))
        } catch (e: Exception) {
            onProgress(DownloadState.Error(e.message ?: "download error"))
        }
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isReady) return@withContext true
            try {
                if (!isModelDownloaded()) return@withContext false
                model = LlamaModel.load(modelFile.absolutePath) {
                    contextSize = 2048
                    threads = 4
                    temperature = 0.0f
                }
                isReady = true; Log.i(TAG, "Model loaded"); true
            } catch (e: Exception) { Log.e(TAG, "Model load failed", e); false }
        }
    }

    suspend fun translate(
        text: String,
        sourceLang: Language,
        targetLang: Language
    ): String = withContext(Dispatchers.IO) {
        if (!isReady) throw IllegalStateException("Model not initialized")

        val prompt = "translate ${sourceLang.hyCode} to ${targetLang.hyCode}: $text"
        val flow = model!!.generateStream(prompt)
        // Collect all tokens and join
        val sb = StringBuilder()
        flow.collect { token -> sb.append(token) }
        sb.toString().trim()
    }

    suspend fun translateBatch(
        texts: List<String>,
        sourceLang: Language,
        targetLang: Language
    ): List<String> = withContext(Dispatchers.IO) {
        texts.map { translate(it, sourceLang, targetLang) }
    }

    fun release() {
        model?.close()
        model = null
        isReady = false
    }

    private fun tryDl(urlStr: String, dest: File, cb: (DownloadState) -> Unit): Boolean = try {
        val con = (URL(urlStr).openConnection() as HttpURLConnection).apply { connectTimeout = 15000; readTimeout = 60000; connect() }
        val total = con.contentLengthLong
        val ins = con.inputStream; val out = FileOutputStream(dest)
        val buf = ByteArray(8192); var n: Int; var rd = 0L; val t0 = System.currentTimeMillis()
        while (ins.read(buf).also { n = it } != -1) {
            out.write(buf, 0, n); rd += n
            if (total > 0) {
                val pct = (rd * 100 / total).toInt()
                val el = (System.currentTimeMillis() - t0) / 1000
                cb(DownloadState.Downloading(pct, if (el > 0) "${rd / 1024 / el} KB/s" else ""))
            }
        }
        out.close(); ins.close(); con.disconnect(); true
    } catch (e: Exception) { Log.e(TAG, "dl fail", e); if (dest.exists()) dest.delete(); false }
}
