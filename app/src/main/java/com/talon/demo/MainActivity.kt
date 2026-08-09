package com.talon.demo

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.pytorch.executorch.LlamaCallback
import org.pytorch.executorch.LlamaModule
import java.io.File

/**
 * NOTE ON EXECUTORCH API SURFACE
 * -------------------------------
 * LlamaModule's constructor/method signatures shown here follow the
 * pattern used in Meta's reference app (meta-pytorch/executorch-examples,
 * llm/android/LlamaDemo). ExecuTorch's Java/Kotlin API has changed across
 * releases — before building, diff this against the current LlamaDemo's
 * MainActivity/ModelRunner to catch any signature drift for the
 * ExecuTorch version you're pinned to.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var llamaModule: LlamaModule
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
        llamaModule = LlamaModule(modelPath, tokenizerPath, 0.8f /* temperature */)

        CoroutineScope(Dispatchers.IO).launch {
            val loadResult = llamaModule.load()
            runOnUiThread {
                statusView.text = if (loadResult == 0) "Model loaded" else "Load failed: $loadResult"
            }
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

            llamaModule.generate(
                fullPrompt,
                /* seqLen = */ 128,
                object : LlamaCallback {
                    override fun onResult(token: String) {
                        if (tokenCount == 0) firstTokenAt = System.currentTimeMillis()
                        tokenCount++
                        builder.append(token)
                        runOnUiThread { outputView.text = builder.toString() }
                    }

                    override fun onStats(stats: String) {
                        // ExecuTorch emits a JSON-ish stats string on completion;
                        // parse it if you need model-reported timing instead of
                        // the wall-clock timestamps captured here.
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
        llamaModule.stop()
        super.onDestroy()
    }
}
