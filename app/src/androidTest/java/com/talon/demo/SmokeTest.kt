package com.talon.demo

import android.widget.Button
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * CI functional smoke test, not an on-device Arm benchmark: runs on the
 * x86_64 Android emulator GH-hosted runners actually have, so it doesn't
 * exercise KleidiAI's Arm-specific kernels. It exists to prove the app
 * actually loads a real .pte + tokenizer (pushed to /data/local/tmp/llama
 * by build.yml's emulator-smoke-test job) and generates real output end
 * to end -- something no session has been able to verify without a
 * physical Arm64 device.
 */
@RunWith(AndroidJUnit4::class)
class SmokeTest {

    @Test
    fun modelLoadsAndGeneratesOutput() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            waitFor(MODEL_LOAD_TIMEOUT_MS) { statusText(scenario) == "Model loaded" }

            scenario.onActivity { activity ->
                activity.findViewById<Button>(R.id.runPromptButton).performClick()
            }

            // MainActivity streams tokens into outputView as they arrive, so
            // "non-blank" is true after just the first token while generate()
            // (up to 128 tokens) is still running in the background. Closing
            // the scenario at that point tears down the Activity -- and
            // llmModule.close() -- while generate() is mid-call, which
            // ExecuTorch correctly rejects (IllegalStateException: Cannot
            // close module while method is executing). Confirmed via CI
            // failure 2026-08-10. Wait for the streamed text to actually
            // stop changing instead of just appearing.
            waitForStableOutput(scenario, GENERATE_TIMEOUT_MS)

            val output = outputText(scenario)
            val status = statusText(scenario)
            assertTrue("Expected non-empty generated output, got: '$output'", output.isNotBlank())
            assertFalse(
                "Status reported a generation error: $status",
                status.contains("error", ignoreCase = true),
            )
        }
    }

    private fun statusText(scenario: ActivityScenario<MainActivity>): String {
        var text = ""
        scenario.onActivity { activity -> text = activity.findViewById<TextView>(R.id.statusView).text.toString() }
        return text
    }

    private fun outputText(scenario: ActivityScenario<MainActivity>): String {
        var text = ""
        scenario.onActivity { activity -> text = activity.findViewById<TextView>(R.id.outputView).text.toString() }
        return text
    }

    private fun waitFor(timeoutMs: Long, pollIntervalMs: Long = 500, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return
            Thread.sleep(pollIntervalMs)
        }
        throw AssertionError("Condition not met within ${timeoutMs}ms")
    }

    /** Waits for streamed output to stop changing, not just appear. */
    private fun waitForStableOutput(scenario: ActivityScenario<MainActivity>, timeoutMs: Long) {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastText = ""
        var stableSince = 0L
        while (System.currentTimeMillis() < deadline) {
            val text = outputText(scenario)
            if (text.isNotBlank()) {
                if (text != lastText) {
                    lastText = text
                    stableSince = System.currentTimeMillis()
                } else if (System.currentTimeMillis() - stableSince >= STABLE_WINDOW_MS) {
                    return
                }
            }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        throw AssertionError("Output did not stabilize within ${timeoutMs}ms (last seen: '$lastText')")
    }

    companion object {
        private const val MODEL_LOAD_TIMEOUT_MS = 120_000L
        private const val GENERATE_TIMEOUT_MS = 180_000L
        private const val STABLE_WINDOW_MS = 5_000L
        private const val POLL_INTERVAL_MS = 1_000L
    }
}
