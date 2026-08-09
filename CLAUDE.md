# Project context for Claude Code

This file is a handoff note from a prior planning conversation (Claude
chat, not Code). Read this before making changes.

## What this is

**TALON** — a scoped ExecuTorch + XNNPACK + KleidiAI on-device LLM
inference demo, built for Track 3 (Mobile AI) of the Arm Create: AI
Optimization Challenge 2026. Deadline: **Aug 14, 2026 @ 4:00pm PDT.**

Full context: `README.md` (overview + setup), `docs/ARCHITECTURE.md`
(design boundary), `SUBMISSION_CHECKLIST.md` (what's left before
submitting).

## Hard constraints — do not violate these

1. **This repo stays standalone.** It must not reference, depend on, or
   be built as a branch/fork of any other project. `MemoryBridge.kt` is
   intentionally a mock stub — do not wire it to a real backend, do not
   import code from elsewhere, do not add naming or comments that tie
   this repo to any other codebase.
2. **No SDLC/governance tooling artifacts in this repo.** If Vishwakarma
   (Claude Council) or any other internal process framework is used to
   *help* build this — task breakdown, review passes, whatever — none of
   its process artifacts may land here: no ADR/CDR files, no agent names
   in commits or comments, no phase-tagged commit messages. `.gitignore`
   already excludes `.vishwakarma/` and `*ADR*`/`*CDR*` filenames, but
   that's a backstop, not a substitute for not creating them here in the
   first place. If you need that kind of planning artifact, keep it
   outside this working tree.
3. **Commit as the repo owner, not as a tool.** Git is already configured
   in this repo (`user.name`/`user.email` set locally) — don't override
   it with a different identity.
4. **Open-source decision is final.** This repo is going public under
   Apache 2.0 regardless of how the competition's (ambiguous, possibly
   contradictory) rules resolve — don't gate further work on that.

## Known gaps — verify before relying on them

- `MainActivity.kt`'s ExecuTorch API calls (`LlamaModule`, `LlamaCallback`)
  follow the pattern in Meta's reference app
  (`meta-pytorch/executorch-examples`, `llm/android/LlamaDemo`) as of
  when this was written, but **have not been compiled or run**. Diff
  against the current `LlamaDemo` source for your pinned ExecuTorch
  version before assuming these signatures are correct.
- No model weights are included (Llama weights are separately licensed
  via Meta). `scripts/export_model.py` is a template, not tested against
  a real checkpoint.
- `results/benchmark_log.csv` currently has only a header row — no real
  runs yet.
- Git history is a single tagged baseline commit
  (`v0.1.0-mvp-scaffold`) with no remote configured.

## Current checklist state

See `SUBMISSION_CHECKLIST.md` for the live list. As of this handoff:
open-source/licensing decision is settled (Apache 2.0, not blocked on
anything); everything else — build verification, benchmark numbers, demo
video — is still open.
