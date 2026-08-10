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

### 2026-08-09 — Repo made standalone on GitHub; API-drift audit and build scaffolding

**What changed**
- Pushed this project to its own public GitHub repo, `s-p-c-git/talon`,
  preserving the existing 3-commit history and `v0.1.0-mvp-scaffold` tag
  rather than re-committing (tag push itself hit a permission error on
  the hosting side — branch pushed fine, tag needs to be created manually
  via the GitHub UI pointing at the `Initial TALON scaffold...` commit).
- Ran the "diff against current LlamaDemo source" audit flagged as open
  in the previous entry, by cloning `pytorch/executorch` and
  `meta-pytorch/executorch-examples` at their current `main` and reading
  the actual `LlmModule.kt`/`LlmCallback.kt` source (not just call sites)
  plus `examples/models/llama/README.md` and the `export_llm` config
  presets. Found real, confirmed drift and fixed it:
  - `org.pytorch.executorch.LlamaModule`/`LlamaCallback` no longer exist
    under that package — renamed to
    `org.pytorch.executorch.extension.llm.LlmModule`/`LlmCallback`.
    `MainActivity.kt` updated.
  - `LlmModule.load()` now returns `Unit` and throws
    `ExecutorchRuntimeException` on failure, instead of returning an
    Int status code. The old `if (loadResult == 0)` check was silently
    wrong (`load()` doesn't return a value to compare) — fixed to a
    try/catch.
  - `LlmModule` now implements `Closeable`; `onDestroy()` only called
    `stop()`, not `close()` — added the `close()` call.
  - `LlmCallback` gained a third method, `onError(errorCode, message)`
    (default no-op) — implemented it in `MainActivity.kt` instead of
    leaving it to the default, since silently swallowing generation
    errors would have made benchmark runs fail confusingly.
  - The 3-arg `LlmModule(path, tokenizer, temperature)` constructor and
    the 3-arg `generate(prompt, seqLen, callback)` overload TALON was
    already using are **unchanged** — confirmed directly against
    `LlmModule.kt`'s source, not inferred from a caller.
  - ExecuTorch now publishes `org.pytorch:executorch-android` to Maven
    Central (confirmed via the artifact's POM, version 1.1.0 pinned to
    match what the current reference LlamaDemo app depends on; fbjni
    and nativeloader come in transitively). This means the manual
    `sh scripts/build_android_library.sh` AAR-from-source step the
    README and `scripts/setup.sh` treated as mandatory Step 1 is no
    longer required for this project's default XNNPACK/KleidiAI
    config — `app/build.gradle.kts` now depends on the Maven artifact
    directly. `scripts/setup.sh` keeps the source-build path behind an
    opt-in `--build-aar` flag for anyone adding a non-default backend
    (QNN, Vulkan).
  - `scripts/export_model.py`'s export CLI was invoking a module path
    (`examples.models.llama.export_llama`) and flat flags
    (`-qmode`, `-group_size`, `-embedding-quantize`) that no longer
    exist. Current ExecuTorch uses `extension.llm.export.export_llm`
    with a `--config <preset.yaml>` plus dotted Hydra-style overrides.
    Rewrote the script against `examples/models/llama/config/llama_xnnpack.yaml`
    (confirmed this preset already bundles KV cache + SDPA + XNNPACK
    extended ops + 8da4w quantization — matches what TALON's flags were
    trying to express) with explicit `quantization.*` overrides layered
    on top so the "4bit-groupwise" intent stays visible in this repo
    rather than hidden inside the executorch checkout.
- Added the Gradle project scaffolding that was missing from the
  original scaffold: root `settings.gradle.kts`, root `build.gradle.kts`
  (AGP 8.6.0 / Kotlin 1.9.24, chosen for compatibility with the pinned
  Gradle version and `compileSdk 34`), `gradle.properties`, and a
  generated Gradle 8.9 wrapper (`gradlew`/`gradlew.bat`/`gradle/wrapper/`).
  Without these, `./gradlew :app:installDebug` as written in the README
  had nothing to run — there was no root project.
- Updated `README.md` (Setup steps 1-2, prerequisites) and
  `SUBMISSION_CHECKLIST.md`'s build-and-verify section to match all of
  the above.

**Decisions made this session**
- Keep the ExecuTorch AAR-from-source path in `scripts/setup.sh` as an
  opt-in flag rather than deleting it — it's still the right path for
  anyone adding a backend not in the published Maven artifact, and
  deleting it would lose real, previously-checked-in knowledge for a
  narrow simplification win.
- Pin `executorch-android` to `1.1.0` (matching the version actually
  verified against the `LlmModule`/`LlmCallback` source this session),
  not the latest Maven release (`1.4.0` as of this check) — staying
  consistent with what was actually diffed rather than assuming a newer
  version's `@Experimental` API is identical. Bumping the version is a
  "diff again first" task, not a "trust and bump" one.

**Deviations from CLAUDE.md constraints or the standing plan**
- None. Repo stays standalone (only touched files already in this repo);
  no SDLC/governance-tooling artifacts added; commits will carry the
  repo owner's configured git identity, not a tool's.

**Open questions / blockers**
- This audit was done in a sandboxed cloud session with **no Android
  SDK, no NDK, and no network access to `dl.google.com`** (Google's
  Maven/SDK-manager host — confirmed via direct 403s through the
  session's proxy; `repo1.maven.org` and `services.gradle.org` both
  work fine). Concretely, `./gradlew help` gets through settings/root
  build evaluation and the Gradle wrapper download cleanly, then fails
  exactly at resolving the Android Gradle Plugin from `google()`. That
  means **none of this session's fixes were compiled or run** — they're
  verified by reading the actual current library source
  (`LlmModule.kt`, `LlmCallback.kt`, `export_llm`'s config presets)
  line-by-line against every call site changed, not by a green build.
  A real environment with normal network access should get much
  further; run `./gradlew :app:assembleDebug` there as the first real
  compile check.
- Git tag `v0.1.0-mvp-scaffold` still needs to be created manually on
  `s-p-c-git/talon` (pointing at the `Initial TALON scaffold...` commit)
  — the automated push got a 403 specifically on tag-ref creation while
  the branch push succeeded.
- Model weights, a physical Arm64 device, and the KleidiAI-on/off
  benchmark comparison are all still unobtained/undone — unchanged from
  the previous entry, and none of them were possible in this sandbox
  either.

**Checklist state**
- Build-and-verify: added a checked-off "source-level API audit" line;
  everything requiring an actual compile, a device, or model weights is
  still open (see `SUBMISSION_CHECKLIST.md`).

**Next session should start with**
- Running `./gradlew :app:assembleDebug` in an environment with a real
  Android SDK and normal network access — this is the first actual
  compile of this session's fixes, and should surface anything the
  source-reading audit missed.

### 2026-08-10 — First real green `assembleDebug`, via CI; source-audit gap found and fixed

**What changed**
- Added `.github/workflows/build.yml`: a real `./gradlew :app:assembleDebug`
  job (JDK 17, `android-actions/setup-android`, `compileSdk 34` +
  build-tools) plus a separate job checking whether
  `pytorch/Qwen3-4B-INT8-INT4`'s pre-exported `.pte` on Hugging Face is
  reachable without auth. GitHub Actions runners have normal network
  access — this sandbox still can't reach `dl.google.com` (re-confirmed
  again this session) or `huggingface.co` at all, so CI is now the actual
  verification path for both, not a one-off workaround.
- **First CI run failed the build — for real, not from a network wall.**
  Compiler errors: `MainActivity.kt:101:21 'onError' overrides nothing`
  and `MainActivity.kt:136:19 Unresolved reference: close`. Root cause:
  the previous session's source-level audit read
  `LlmCallback.onError()`/`LlmModule`'s `Closeable`/`close()` off
  pytorch/executorch's **GitHub main branch**, then pinned the dependency
  to `1.1.0` because that's what the reference app's build file (also
  read from GitHub main) happened to use. Main-branch source and a
  specific *released* artifact are not the same thing — those two
  members were added to the library after `1.1.0` shipped. No amount of
  reading GitHub source catches that; only compiling against the actual
  binary does. This is exactly the failure mode CI was added to catch,
  and it caught it on the very first run.
- Fix: bumped `executorch-android` to `1.4.0` (latest published release,
  per Maven metadata checked last session). Second CI run:
  **`Build debug APK` succeeded, APK artifact uploaded.** First genuinely
  green, compiler-verified build this project has ever had.
- Qwen3 reachability check: passed — `pytorch/Qwen3-4B-INT8-INT4`'s
  `.pte` returns 200 unauthenticated. Confirmed as a real fallback path
  for validating the app end-to-end (build, install, load, generate) if
  Meta's Llama license isn't resolved before the deadline. `LlmModule`
  only takes file paths, so swapping models later needs zero code
  changes — just different files on-device and a re-run of the benchmark
  on whichever model actually ships.
- `v0.1.0-mvp-scaffold` git tag and an "Initial scaffold" GitHub release
  now exist on `s-p-c-git/talon`, pinned to the original scaffold commit
  — created manually (the automated tag push kept 403ing on tag-ref
  creation specifically, branch pushes were always fine). Verified via
  the GitHub API, not just taken on report.
- Updated `SUBMISSION_CHECKLIST.md`: checked off repo URL/tag, real
  `assembleDebug`, `scripts/setup.sh` verified-clean, and the two
  pre-public-push governance-artifact checks (both actually re-run this
  session, not just assumed still valid).

**Decisions made this session**
- Bump `executorch-android` to whatever the actual latest released
  version is (`1.4.0`) rather than trying to pin to some specific
  "known-good" version by reading source again — a real compile is the
  only thing that can confirm a pinned version's actual API surface, and
  CI now gives a fast, cheap way to re-check that on every push. Treat
  future version bumps the same way: bump, let CI compile it, don't
  assume from source reading alone.
- Ship the Qwen3 fallback as a *documented option*, not a silent
  replacement for Llama — the challenge's actual model requirements are
  still unconfirmed (rules page unreachable from the dev sandbox), so
  the checklist notes it as a validation/fallback path, not a decision
  to switch away from Llama.

**Deviations from CLAUDE.md constraints or the standing plan**
- **Real, twice-repeated deviation, not a "none":** this session's git
  identity in `/workspace/talon` reverted to `Claude <noreply@anthropic.com>`
  on its own between working-directory setups — not something done
  deliberately — and it happened *twice*, producing two commits with the
  wrong author before being caught. Both times: caught by explicitly
  checking `git config user.name`/`user.email` before trusting a commit,
  confirmed with the user, fixed via `commit --amend` +
  `push --force-with-lease`, and verified against `origin/main` (not just
  the local ref) afterward. Root cause of the reversion itself wasn't
  tracked down — worth treating `git remote -v` **and**
  `git config user.name`/`user.email` as a check before *every* commit
  in this repo, every session, not a one-time setup step. No governance
  file from any other source ever touched this repo.

**Open questions / blockers**
- Model weights, a physical Arm64 device, and the KleidiAI-on/off
  benchmark comparison are still unobtained/undone.
- The competition's actual rules page
  (`arm-ai-optimization-challenge.devpost.com`) is unreachable from this
  sandbox (network egress policy blocks the whole domain) — whether the
  submission needs to be Llama specifically is still unconfirmed from a
  primary source; what's known is inferred from search-engine snippets
  only.
- Root cause of the recurring git-identity reversion in
  `/workspace/talon` was never actually diagnosed — it was caught and
  fixed reactively twice, not prevented.

**Checklist state**
- `SUBMISSION_CHECKLIST.md`: repository URL/tag, real `assembleDebug`,
  `scripts/setup.sh`, and both pre-public-push governance checks moved
  to done. Model export, device install, benchmark run, and the
  KleidiAI comparison are still open — all genuinely blocked on external
  resources (weights, hardware), not on anything fixable in software.

**Next session should start with**
- If Llama's license is still unresolved: pursue the Qwen3 fallback for
  a real on-device validation (build already green; next step would be
  an emulator or physical-device install using the Qwen3 `.pte`) so the
  app's actual runtime behavior gets proven before the deadline even if
  Llama itself is still pending.
- Directly check `arm-ai-optimization-challenge.devpost.com/rules` from
  an unrestricted environment to settle the model-requirement question
  with a primary source instead of search snippets.
