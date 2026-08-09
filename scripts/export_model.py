#!/usr/bin/env python3
"""
Exports and quantizes a Llama model for ExecuTorch + XNNPACK + KleidiAI.

This is a thin wrapper around ExecuTorch's own `export_llm` entry point.
Run `scripts/setup.sh` first so the `executorch` checkout this script
expects is present as a sibling directory.

Reference:
https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/build-llama3-chat-android-app-using-executorch-and-xnnpack/4-prepare-llama-models/
https://github.com/pytorch/executorch/blob/main/examples/models/llama/README.md

NOTE: You must obtain model weights directly from Meta's Llama downloads
page (or another source you're licensed to use) — this script does not
download or bundle any model weights.

NOTE ON EXECUTORCH CLI SURFACE
-------------------------------
Verified 2026-08-09 against the current pytorch/executorch main branch.
The export entry point moved from `examples.models.llama.export_llama`
with flat `-qmode`/`-group_size`/`-embedding-quantize` CLI flags to
`extension.llm.export.export_llm`, which takes a `--config <yaml>` preset
plus dotted-key overrides (Hydra/OmegaConf style: `+base.checkpoint=...`,
`quantization.group_size=...`). `examples/models/llama/config/llama_xnnpack.yaml`
is the current preset matching this project's intent (KV cache + SDPA +
XNNPACK with extended ops + 8da4w quantization, group_size 128, embedding
quantize 4,32) — re-check that config file before relying on the presets
below if you've re-run `scripts/setup.sh` against a newer executorch
checkout, since these are `@Experimental`-adjacent and can change.
"""
import argparse
import subprocess
import sys
from pathlib import Path

EXECUTORCH_DIR = Path(__file__).resolve().parent.parent / "executorch"

# Dotted-key overrides layered on top of each config preset's own
# quantization.* defaults. Kept explicit here (rather than relying solely
# on the preset file) so the "4bit-groupwise" intent is visible in this
# script rather than hidden inside the executorch checkout.
QUANT_PRESETS = {
    "4bit-groupwise": {
        "config": "examples/models/llama/config/llama_xnnpack.yaml",
        "overrides": [
            "quantization.qmode=8da4w",
            "quantization.group_size=128",
            "quantization.embedding_quantize=4,32",
        ],
    },
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", required=True, help="e.g. llama3_2 (passed as base.model_class)")
    parser.add_argument("--checkpoint", required=True, help="Path to downloaded .pth/.safetensors weights")
    parser.add_argument("--params", required=True, help="Path to the model's params.json")
    parser.add_argument("--quant", choices=QUANT_PRESETS.keys(), default="4bit-groupwise")
    parser.add_argument("--output-dir", default=".", help="Where to write model.pte")
    args = parser.parse_args()

    if not EXECUTORCH_DIR.exists():
        sys.exit(f"Expected an executorch checkout at {EXECUTORCH_DIR} — run scripts/setup.sh first.")

    preset = QUANT_PRESETS[args.quant]
    cmd = [
        sys.executable, "-m", "extension.llm.export.export_llm",
        "--config", preset["config"],
        f"+base.model_class={args.model}",
        f"+base.checkpoint={args.checkpoint}",
        f"+base.params={args.params}",
        f"export.output_name={args.output_dir}/model.pte",
        *preset["overrides"],
    ]

    print("==> Running:", " ".join(cmd))
    subprocess.run(cmd, cwd=EXECUTORCH_DIR, check=True)
    print(f"==> Wrote {args.output_dir}/model.pte")
    print("==> Don't forget to also copy the model's tokenizer to "
          f"{args.output_dir}/tokenizer.bin before pushing to device.")


if __name__ == "__main__":
    main()
