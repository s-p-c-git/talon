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

- Loading a quantized Llama model with ExecuTorch's `LlamaModule`
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
- Android Studio (latest) + Android NDK ≥ 28.0.12433566
- Java 17 JDK
- Python ≥ 3.10
- An Arm-powered Android phone with the `i8mm` CPU feature, ≥ 16GB RAM
- `adb` on your PATH

### 1. Build the ExecuTorch Android Archive (AAR)

```bash
git clone https://github.com/pytorch/executorch.git
cd executorch
export ANDROID_NDK=$ANDROID_HOME/ndk/29.0.14206865/
export ANDROID_ABI=arm64-v8a
export ANDROID_SDK=$ANDROID_HOME
sh scripts/build_android_library.sh
```

### 2. Export and quantize the model

```bash
cd ../talon
python3 -m venv .venv && source .venv/bin/activate
pip install -r scripts/requirements.txt
python3 scripts/export_model.py --model llama3_2-1B --quant 4bit-groupwise
```

This produces `model.pte` and `tokenizer.bin` — see `scripts/export_model.py`
for the exact quantization args used (4-bit groupwise PTQ, matching the
challenge's "Model size" and "Model speed" optimization categories).

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
