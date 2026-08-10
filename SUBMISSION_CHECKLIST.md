# Submission checklist — Arm Create: AI Optimization Challenge 2026

Deadline: **Aug 14, 2026 @ 4:00pm PDT**

## Decided

- [x] Open-source regardless of which rule actually governs Track 3 —
      not waiting on organizer clarification. Repo is public, Apache 2.0
      (`LICENSE` + `NOTICE`-file attribution).
- [x] Track 3 Llama requirement: confirmed NOT required — eligibility is
      framework-based (ExecuTorch/ONNX Runtime/LiteRT/MediaPipe/etc.),
      never model-specific; Llama only appears in Arm's reference
      Learning Paths, not eligibility or judging criteria. Verified
      against the live rules page by a Claude-chat session with normal
      web access, 2026-08-10 — **not independently re-verified from
      this Code session**, since `huggingface.co` and the rules domain
      are both blocked by this sandbox's network egress proxy
      (confirmed via WebFetch `EGRESS_BLOCKED`, not just a `curl`
      failure). Worth a direct check from an unrestricted environment
      before treating this as fully closed.
- [x] Qwen2.5-0.5B-Instruct license confirmed Apache 2.0 (HF model card
      + LICENSE file); `pytorch/Qwen3-4B-INT8-INT4` fallback also
      Apache 2.0 (per the Qwen team's own blog: only the 2.5-generation
      3B/72B variants carry a different license, and Qwen3 dropped that
      exception entirely). Same provenance caveat as above — relayed
      from a Claude-chat session, not independently re-verified here.
- [x] `NOTICE` credits Qwen2.5 as the source of the distributed `.pte`
      model weights (added 2026-08-10) — the `.pte` uploaded via
      `qwen-export-pilot` CI artifacts (~440 MB) is a redistributed
      derivative of an Apache-2.0-licensed model, and Apache 2.0's
      NOTICE-carryforward obligation applies to it the same as it does
      to TALON's own code, not just to the vendored
      `convert_qwen_checkpoint.py` helper already credited there.

## Devpost required fields

- [x] Repository URL — public, Apache 2.0 license file detectable in
      the repo's "About" section on GitHub. `v0.1.0-mvp-scaffold` tag
      and a published "Initial scaffold" release both exist, pinned to
      the original scaffold commit.
- [x] Project Overview — drafted in `docs/DEVPOST_SUBMISSION.md`
      (2026-08-10), ready to paste into the Devpost form
- [x] Functionality / Output — drafted in `docs/DEVPOST_SUBMISSION.md`,
      accurately distinguishes what's CI-verified (build, export, an
      emulator-based functional smoke test) from what still needs a
      physical device (real KleidiAI performance)
- [ ] Setup Instructions — draft in `docs/DEVPOST_SUBMISSION.md` points
      to README Steps 1–5, but still needs the real thing this item
      actually requires: **tested end-to-end on an actual Arm64 Android
      device before submitting** (not just read through, and not
      satisfied by the emulator smoke test — see the checklist item
      above about physical Arm64 device)
- [ ] Optional demo video (≤3 min, public on YouTube/Vimeo/Youku) —
      show the app loading the model, running a prompt, and the
      benchmark log populating; needs the physical device first (see
      `docs/DEVPOST_SUBMISSION.md`'s note on this)

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
- [x] App installs, loads a real exported `.pte` + tokenizer, and
      generates real output — **functional smoke test passing in CI,
      2026-08-10** (`.github/workflows/emulator-smoke-test.yml`, run
      `31404727059`, commit `ed05755`: `BUILD SUCCESSFUL`, `Finished 1
      tests`, `0 failed`). **This is not the item below it.** GH-hosted
      runners are x86_64, so this runs on an x86_64 Android emulator —
      it proves the app/model-loading/generation code path genuinely
      works end to end, but exercises none of XNNPACK's KleidiAI
      Arm-specific kernels and says nothing about real performance.
      Still open: an actual physical Arm64 device with `i8mm` support
      for the item below.
- [ ] App installs and runs on a physical Arm64 Android device with
      `i8mm` support (KleidiAI kernels actually exercised — the item
      above is a CI stand-in for this, not a substitute)
- [ ] Benchmark run completes and `results/benchmark_log.csv` populates
- [ ] Baseline-vs-KleidiAI comparison captured in `results/README.md`.
      Prep work done 2026-08-10 so this is a flip-a-flag exercise once a
      device exists: `scripts/build_kleidi_off_aar.sh` builds a second
      AAR with `EXECUTORCH_XNNPACK_ENABLE_KLEIDI=OFF` (the "on" side
      just uses the existing default Maven artifact — no need to
      rebuild that from source too); `./gradlew :app:assembleDebug
      -PkleidiOff=true` builds the app against it. Real compile
      verification via `.github/workflows/kleidi-off-aar.yml`
      (`workflow_dispatch`, not run automatically — it's a ~20-30min
      native build) is [status TBD, trigger and check before trusting
      this line].

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
- [x] Check commit author name/email and commit message text before
      every push — not a one-time state. An agent-tool identity
      (`Claude <noreply@anthropic.com>`) leaked into commit authorship
      twice in an earlier session, caught reactively each time and
      fixed via `commit --amend` + force-push. **Root cause found
      2026-08-10** (previously just patched, not diagnosed): this
      repo's local `.git/config` has never actually had a `[user]`
      section — CLAUDE.md's assumption that identity is "already
      configured locally" was never true — so it silently falls back to
      this environment's global default every time unless every commit
      is explicitly scoped (`git -c user.name=... -c user.email=...`).
      That session used this scoping throughout, verified author +
      committer after every commit, and never leaked a bad-author
      commit — but the underlying repo config still isn't set, so the
      workaround has to be repeated every commit, every session, until
      the repo owner sets it directly (a call for them, not made
      unilaterally here — see `REVIEW.md`).
- [ ] Do a final `git log --all --oneline` skim right before making the
      repo public, since this is the one check that can't be automated
      away by .gitignore
