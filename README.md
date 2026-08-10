# TALON — Torch-based ARm LLM On-device Node

**Arm Create: AI Optimization Challenge 2026 — Track 3: Mobile AI**

> **Naming note:** the "Torch-based" in TALON reflects this project's
> current inference stack (ExecuTorch, part of the PyTorch Edge
> ecosystem). If the underlying runtime ever moves away from
> ExecuTorch/PyTorch — e.g. to ONNX Runtime, LiteRT, or llama.cpp — this
> expansion should be revisited so the name doesn't misdescribe the
> stack. The initialism itself (TALON) is stack-independent and doesn't
> need to change even if the expansion's first word does.

## What this is

A minimal, standalone demo of an on-device LLM inference path for Arm
mobile hardware. It demonstrates the inference layer only: model loading
via ExecuTorch, Arm-optimized execution via XNNPACK + KleidiAI, and a
benchmark harness capturing the performance metrics the challenge asks
for (model size, time to first token, tokens/sec).

**This repo does not contain a full agent/memory system.** A stubbed
local-context interface (`MemoryBridge.kt`) shows where a retrieval step
would sit in front of generation in a larger application, returning fixed
mock data rather than performing real retrieval. The demo is fully
functional and buildable on its own, with no external or private
dependency.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the shape of that
boundary.

## What it demonstrates

- Loading a quantized on-device LLM with ExecuTorch's `LlmModule` — piloted
  with Qwen2.5 (ungated on Hugging Face, no license-acceptance wait; see
  Setup step 2), with Llama supported as a drop-in swap once its license
  is accepted. `LlmModule` only takes file paths, so switching models
  needs zero app-code changes.
- Arm-specific acceleration via XNNPACK with KleidiAI kernels
  (`-DEXECUTORCH_XNNPACK_ENABLE_KLEIDI=ON`, default since ExecuTorch 0.7)
- A benchmark harness measuring time-to-first-token, tokens/sec, and peak
  memory during generation — logged to `results/benchmark_log.csv`
- A stubbed local-context call pattern (`MemoryBridge`) showing how a
  retrieval step would sit in front of the generation call in a larger
  application, without depending on any specific implementation of it

## Setup

This project builds on top of Meta's reference ExecuTorch Android demo app
rather than reimplementing JNI/CMake wiring from scratch — see
[Arm's own Learning Path](https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/build-llama3-chat-android-app-using-executorch-and-xnnpack/)
for the canonical version of these steps.

### Prerequisites
- Android Studio (latest) or Android SDK (compileSdk 34) + Java 17 JDK
- Python ≥ 3.10
- An Arm-powered Android phone with the `i8mm` CPU feature, ≥ 16GB RAM
- `adb` on your PATH
- Android NDK ≥ 28.0.12433566 — only needed for the optional
  `scripts/setup.sh --build-aar` path (custom backends)

### 1. Set up the ExecuTorch checkout (for model export)

`app/build.gradle.kts` depends on the published `org.pytorch:executorch-android`
Maven artifact (XNNPACK + KleidiAI enabled by default since ExecuTorch 0.7)
— no AAR build from source needed for this project's default config.

```bash
./scripts/setup.sh
```

This clones a local `executorch/` checkout (needed by `scripts/export_model.py`
in the next step) and `executorch-examples/` (the reference LlamaDemo app,
kept as a live diff target for API drift — ExecuTorch's Java/Kotlin API is
`@Experimental` and has changed across releases; re-diff `MainActivity.kt`
against it before trusting the signatures if you're building against a
different pinned version).

Only pass `--build-aar` (and set `ANDROID_HOME`) if you need a non-default
backend, like QNN or Vulkan, that isn't in the published artifact.

### 2. Export and quantize the model

This project's pilot model is **Qwen2.5-0.5B-Instruct** — ungated on
Hugging Face (no Meta-license approval wait) and, per its model card,
Apache-2.0 licensed (confirm on the card before you rely on this). Llama
is fully supported too — see the `--family llama` branch below — but
requires accepting Meta's license first.

```bash
python3 -m venv .venv && source .venv/bin/activate
pip install -r scripts/requirements.txt

# Qwen2.5 pilot (no license wait):
huggingface-cli download Qwen/Qwen2.5-0.5B-Instruct --local-dir qwen2.5-0.5b-hf
python3 executorch/examples/models/qwen2_5/convert_weights.py qwen2.5-0.5b-hf qwen2.5-0.5b-meta.pth
python3 scripts/export_model.py --family qwen2_5 --model qwen2_5_0_5b \
  --checkpoint qwen2.5-0.5b-meta.pth \
  --params executorch/examples/models/qwen2_5/config/0_5b_config.json \
  --quant 4bit-groupwise

# Llama (once you've accepted Meta's license and downloaded weights):
python3 scripts/export_model.py --family llama --model llama3_2 \
  --checkpoint <path/to/consolidated.00.pth> --params <path/to/params.json> \
  --quant 4bit-groupwise
```

This produces `model.pte` — see `scripts/export_model.py` for the exact
quantization args used (4-bit groupwise PTQ via ExecuTorch's per-family
config presets, matching the challenge's "Model size" and "Model speed"
optimization categories). Copy the model's tokenizer files alongside it
(`tokenizer.json`/`tokenizer_config.json` for Qwen2.5, `tokenizer.model`
renamed to `tokenizer.bin` for Llama) before the next step.

### 3. Push model artifacts to device

```bash
adb shell mkdir -p /data/local/tmp/llama
adb push model.pte /data/local/tmp/llama/
adb push tokenizer.bin /data/local/tmp/llama/
```

### 4. Build and run

Open `app/` in Android Studio and run (`^R`), or from the command line:

```bash
./gradlew :app:installDebug
```

### 5. Run the benchmark

From the app's demo screen, tap **Run Benchmark**. This runs a fixed prompt
set through `BenchmarkHarness`, which records:

| Metric | Where it's logged |
|---|---|
| Time to first token (ms) | `results/benchmark_log.csv` |
| Tokens/sec (decode) | `results/benchmark_log.csv` |
| Peak RSS during generation (MB) | `results/benchmark_log.csv` |
| Model size on disk (MB) | printed at app launch |

## Submission mapping (Devpost requirements)

- **Project Overview / Functionality / Output** — see this README and
  `docs/ARCHITECTURE.md`
- **Setup Instructions** — Steps 1–5 above, tested against an Arm64
  Android device
- **Arm-specific optimization** — KleidiAI kernels via XNNPACK, 4-bit
  groupwise quantization, benchmarked TTFT/tokens-per-sec deltas vs.
  the un-accelerated baseline (see `results/`)
- **License** — Apache 2.0, see [`LICENSE`](LICENSE) and [`NOTICE`](NOTICE)

## What this repo is *not*

Not a general-purpose chat app, and not a complete product. It's a
scoped proof point for one layer — on-device inference — of a larger
application design. `docs/ARCHITECTURE.md` describes the shape of that
larger design at a conceptual level, without including its implementation.

## License

Apache License 2.0 — see [`LICENSE`](LICENSE). Attribution requirements
are in [`NOTICE`](NOTICE).
