#!/usr/bin/env bash
# Clones the ExecuTorch checkout scripts/export_model.py needs, and
# (optionally) builds a custom ExecuTorch Android AAR from source.
#
# As of ExecuTorch 1.1.0, org.pytorch:executorch-android is published to
# Maven Central with XNNPACK + KleidiAI enabled by default — app/build.gradle.kts
# depends on it directly, so building the AAR from source is NOT required
# for this project's default configuration. Only run this script with
# --build-aar if you need a non-default backend (e.g. QNN, Vulkan) that
# isn't in the published artifact.
#
# Verified 2026-08-09 against pytorch/executorch main and the current
# meta-pytorch/executorch-examples LlamaDemo app.build.gradle.kts.
set -euo pipefail

BUILD_AAR=false
for arg in "$@"; do
  case "$arg" in
    --build-aar) BUILD_AAR=true ;;
    *) echo "Unknown argument: $arg" >&2; exit 1 ;;
  esac
done

echo "==> Cloning ExecuTorch (needed for scripts/export_model.py)"
if [ ! -d "executorch" ]; then
  # extension/llm/tokenizers is a submodule install_requirements.py
  # pip-installs directly -- without it, that directory is empty and
  # the install fails with "not installable" (found the hard way via
  # CI, 2026-08-10). A blanket --recurse-submodules --shallow-submodules
  # tries to shallow-fetch every registered submodule though, including
  # third-party/ao (TorchAO) -- which isn't needed here and can fail a
  # shallow fetch if its pinned commit has aged out of reach (also found
  # via CI, 2026-08-10). Scope the submodule init to just the one
  # submodule actually needed instead.
  git clone --filter=blob:none https://github.com/pytorch/executorch.git
  (cd executorch && git submodule update --init --recursive --depth 1 -- extension/llm/tokenizers)
fi

if [ "$BUILD_AAR" = true ]; then
  : "${ANDROID_HOME:?Set ANDROID_HOME before running with --build-aar}"
  cd executorch

  export ANDROID_NDK="${ANDROID_HOME}/ndk/29.0.14206865/"
  export ANDROID_ABI=arm64-v8a
  export ANDROID_SDK="${ANDROID_HOME}"

  echo "==> Building ExecuTorch Android AAR from source (KleidiAI enabled by default, ExecuTorch >=0.7)"
  sh scripts/build_android_library.sh
  echo "==> AAR built. Copy it to app/libs/executorch.aar and switch"
  echo "    app/build.gradle.kts back to the files(\"libs/executorch.aar\") dependency."

  cd ..
fi

echo "==> Cloning reference LlamaDemo app (kept as a live diff target for API drift)"
if [ ! -d "executorch-examples" ]; then
  git clone https://github.com/meta-pytorch/executorch-examples.git
fi

echo "==> Done. Next: run scripts/export_model.py to produce model.pte + tokenizer.bin"
