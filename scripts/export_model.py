#!/usr/bin/env python3
"""
Exports and quantizes an LLM (Llama or Qwen2.5) for ExecuTorch + XNNPACK +
KleidiAI.

This is a thin wrapper around ExecuTorch's own `export_llm` entry point.
Run `scripts/setup.sh` first so the `executorch` checkout this script
expects is present as a sibling directory.

Reference:
https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/build-llama3-chat-android-app-using-executorch-and-xnnpack/4-prepare-llama-models/
https://github.com/pytorch/executorch/blob/main/examples/models/llama/README.md
https://github.com/pytorch/executorch/blob/main/examples/models/qwen2_5/README.md

NOTE ON MODEL WEIGHTS
----------------------
Llama (--family llama): obtain weights directly from Meta's Llama
downloads page (or another source you're licensed to use) — this script
does not download or bundle any model weights.

Qwen2.5 (--family qwen2_5): the pilot path for this project, since the
0.5B/1.5B checkpoints are ungated on Hugging Face and (per their model
cards — confirm before relying on this) Apache-2.0 licensed, so there's
no manual license-acceptance wait. Two steps before this script can run:
  hf download Qwen/Qwen2.5-0.5B-Instruct --local-dir <dir>
  python scripts/convert_qwen_checkpoint.py <dir> <out.pth>
`<out.pth>` is then this script's --checkpoint. (Use our own
convert_qwen_checkpoint.py, not executorch's own
examples/models/qwen2_5/convert_weights.py — the latter imports the full
`torchtune` package just for one helper function, which pulls in a
torchao submodule incompatible with the torchao nightly ExecuTorch's own
install_requirements.py pins. Confirmed via CI failure 2026-08-10 — see
REVIEW.md.)

NOTE ON EXECUTORCH CLI SURFACE
-------------------------------
Verified 2026-08-10 against the current pytorch/executorch main branch
(config presets and READMEs, not a compiled run from this repo's sandbox
— see REVIEW.md for what CI still needs to confirm). The export entry
point is `extension.llm.export.export_llm`, taking a `--config <yaml>`
preset plus dotted-key overrides (Hydra/OmegaConf style:
`+base.checkpoint=...`, `quantization.group_size=...`). Qwen2.5 reuses
Llama's example code — same entry point, different model_class/params/
config preset — per `examples/models/qwen2_5/README.md`.
"""
import argparse
import subprocess
import sys
from pathlib import Path

EXECUTORCH_DIR = Path(__file__).resolve().parent.parent / "executorch"

# Dotted-key overrides layered on top of each family's config preset.
# Kept explicit here (rather than relying solely on the preset file) so
# the "4bit-groupwise" intent is visible in this script rather than
# hidden inside the executorch checkout.
FAMILY_PRESETS = {
    "llama": {
        "config": "examples/models/llama/config/llama_xnnpack.yaml",
        "overrides": [
            "quantization.qmode=8da4w",
            "quantization.group_size=128",
            "quantization.embedding_quantize=4,32",
        ],
    },
    "qwen2_5": {
        # Preset already sets qmode=8da4w; layer on group_size to match
        # this project's "4bit-groupwise" intent (preset leaves it unset).
        "config": "examples/models/qwen2_5/config/qwen2_5_xnnpack_q8da4w.yaml",
        "overrides": [
            "quantization.group_size=128",
        ],
    },
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--family", choices=FAMILY_PRESETS.keys(), default="qwen2_5")
    parser.add_argument("--model", required=True, help="e.g. qwen2_5_0_5b or llama3_2 (passed as base.model_class)")
    parser.add_argument("--checkpoint", required=True, help="Path to Meta-format .pth weights (see NOTE ON MODEL WEIGHTS)")
    parser.add_argument("--params", required=True, help="Path to the model's params.json")
    parser.add_argument("--quant", choices=["4bit-groupwise"], default="4bit-groupwise")
    parser.add_argument("--output-dir", default=".", help="Where to write model.pte")
    args = parser.parse_args()

    if not EXECUTORCH_DIR.exists():
        sys.exit(f"Expected an executorch checkout at {EXECUTORCH_DIR} — run scripts/setup.sh first.")

    preset = FAMILY_PRESETS[args.family]
    cmd = [
        sys.executable, "-m", "extension.llm.export.export_llm",
        "--config", preset["config"],
        f"+base.model_class={args.model}",
        f"+base.checkpoint={args.checkpoint}",
        f"+base.params={args.params}",
        f"+export.output_name={args.output_dir}/model.pte",
        *preset["overrides"],
    ]

    print("==> Running:", " ".join(cmd))
    subprocess.run(cmd, cwd=EXECUTORCH_DIR, check=True)
    print(f"==> Wrote {args.output_dir}/model.pte")
    print("==> Don't forget to also copy the model's tokenizer to "
          f"{args.output_dir}/tokenizer.bin before pushing to device.")


if __name__ == "__main__":
    main()
