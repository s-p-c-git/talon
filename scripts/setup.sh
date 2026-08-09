#!/usr/bin/env bash
# Sets up the ExecuTorch build environment and pulls the reference
# LlamaDemo app used as this project's base. Mirrors:
# https://learn.arm.com/learning-paths/mobile-graphics-and-gaming/build-llama3-chat-android-app-using-executorch-and-xnnpack/
set -euo pipefail

: "${ANDROID_HOME:?Set ANDROID_HOME before running this script}"

echo "==> Cloning ExecuTorch"
if [ ! -d "executorch" ]; then
  git clone https://github.com/pytorch/executorch.git
fi
cd executorch

export ANDROID_NDK="${ANDROID_HOME}/ndk/29.0.14206865/"
export ANDROID_ABI=arm64-v8a
export ANDROID_SDK="${ANDROID_HOME}"

echo "==> Building ExecuTorch Android AAR (KleidiAI enabled by default, ExecuTorch >=0.7)"
sh scripts/build_android_library.sh

cd ..

echo "==> Cloning reference LlamaDemo app (base for this project's app/ module)"
if [ ! -d "executorch-examples" ]; then
  git clone https://github.com/meta-pytorch/executorch-examples.git
fi

echo "==> Done. Next: run scripts/export_model.py to produce model.pte + tokenizer.bin"
