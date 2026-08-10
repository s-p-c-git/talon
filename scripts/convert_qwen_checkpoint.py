#!/usr/bin/env python3
"""
Converts a Qwen2.5 Hugging Face checkpoint (safetensors) to the Meta
checkpoint format ExecuTorch's export_llm expects.

This is a trimmed, self-contained reimplementation of
`executorch/examples/models/qwen2_5/convert_weights.py`, which in turn
imports `torchtune.models.convert_weights.get_mapped_key`. We vendor just
that function (and the Qwen2.5-specific key mapping) instead of importing
the full `torchtune` package, because `import torchtune` eagerly loads
`torchtune.datasets.multimodal`, which references
`torchao.dtypes.nf4tensor.NF4Tensor` — a module absent from the torchao
nightly `executorch/install_requirements.py` pins for the export path
itself. Confirmed via a real CI failure 2026-08-10 (see REVIEW.md), not
assumed. `get_mapped_key` is verified against the current
https://github.com/pytorch/torchtune source
(torchtune/models/convert_weights.py, BSD-3-Clause license — see NOTICE)
and the key mapping against
https://github.com/pytorch/executorch/blob/main/examples/models/qwen2_5/convert_weights.py
(same license family).

Usage: python3 scripts/convert_qwen_checkpoint.py <hf-checkpoint-dir> <output.pth>
"""
import argparse
import json
import os
import re
from typing import Dict

import torch
from safetensors.torch import load_file

# Verified against pytorch/torchtune's torchtune/models/convert_weights.py.
def get_mapped_key(key: str, mapping_dict: Dict[str, str]) -> str:
    try:
        if any(k.isdigit() for k in key.split(".")):
            abstract_key = re.sub(r"(\.\d+)", ".{}", key)
            layer_num = re.search(r"\d+", key).group(0)
            new_key = mapping_dict[abstract_key]
            new_key = new_key.format(layer_num)
        else:
            new_key = mapping_dict[key]
    except KeyError as e:
        raise Exception(
            f'Error converting the state dict. Found unexpected key: "{key}". '
            "Please make sure you're loading a checkpoint with the right format."
        ) from e
    return new_key


# Verified against pytorch/executorch's examples/models/qwen2_5/convert_weights.py.
_QWEN_2_5_FROM_META = {
    "tok_embeddings.weight": "model.embed_tokens.weight",
    "norm.weight": "model.norm.weight",
    "output.weight": "lm_head.weight",
    "layers.{}.attention.wq.weight": "model.layers.{}.self_attn.q_proj.weight",
    "layers.{}.attention.wq.bias": "model.layers.{}.self_attn.q_proj.bias",
    "layers.{}.attention.wk.weight": "model.layers.{}.self_attn.k_proj.weight",
    "layers.{}.attention.wk.bias": "model.layers.{}.self_attn.k_proj.bias",
    "layers.{}.attention.wv.weight": "model.layers.{}.self_attn.v_proj.weight",
    "layers.{}.attention.wv.bias": "model.layers.{}.self_attn.v_proj.bias",
    "layers.{}.attention.wo.weight": "model.layers.{}.self_attn.o_proj.weight",
    "layers.{}.attention_norm.weight": "model.layers.{}.input_layernorm.weight",
    "layers.{}.ffn_norm.weight": "model.layers.{}.post_attention_layernorm.weight",
    "layers.{}.feed_forward.w1.weight": "model.layers.{}.mlp.gate_proj.weight",
    "layers.{}.feed_forward.w2.weight": "model.layers.{}.mlp.down_proj.weight",
    "layers.{}.feed_forward.w3.weight": "model.layers.{}.mlp.up_proj.weight",
}


def qwen_2_5_hf_to_meta(state_dict: Dict[str, torch.Tensor]) -> Dict[str, torch.Tensor]:
    converted_state_dict = {}
    inverted_mapping_dict = {v: k for k, v in _QWEN_2_5_FROM_META.items()}

    for key, value in state_dict.items():
        new_key = get_mapped_key(key, inverted_mapping_dict)
        converted_state_dict[new_key] = value

    # Models with tied embeddings (0.5B, 1.5B) don't have a separate lm_head.weight.
    if "lm_head.weight" not in state_dict:
        converted_state_dict["output.weight"] = converted_state_dict["tok_embeddings.weight"]

    return converted_state_dict


def load_checkpoint_from_safetensors(input_dir: str) -> Dict:
    index_path = os.path.join(input_dir, "model.safetensors.index.json")
    if os.path.exists(index_path):
        with open(index_path, "r") as f:
            index = json.load(f)
        weight_map = index["weight_map"]
        checkpoint_shards = sorted(set(weight_map.values()))

        shard_to_keys = {}
        for weight_name, shard in weight_map.items():
            shard_to_keys.setdefault(shard, []).append(weight_name)

        merged_state_dict = {}
        for shard in checkpoint_shards:
            shard_data = load_file(os.path.join(input_dir, shard))
            for weight_name in shard_to_keys[shard]:
                merged_state_dict[weight_name] = shard_data[weight_name]
            del shard_data
        return merged_state_dict

    model_path = os.path.join(input_dir, "model.safetensors")
    if os.path.exists(model_path):
        return load_file(model_path)

    raise FileNotFoundError(f"Could not find safetensors checkpoint in {input_dir}")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("input_dir", help="Path to directory containing safetensors checkpoint files")
    parser.add_argument("output", help="Path to write the converted .pth checkpoint")
    args = parser.parse_args()

    print("Loading checkpoint...")
    sd = load_checkpoint_from_safetensors(args.input_dir)
    print("Converting checkpoint...")
    sd = qwen_2_5_hf_to_meta(sd)
    print("Saving checkpoint...")
    torch.save(sd, args.output)
    print(f"Done. Wrote {args.output}")


if __name__ == "__main__":
    main()
