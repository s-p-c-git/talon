#!/usr/bin/env bash
# Builds a second ExecuTorch Android AAR with KleidiAI kernels disabled,
# for the baseline-vs-KleidiAI comparison in results/README.md. Only
# builds the OFF variant -- the ON side of the comparison is already
# covered by the default org.pytorch:executorch-android Maven artifact
# app/build.gradle.kts depends on, so there's no need to also rebuild
# that one from source.
#
# EXECUTORCH_XNNPACK_ENABLE_KLEIDI is a plain CMake option() (default
# ON) defined in tools/cmake/preset/default.cmake, not something baked
# into the Android CMake preset itself -- verified 2026-08-10 by reading
# that file directly, not assumed. That makes it a legitimate -D
# override on top of the same configure+build steps
# executorch/scripts/build_android_library.sh runs, so this script
# mirrors that script's cmake invocation (single ABI, arm64-v8a --
# that's the real target hardware, no need for x86_64 here) rather than
# forking/patching it. Re-diff against that script if this stops
# working after an ExecuTorch version bump, same as this project's other
# ExecuTorch-wrapping scripts.
#
# Run scripts/setup.sh first so the executorch/ checkout this script
# expects is present as a sibling directory. Requires ANDROID_HOME.
set -euo pipefail

: "${ANDROID_HOME:?Set ANDROID_HOME before running this script}"

REPO_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
EXECUTORCH_DIR="${REPO_ROOT}/executorch"
if [ ! -d "${EXECUTORCH_DIR}" ]; then
  echo "Expected an executorch checkout at ${EXECUTORCH_DIR} — run scripts/setup.sh first." >&2
  exit 1
fi

ANDROID_NDK="${ANDROID_HOME}/ndk/29.0.14206865/"
ANDROID_ABI=arm64-v8a
CMAKE_OUT="cmake-out-android-${ANDROID_ABI}"

cd "${EXECUTORCH_DIR}"

echo "==> Configuring with EXECUTORCH_XNNPACK_ENABLE_KLEIDI=OFF"
cmake . -DCMAKE_INSTALL_PREFIX="${CMAKE_OUT}" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK}/build/cmake/android.toolchain.cmake" \
  -DPYTHON_EXECUTABLE=python3 \
  --preset "android-${ANDROID_ABI}" \
  -DANDROID_PLATFORM=android-26 \
  -DEXECUTORCH_BUILD_EXTENSION_LLM=ON \
  -DEXECUTORCH_BUILD_EXTENSION_LLM_RUNNER=ON \
  -DEXECUTORCH_BUILD_LLAMA_JNI=ON \
  -DEXECUTORCH_XNNPACK_ENABLE_KLEIDI=OFF \
  -DFLATCC_ALLOW_WERROR=OFF \
  -DSUPPORT_REGEX_LOOKAHEAD=ON \
  -DCMAKE_BUILD_TYPE=Release \
  -B"${CMAKE_OUT}"

echo "==> Building (this is a full ExecuTorch native build — expect 15-25 min)"
cmake --build "${CMAKE_OUT}" -j"$(nproc)" --target install --config Release

mkdir -p "cmake-out-android-so/${ANDROID_ABI}"
cp "${CMAKE_OUT}"/extension/android/*.so "cmake-out-android-so/${ANDROID_ABI}/libexecutorch.so"

echo "==> Packaging AAR"
pushd extension/android/ >/dev/null
ANDROID_HOME="${ANDROID_HOME}" ./gradlew build
popd >/dev/null

mkdir -p "${REPO_ROOT}/app/libs"
cp extension/android/executorch_android/build/outputs/aar/executorch_android-debug.aar \
  "${REPO_ROOT}/app/libs/executorch-kleidi-off.aar"

echo "==> Done: app/libs/executorch-kleidi-off.aar"
echo "==> Build with: ./gradlew :app:assembleDebug -PkleidiOff=true"
