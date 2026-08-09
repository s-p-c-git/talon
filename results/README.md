# Benchmark results

`benchmark_log.csv` is appended to automatically by `BenchmarkHarness`
each time the demo's "Run Benchmark" button is used.

## Comparison to fill in before submission

Run the same prompt set twice — once with KleidiAI kernels enabled
(default since ExecuTorch 0.7, `-DEXECUTORCH_XNNPACK_ENABLE_KLEIDI=ON`)
and once with it explicitly disabled — and record both here. This is the
single strongest piece of evidence for the challenge's "Arm-specific
optimization" criterion: a direct before/after number on the same device.

| Config | Avg TTFT (ms) | Tokens/sec | Peak RSS (MB) | Model size (MB) |
|---|---|---|---|---|
| Baseline (KleidiAI off) | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| KleidiAI on | _fill in_ | _fill in_ | _fill in_ | _fill in_ |
| **Delta** | _fill in_ | _fill in_ | — | — |

Device used: _model, chipset, RAM — fill in_
Model: _fill in (e.g. Llama 3.2 1B, 4-bit groupwise)_
