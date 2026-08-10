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

            waitFor(GENERATE_TIMEOUT_MS) { outputText(scenario).isNotBlank() }

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

    companion object {
        private const val MODEL_LOAD_TIMEOUT_MS = 120_000L
        private const val GENERATE_TIMEOUT_MS = 180_000L
    }
}
