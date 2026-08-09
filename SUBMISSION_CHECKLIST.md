# Submission checklist — Arm Create: AI Optimization Challenge 2026

Deadline: **Aug 14, 2026 @ 4:00pm PDT**

## Decided

- [x] Open-source regardless of which rule actually governs Track 3 —
      not waiting on organizer clarification. Repo is public, Apache 2.0
      (`LICENSE` + `NOTICE`-file attribution).

## Devpost required fields

- [ ] Repository URL — public, Apache 2.0 license file detectable in
      the repo's "About" section on GitHub
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
- [ ] `./gradlew :app:assembleDebug` actually run to completion — not yet;
      needs a real Android SDK (compileSdk 34) and network access to
      Google's Maven repo, neither available in the sandbox this was
      audited in
- [ ] `scripts/setup.sh` runs clean on a fresh checkout
- [ ] Model export via `scripts/export_model.py` produces a working
      `.pte` (needs licensed Llama weights — not obtained yet)
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

- [ ] Search the working tree for any process-tooling artifacts before
      the first public push: `grep -ril "vishwakarma\|adr\|cdr" .`
      (`.gitignore` already excludes `.vishwakarma/` and `*ADR*`/`*CDR*`
      files going forward, but that only stops new files — it doesn't
      retroactively clean anything already committed)
- [ ] Check commit author name/email and commit message text — if
      commits were made through an agent/tool with its own identity or
      naming conventions, squash or rewrite history before the repo goes
      public so authorship reads as your own, not a tool's
- [ ] Do a final `git log --all --oneline` skim right before making the
      repo public, since this is the one check that can't be automated
      away by .gitignore
