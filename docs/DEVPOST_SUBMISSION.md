# Devpost submission text (draft)

Copy-paste source for the Devpost form fields flagged open in
`SUBMISSION_CHECKLIST.md`. Draft as of 2026-08-10 — update the
"Current verification status" paragraph under Setup Instructions once a
physical device run actually happens; don't let this drift from what's
actually been verified.

## Project Overview

TALON demonstrates on-device LLM inference optimized for Arm mobile
silicon: ExecuTorch, XNNPACK, and KleidiAI running a quantized language
model directly on an Android phone, with a benchmark harness capturing
the metrics Track 3 asks for (model size, time to first token,
tokens/sec). It's a scoped, standalone proof point — the inference layer
only — with a stubbed integration point showing where a retrieval/memory
layer would sit in a larger application, without disclosing that private
system.

## Functionality / Output

- Loads a 4-bit groupwise-quantized `.pte` model via ExecuTorch's
  `LlmModule`, running inference through XNNPACK's Arm-optimized
  KleidiAI kernels (enabled by default since ExecuTorch 0.7).
- Two demo actions in the app: a single prompt ("Run Single Prompt") and
  a fixed 3-prompt benchmark suite ("Run Benchmark").
- `BenchmarkHarness` records time-to-first-token, tokens/sec, peak RSS,
  and model size to `results/benchmark_log.csv` on every run.
- `MemoryBridge` stub shows the call shape for a retrieval step in front
  of generation, without implementing or disclosing the private
  retrieval system it stands in for.
- Pilot model: **Qwen2.5-0.5B-Instruct** (Apache 2.0, ungated on Hugging
  Face) — the export pipeline is verified end to end in CI
  (`.github/workflows/build.yml`'s `qwen-export-pilot` job). Llama is
  supported as a drop-in swap (`--family llama`) once its license is
  accepted, since `LlmModule` only takes file paths — no app-code
  changes needed to switch models.
- Verified in CI, not just written: `assembleDebug` compiles against a
  real Android SDK; the export pipeline produces a real, working `.pte`;
  an Android-emulator instrumented test
  (`.github/workflows/emulator-smoke-test.yml`) proves the app installs,
  loads that `.pte`, and generates real output end to end.

## Setup Instructions

See [`README.md`](../README.md) Steps 1–5 for the full walkthrough.
Summary:

1. `./scripts/setup.sh` — clones the ExecuTorch checkout used for model
   export.
2. Export and quantize the model (Qwen2.5 pilot path, or Llama once
   licensed — see README Setup step 2).
3. `adb push` the resulting `model.pte` + tokenizer to
   `/data/local/tmp/llama` on-device.
4. `./gradlew :app:installDebug` (or open `app/` in Android Studio and
   run).
5. Tap **Run Single Prompt** or **Run Benchmark** in the app.

**Current verification status:** build, export, and app functionality
(via an Android emulator, not a physical device) are all CI-proven — see
the workflow runs in the repository's Actions tab. End-to-end
verification on a physical Arm64 Android device with `i8mm` support, and
the baseline-vs-KleidiAI benchmark comparison, are still pending (see
`SUBMISSION_CHECKLIST.md`) — that hardware-based step hasn't happened
yet.

## Optional demo video

Not yet recorded. Once a physical device is available, capture: app
launch → model load → a single prompt generating real output → the
benchmark suite running → `results/benchmark_log.csv` populated with
real numbers. Under 3 minutes, per Devpost's limit.
