# Architecture — public demo vs. private application

## The boundary, in one picture

```
┌─────────────────────────────────────────────────────────┐
│  TALON (this repo — public, Apache 2.0)                  │
│                                                           │
│   MainActivity ──▶ MemoryBridge ──▶ [stubbed response]   │
│         │                                                │
│         ▼                                                │
│   ExecuTorch LlmModule (XNNPACK + KleidiAI)               │
│         │                                                │
│         ▼                                                │
│   BenchmarkHarness ──▶ results/benchmark_log.csv          │
└─────────────────────────────────────────────────────────┘
                          │
                          │  (local socket / loopback API,
                          │   not implemented in this repo)
                          ▼
┌─────────────────────────────────────────────────────────┐
│  A private local memory/retrieval service                │
│  (private repo — not part of this submission)            │
│                                                           │
│   Persists and indexes context locally on-device          │
│   Applies privacy classification before context is used   │
│   Not disclosed as part of this submission                │
└─────────────────────────────────────────────────────────┘
```

## Why the split

This submission's purpose is to demonstrate one thing well: on-device LLM
inference optimized for Arm silicon (ExecuTorch + XNNPACK + KleidiAI,
quantization, measured TTFT/tokens-per-sec). That's what Track 3 judges on.

The memory/retrieval layer above the dotted line — how context gets
stored, ranked, and classified before reaching the model — is a separate,
larger piece of engineering that predates this competition and isn't the
thing being evaluated here. Keeping it out of the public repo means:

- The demo repo is genuinely self-contained and buildable by anyone,
  satisfying the "must be functional" requirement without needing
  access to anything private
- The private system's design and implementation aren't disclosed as a
  side effect of a competition submission

## What `MemoryBridge` actually does here

In a full application, a query would be matched against stored context,
filtered, and returned as retrieved context before the LLM generation
step. In this repo, `MemoryBridge.kt` returns a small set of fixed,
representative strings instead of performing that retrieval. The call
shape (async, returns a context string, times out gracefully) mirrors a
real integration point, so the demo's control flow is representative even
though the retrieval logic isn't present.

## Where the two repos will diverge going forward

This demo repo is intentionally not built as a branch or fork of any
private repository, and its history won't be merged into one — it's a
standalone artifact scoped to this submission.
