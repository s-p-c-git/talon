# TALON — Development Review Log

A running, structured handoff between Claude Code (building) and Claude
chat (planning/reflecting) sessions on this project. Purpose: let a chat
session catch up on real progress and real decisions without re-reading
the whole codebase, and let Claude Code sessions know what was agreed
outside the repo before picking work back up.

## How to use this file

- **Claude Code**: append a new dated entry using the template below
  when wrapping up a work session — not after every small commit, but
  whenever a meaningful chunk of work or a decision worth remembering
  happened. Never edit or delete prior entries; this is a log, not a
  status board. Newest entry goes at the bottom.
- **Claude chat**: read this file — most recent entries first — at the
  start of a session that picks this project back up. Treat it as more
  current than anything said in an earlier chat conversation.
- **Either direction**: if something here contradicts `CLAUDE.md` or
  `SUBMISSION_CHECKLIST.md`, flag the contradiction explicitly in the
  next entry rather than silently picking one — those files should stay
  in sync, and drift between them is worth surfacing, not resolving
  quietly.

---

## Entry template

Copy this block for each new entry. Leave sections in even if the answer
is "none" or "no change" — an explicit "none" is worth more than an
omitted section, since it tells the next reader that was actually
checked rather than skipped.

```
### YYYY-MM-DD — <one-line summary of what this session covered>

**What changed**
-

**Decisions made this session**
(Decisions only, not implementation detail — the kind of thing someone
picking this up cold would need to know before continuing.)
-

**Deviations from CLAUDE.md constraints or the standing plan**
(If none, write "None." Don't skip this section.)
-

**Open questions / blockers**
-

**Checklist state**
(Reference SUBMISSION_CHECKLIST.md items that moved — don't duplicate
the whole checklist here, just what changed since the last entry.)
-

**Next session should start with**
-
```

## Pre-submission self-assessment

Fill this in once, close to submission — not every entry. Be genuinely
honest here; an inflated self-score is worse than useless since it's the
one section meant to catch problems before a judge does.

| Criterion | Points | Honest read |
|---|---|---|
| Technological Implementation | 40 | |
| User / Developer Experience | 15 | |
| Potential Impact | 20 | |
| "WOW" factor | 25 | |

---

## Log

### 2026-08-09 — Scaffold created and handed off

**What changed**
- Full MVP scaffold built from scratch: ExecuTorch/XNNPACK/KleidiAI
  wiring in `MainActivity.kt`, mock `MemoryBridge.kt`, `BenchmarkHarness.kt`,
  Gradle config, setup scripts, licensing files, `README.md`,
  `docs/ARCHITECTURE.md`, `SUBMISSION_CHECKLIST.md`, `CLAUDE.md`.
- Project renamed twice during planning: working name → TALON
  ("Torch-based ARm LLM On-device Node").
- Git initialized locally with correct authorship; baseline tagged
  `v0.1.0-mvp-scaffold`.

**Decisions made this session**
- Repo will be public, Apache 2.0, regardless of which competition rule
  (blanket vs. Track-3-specific) actually governs submission format —
  no longer blocked on organizer clarification.
- Repo is deliberately standalone: no reference to, or dependency on,
  any other project in this codebase, its docs, or its naming.
- Any internal SDLC/governance tooling (Vishwakarma) used to help build
  this must never appear in the repo, commit history, or submission
  text.

**Deviations from CLAUDE.md constraints or the standing plan**
- None — this entry establishes the baseline `CLAUDE.md` describes.

**Open questions / blockers**
- ExecuTorch API calls in `MainActivity.kt` are unverified against a
  real build — written from documentation/reference-app patterns, not
  compiled or run.
- No model weights obtained yet; `scripts/export_model.py` untested
  against a real checkpoint.
- No baseline-vs-KleidiAI benchmark numbers captured yet.
- No demo video yet.

**Checklist state**
- Open-source/licensing decision: resolved (see Decisions above).
- Everything else in `SUBMISSION_CHECKLIST.md` is still open.

**Next session should start with**
- Running `scripts/setup.sh`, confirming the ExecuTorch AAR builds, and
  diffing `MainActivity.kt` against the current
  `meta-pytorch/executorch-examples` `LlamaDemo` source before trusting
  any of its API calls.
