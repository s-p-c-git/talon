#!/usr/bin/env python3
"""
Exports and quantizes a Llama model for ExecuTorch + XNNPACK + KleidiAI.

This is a thin wrapper around ExecuTorch's own `export_llama` entry point.
Run `scripts/setup.sh` first so the `executorch` checkout this script
expects is present as a sibling directory.

Reference:
https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/build-llama3-chat-android-app-using-executorch-and-xnnpack/4-prepare-llama-models/

NOTE: You must obtain model weights directly from Meta's Llama downloads
page (or another source you're licensed to use) — this script does not
download or bundle any model weights.
"""
import argparse
import subprocess
import sys
from pathlib import Path

EXECUTORCH_DIR = Path(__file__).resolve().parent.parent / "executorch"

QUANT_PRESETS = {
    # 4-bit groupwise per-token dynamic quantization of linear layers —
    # the configuration used in Arm's own benchmark numbers for this
    # model family. Reduces on-disk/in-memory size (the challenge's
    # "Model size" optimization category) with a small accuracy cost.
    "4bit-groupwise": [
        "-qmode", "8da4w",
        "-group_size", "128",
        "-embedding-quantize", "4,32",
    ],
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", required=True, help="e.g. llama3_2-1B")
    parser.add_argument("--checkpoint", required=True, help="Path to downloaded .pth/.safetensors weights")
    parser.add_argument("--params", required=True, help="Path to the model's params.json")
    parser.add_argument("--quant", choices=QUANT_PRESETS.keys(), default="4bit-groupwise")
    parser.add_argument("--output-dir", default=".", help="Where to write model.pte")
    args = parser.parse_args()

    if not EXECUTORCH_DIR.exists():
        sys.exit(f"Expected an executorch checkout at {EXECUTORCH_DIR} — run scripts/setup.sh first.")

    cmd = [
        sys.executable, "-m", "examples.models.llama.export_llama",
        "--checkpoint", args.checkpoint,
        "--params", args.params,
        "-kv",
        "--use_sdpa_with_kv_cache",
        "-X",  # enable XNNPACK delegate
        "--xnnpack-extended-ops",
        *QUANT_PRESETS[args.quant],
        "--output_name", f"{args.output_dir}/model.pte",
    ]

    print("==> Running:", " ".join(cmd))
    subprocess.run(cmd, cwd=EXECUTORCH_DIR, check=True)
    print(f"==> Wrote {args.output_dir}/model.pte")
    print("==> Don't forget to also copy the model's tokenizer to "
          f"{args.output_dir}/tokenizer.bin before pushing to device.")


if __name__ == "__main__":
    main()
