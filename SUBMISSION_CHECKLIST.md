# Submission checklist — Arm Create: AI Optimization Challenge 2026

Deadline: **Aug 14, 2026 @ 4:00pm PDT**

## Decided

- [x] Open-source regardless of which rule actually governs Track 3 —
      not waiting on organizer clarification. Repo is public, Apache 2.0
      (`LICENSE` + `NOTICE`-file attribution).

## Devpost required fields

- [x] Repository URL — public, Apache 2.0 license file detectable in
      the repo's "About" section on GitHub. `v0.1.0-mvp-scaffold` tag
      and a published "Initial scaffold" release both exist, pinned to
      the original scaffold commit.
- [ ] Project Overview — pull from `README.md` intro
- [ ] Functionality / Output — the ExecuTorch + KleidiAI inference path,
      MemoryBridge stub, benchmark harness and its CSV output
- [ ] Setup Instructions — README Steps 1–5, tested end-to-end on an
      actual Arm64 Android device before submitting (not just read
      through)
- [ ] Optional demo video (≤3 min, public on YouTube/Vimeo/Youku) —
      show the app loading the model, running a prompt, and the
      benchmark log populating

## Build-and-verify (do this yourself, not just read the README)

- [x] Source-level API audit: diffed `MainActivity.kt`'s ExecuTorch calls
      and `scripts/export_model.py`'s export CLI against the current
      pytorch/executorch and meta-pytorch/executorch-examples source
      (2026-08-09, sandboxed session with no Android SDK/device — see
      `REVIEW.md` entry for what was and wasn't possible to verify this
      way). Found and fixed real drift; see that entry for details.
- [x] `./gradlew :app:assembleDebug` actually run to completion — green
      via GitHub Actions CI (`.github/workflows/build.yml`, first
      genuinely successful run 2026-08-10; the dev sandbox itself still
      can't reach Google's Maven repo, so CI is how this gets verified
      going forward, not a one-off workaround). Caught and fixed a real
      defect on the first attempt: `executorch-android:1.1.0` didn't
      actually have `LlmCallback.onError()`/`Closeable.close()` despite
      GitHub main-branch source suggesting it did — bumped to `1.4.0`.
      See `REVIEW.md`.
- [x] `scripts/setup.sh` runs clean on a fresh checkout — verified
      2026-08-10 (default path, no `--build-aar`)
- [x] Model export via `scripts/export_model.py` produces a working
      `.pte` — **real, CI-compiled success 2026-08-10** (run
      `31394392855`, commit `a276286`), pilot model
      **Qwen2.5-0.5B-Instruct** (`--family qwen2_5`), after 8 rounds of
      real CI failures each fixed from actual error text/upstream source;
      see the `REVIEW.md` entry for the full list. `.pte` artifact
      uploaded (~440 MB zipped) via `.github/workflows/build.yml`'s
      `qwen-export-pilot` job. Llama remains fully supported
      (`--family llama`) once its license is accepted — untested in CI
      so far since Qwen2.5 is the default; swapping models needs zero
      app-code changes (`LlmModule` only takes file paths).
      `pytorch/Qwen3-4B-INT8-INT4`'s pre-exported `.pte` remains
      available as a second, already-exported fallback. **Still
      unverified from this environment** (network egress blocks
      `huggingface.co` and the rules domain): the framework-based (not
      Llama-specific) Track 3 eligibility claim this model choice rests
      on, and Qwen2.5-0.5B-Instruct's exact license per its HF model
      card — confirm both from an unrestricted environment before
      finalizing.
- [ ] App installs and runs on a physical Arm64 Android device with
      `i8mm` support
- [ ] Benchmark run completes and `results/benchmark_log.csv` populates
- [ ] Baseline-vs-KleidiAI comparison captured in `results/README.md`

## MVP scope discipline

This repo is deliberately narrow. Resist adding:
- Any part of the private memory/PII/ranking logic this demo sits in front of
- iOS support (Track 3 demo is Android-only for this submission)
- Multi-turn conversation state, persistence, or UI polish beyond what's
  needed to demonstrate the inference path and benchmark

If there's spare time before the deadline, spend it on the baseline-vs-
KleidiAI benchmark comparison and the demo video — those move the score
more than additional features do.

## Before making the repo public

If any part of this project's development uses internal SDLC/governance
tooling (agent names, ADR/CDR files, phase-tagged commits, or similar
process artifacts), none of that should appear in the public repo or
submission text — only the working code, docs, and results described
above.

- [x] Search the working tree for any process-tooling artifacts before
      the first public push: `grep -ril "vishwakarma\|adr\|cdr" .`
      (`.gitignore` already excludes `.vishwakarma/` and `*ADR*`/`*CDR*`
      files going forward, but that only stops new files — it doesn't
      retroactively clean anything already committed). Verified clean —
      no matches beyond this file's own policy text.
- [x] Check commit author name/email and commit message text — an
      agent-tool identity (`Claude <noreply@anthropic.com>`) leaked into
      commit authorship twice this session (local git config reverting
      unexpectedly between working-directory setups); caught both times
      via this exact check, amended, and force-pushed before the repo's
      history moved further. Worth re-running this check before every
      future push, not just once — it's not a one-time state.
- [ ] Do a final `git log --all --oneline` skim right before making the
      repo public, since this is the one check that can't be automated
      away by .gitignore
