package com.talon.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.executorch.ExecutorchRuntimeException
import org.pytorch.executorch.extension.llm.LlmCallback
import org.pytorch.executorch.extension.llm.LlmModule
import java.io.File

/**
 * NOTE ON EXECUTORCH API SURFACE
 * -------------------------------
 * Verified 2026-08-09 against the current org.pytorch:executorch-android
 * Maven artifact source (pytorch/executorch, extension/android) and the
 * current meta-pytorch/executorch-examples LlamaDemo. As of that check:
 * the top-level `LlamaModule`/`LlamaCallback` classes referenced by older
 * docs have been renamed to `org.pytorch.executorch.extension.llm.LlmModule`
 * / `LlmCallback`; `load()` now returns Unit and throws
 * `ExecutorchRuntimeException` on failure instead of returning an error
 * code; the module implements `Closeable`. The 3-arg `LlmModule(path,
 * tokenizer, temperature)` constructor and the 3-arg
 * `generate(prompt, seqLen, callback)` overload used below are unchanged.
 * Re-diff before building against a newer pinned version, since these APIs
 * are marked `@Experimental` upstream and can change without notice.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var llmModule: LlmModule
    private lateinit var benchmark: BenchmarkHarness
    private val memoryBridge: MemoryBridge = MockMemoryBridge()

    private lateinit var outputView: TextView
    private lateinit var statusView: TextView

    private val modelDir = "/data/local/tmp/llama"
    private val modelPath = "$modelDir/model.pte"
    private val tokenizerPath = "$modelDir/tokenizer.bin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        outputView = findViewById(R.id.outputView)
        statusView = findViewById(R.id.statusView)
        benchmark = BenchmarkHarness(applicationContext)

        statusView.text = "Loading model..."
        llmModule = LlmModule(modelPath, tokenizerPath, 0.8f /* temperature */)

        CoroutineScope(Dispatchers.IO).launch {
            val loadStatus = try {
                llmModule.load()
                "Model loaded"
            } catch (e: ExecutorchRuntimeException) {
                "Load failed: ${e.message}"
            }
            runOnUiThread { statusView.text = loadStatus }
        }

        findViewById<Button>(R.id.runPromptButton).setOnClickListener {
            runPrompt("Explain KleidiAI in one sentence.")
        }

        findViewById<Button>(R.id.runBenchmarkButton).setOnClickListener {
            runBenchmarkSuite()
        }
    }

    private fun runPrompt(userPrompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val context = memoryBridge.retrieveContext(userPrompt)
            val fullPrompt = if (context != null) "[context: $context]\n$userPrompt" else userPrompt

            val startedAt = System.currentTimeMillis()
            var firstTokenAt = 0L
            var tokenCount = 0
            val builder = StringBuilder()

            llmModule.generate(
                fullPrompt,
                /* seqLen = */ 128,
                object : LlmCallback {
                    override fun onResult(result: String) {
                        if (tokenCount == 0) firstTokenAt = System.currentTimeMillis()
                        tokenCount++
                        builder.append(result)
                        runOnUiThread { outputView.text = builder.toString() }
                    }

                    override fun onStats(stats: String) {
                        // ExecuTorch emits a JSON stats string on completion;
                        // parse it if you need model-reported timing instead of
                        // the wall-clock timestamps captured here.
                    }

                    override fun onError(errorCode: Int, message: String) {
                        runOnUiThread { statusView.text = "Generation error $errorCode: $message" }
                    }
                }
            )

            val finishedAt = System.currentTimeMillis()
            val modelSizeMb = benchmark.modelSizeMb(modelPath)
            benchmark.record(
                promptId = fullPrompt.take(24),
                startedAtMs = startedAt,
                firstTokenAtMs = if (firstTokenAt > 0) firstTokenAt else finishedAt,
                finishedAtMs = finishedAt,
                tokensGenerated = tokenCount,
                modelSizeMb = modelSizeMb
            )
        }
    }

    private fun runBenchmarkSuite() {
        // Fixed prompt set for reproducible run-to-run comparisons.
        val prompts = listOf(
            "Summarize the benefits of on-device inference in two sentences.",
            "List three tradeoffs of 4-bit quantization.",
            "Explain what KleidiAI kernels accelerate."
        )
        prompts.forEach { runPrompt(it) }
        runOnUiThread {
            statusView.text = "Benchmark complete — see results/benchmark_log.csv " +
                "(app-external files dir: ${File(getExternalFilesDir(null), "benchmark_log.csv").path})"
        }
    }

    override fun onDestroy() {
        llmModule.stop()
        llmModule.close()
        super.onDestroy()
    }
}
