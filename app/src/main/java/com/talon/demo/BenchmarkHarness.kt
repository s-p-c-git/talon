package com.talon.demo

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Captures the metrics the Arm AI Optimization Challenge asks for
 * ("Optimizations we will look for": model size, model speed, inference
 * server speed) for a fixed prompt set, and appends results to a CSV
 * log under the app's external files directory.
 *
 * This harness wraps calls to ExecuTorch's LlamaModule; it does not
 * implement generation itself.
 */
class BenchmarkHarness(private val context: Context) {

    data class RunResult(
        val promptId: String,
        val timeToFirstTokenMs: Long,
        val tokensGenerated: Int,
        val totalDecodeMs: Long,
        val peakRssMb: Long,
        val modelSizeMb: Long
    ) {
        val tokensPerSec: Double
            get() = if (totalDecodeMs > 0) tokensGenerated * 1000.0 / totalDecodeMs else 0.0
    }

    private val logFile: File by lazy {
        File(context.getExternalFilesDir(null), "benchmark_log.csv").also {
            if (!it.exists()) {
                it.writeText("timestamp,prompt_id,ttft_ms,tokens,decode_ms,tokens_per_sec,peak_rss_mb,model_size_mb\n")
            }
        }
    }

    /**
     * Wraps a single generation call, timing first-token latency and
     * overall decode throughput. [onToken] is invoked by the caller's
     * ExecuTorch token callback; pass along firstTokenAt/lastTokenAt
     * timestamps captured in that callback.
     */
    fun record(
        promptId: String,
        startedAtMs: Long,
        firstTokenAtMs: Long,
        finishedAtMs: Long,
        tokensGenerated: Int,
        modelSizeMb: Long
    ): RunResult {
        val ttft = firstTokenAtMs - startedAtMs
        val decodeMs = finishedAtMs - firstTokenAtMs
        val peakRss = peakRssMb()

        val result = RunResult(
            promptId = promptId,
            timeToFirstTokenMs = ttft,
            tokensGenerated = tokensGenerated,
            totalDecodeMs = decodeMs,
            peakRssMb = peakRss,
            modelSizeMb = modelSizeMb
        )
        appendToLog(result)
        return result
    }

    private fun peakRssMb(): Long {
        val info = Debug.MemoryInfo()
        Debug.getMemoryInfo(info)
        return info.totalPss / 1024L // PSS is in KB
    }

    private fun appendToLog(r: RunResult) {
        val ts = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date())
        logFile.appendText(
            "$ts,${r.promptId},${r.timeToFirstTokenMs},${r.tokensGenerated}," +
                "${r.totalDecodeMs},${"%.2f".format(r.tokensPerSec)}," +
                "${r.peakRssMb},${r.modelSizeMb}\n"
        )
    }

    fun modelSizeMb(modelPath: String): Long {
        val f = File(modelPath)
        return if (f.exists()) f.length() / (1024 * 1024) else -1L
    }
}
