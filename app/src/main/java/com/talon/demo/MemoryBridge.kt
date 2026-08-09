package com.talon.demo

import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Represents the integration point where a larger application would
 * retrieve filtered, ranked context from a local memory store before
 * handing a prompt to the LLM.
 *
 * The real retrieval implementation is not included in this repo — this
 * class exists so the demo's control flow is representative of a full
 * pipeline, while remaining fully functional and buildable with no
 * external or private dependency.
 *
 * Swap [MockMemoryBridge] for a real implementation that talks to your
 * own local memory service over a socket or loopback HTTP endpoint if
 * you have one; the interface is deliberately minimal.
 */
interface MemoryBridge {
    /**
     * Returns retrieved context for [query], or null if nothing relevant
     * was found / the lookup timed out. Must not block the caller
     * indefinitely — a memory lookup should never stall generation.
     */
    suspend fun retrieveContext(query: String, timeoutMs: Long = 250L): String?
}

class MockMemoryBridge : MemoryBridge {

    // Small, fixed set of representative snippets standing in for what
    // a real ranked pgvector lookup would return. No PII, no real user
    // data — this is illustrative only.
    private val sampleMemories = listOf(
        "User prefers concise answers with runnable code.",
        "Last session covered Arm KleidiAI kernel integration.",
        "Device profile: mid-tier Arm64 phone, i8mm supported."
    )

    override suspend fun retrieveContext(query: String, timeoutMs: Long): String? {
        return withTimeoutOrNull(timeoutMs) {
            // Simulated lookup latency, representative of an on-device
            // vector search over a small local index.
            delay(15L)
            sampleMemories.firstOrNull { memory ->
                query.split(" ").any { term ->
                    term.length > 3 && memory.contains(term, ignoreCase = true)
                }
            }
        }
    }
}
