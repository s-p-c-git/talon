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

### 2026-08-10 — First real Qwen2.5-0.5B-Instruct `.pte` export, via 8 rounds of real CI failures

**What changed**
- Switched the pilot model to **Qwen2.5-0.5B-Instruct**. Trigger: a
  Claude-chat session's research, relayed into this session, found that
  Track 3's eligibility criteria are framework-based (ExecuTorch/ONNX
  Runtime/LiteRT/MediaPipe/etc.), not tied to a specific model family —
  Llama only appears in Arm's own "Learning paths" (resources, not
  requirements). **This session could not independently re-verify that
  claim**: both `huggingface.co` and the challenge's rules domain are
  blocked by this sandbox's network egress proxy (confirmed via direct
  `EGRESS_BLOCKED` errors from WebFetch, not just `curl` failures this
  time). Proceeded pragmatically anyway given the low reversal cost
  (`LlmModule` is file-path-only) and the deadline — but this claim is
  relayed, not verified, and should be checked from an unrestricted
  environment before final submission.
- `scripts/export_model.py` generalized from Llama-only to
  `--family {llama,qwen2_5}`, each with its own real ExecuTorch config
  preset.
- `scripts/convert_qwen_checkpoint.py` added: a vendored, self-contained
  reimplementation of `executorch/examples/models/qwen2_5/convert_weights.py`
  (attribution in `NOTICE`) that avoids a real dependency conflict (see
  bugs below).
- `.github/workflows/build.yml`'s `qwen-export-pilot` job went from "does
  it reach huggingface.co" to a real, staged, cache-backed, pinned-commit
  end-to-end export pipeline — install ExecuTorch → download checkpoint →
  convert → export/quantize → upload `.pte`.
- **First genuinely successful end-to-end Qwen2.5-0.5B-Instruct `.pte`
  export**, run `31394392855`, job `93473605751`, commit `a276286`.
  `.pte` artifact uploaded (`qwen2.5-0.5b-instruct-pte`, ~440 MB zipped).
  This took **8 rounds of real CI failures**, each diagnosed from actual
  error text/upstream source and fixed for real — not guessed, and not
  claimed fixed without a subsequent green (or further-progressed) CI
  run to prove it. In order:
  1. Shallow `git clone` without `--recurse-submodules` left
     `extension/llm/tokenizers` (a submodule `install_requirements.py`
     pip-installs directly) empty → "not installable". Fixed in both the
     CI clone step and `scripts/setup.sh` (same latent bug there).
  2. `huggingface-cli` is deprecated and now hard-exits with status 1;
     switched to `hf`.
  3. `executorch`'s own `qwen2_5/convert_weights.py` imports the full
     `torchtune` package for one helper function
     (`get_mapped_key`), but `torchtune/__init__.py` eagerly loads a
     multimodal-dataset submodule referencing
     `torchao.dtypes.nf4tensor.NF4Tensor` — absent from the torchao
     nightly ExecuTorch's own `install_requirements.py` pins. Fixed by
     vendoring just `get_mapped_key` (verified against current
     pytorch/torchtune source) and the Qwen2.5 weight-key mapping
     (verified against pytorch/executorch source) into
     `scripts/convert_qwen_checkpoint.py`, needing only
     `torch`+`safetensors`.
  4. `install_requirements.sh` only installs *dependencies* — never the
     `executorch` package itself. Its `pyproject.toml` maps
     `src/executorch/* -> *` via `package-dir`, a mapping setuptools only
     applies on an actual `pip install`. Switched to
     `install_executorch.py --minimal`, which does the same dependency
     install plus the missing `pip install .` (skipping
     torchvision/torchaudio, irrelevant for a text-only LLM export).
  5. Hydra override syntax: `export.output_name=...` tried to *override*
     an `export:` key that neither `llama_xnnpack.yaml` nor
     `qwen2_5_xnnpack_q8da4w.yaml` define — needed `+export.output_name=`
     (add) instead, same as the `base.*` overrides already used.
  6. Same class, second instance: `quantization.group_size=128` also
     needed `+` for the qwen2_5 preset specifically, since its
     `quantization:` block only defines `qmode` (llama's config already
     defines `group_size`, so that family's override correctly stays
     plain — re-audited every override key in both presets against the
     actual YAML content after this one, to rule out further instances
     before pushing again).
  7. `export_model.py` ran the export subprocess with
     `cwd=EXECUTORCH_DIR`, but `--checkpoint`/`--params`/`--output-dir`
     were passed straight through as relative paths from the *caller's*
     cwd — silently resolving wrong once the subprocess `cd`'d into
     `executorch/`. Fixed by resolving all three to absolute paths before
     building the command.
  8. (This round): everything above, run end-to-end, green.
- Added staged, content-addressed CI caching (`actions/cache`, gated
  behind `if: steps.cache-X.outputs.cache-hit != 'true'`) plus pinning
  `EXECUTORCH_REF` to a fixed commit instead of floating on `main`, so a
  future push that only touches `convert_qwen_checkpoint.py` or the
  export step doesn't re-pay the ~17-20 min from-source ExecuTorch
  install. **This is now confirmed working, not just designed**: this
  run's cache-save steps (`Post Cache ExecuTorch install`, `Post Cache
  Qwen2.5-0.5B-Instruct checkpoint`) both succeeded for the first time —
  every prior run's cache-save had silently no-op'd (`skipped`
  conclusion) because `actions/cache`'s save only fires when the whole
  job succeeds; a job failing downstream of a successful cache-populating
  step still discards that work. Worth verifying on the *next* push that
  install+download are now skipped outright.
- Updated `SUBMISSION_CHECKLIST.md`'s model-export line to reflect the
  real successful export.

**Decisions made this session**
- Pilot with Qwen2.5-0.5B-Instruct pragmatically despite the
  framework-based-eligibility claim being unverified from this
  environment — the reversal cost is low (`LlmModule` is file-path-only)
  and the deadline doesn't leave room to wait on an unrestricted
  environment. Flagged, not silently assumed.
- Treat every CI failure as the source of truth over any prior
  source-reading, consistent with this project's established practice
  (the executorch-android 1.1.0→1.4.0 lesson) — every fix in the list
  above came from reading the actual error and, where needed, the actual
  current upstream source (cloned directly, not assumed from memory),
  never from guessing based on what "should" work.

**Deviations from CLAUDE.md constraints or the standing plan**
- `CLAUDE.md` constraint 3 assumes this repo's local git identity is
  "already configured" — **this was never actually true**. This
  session's `.git/config` had no `[user]` section at all, so it silently
  fell back to this environment's global default
  (`Claude <noreply@anthropic.com>`) every time. This is the real root
  cause behind the "identity reverted" bug two prior sessions each caught
  and patched reactively (`commit --amend` + force-push) without finding
  why. This session did not touch git config (global or local) at all —
  per the standing git-safety rule against editing config — and instead
  scoped the correct identity to every single commit via
  `git -c user.name="Sadagopan Chakravarthy" -c
  user.email="s-p-c-git@users.noreply.github.com" commit ...`, verifying
  both author and committer after every commit. Whether to actually set
  local `user.name`/`user.email` for this repo (a one-time, repo-scoped,
  non-global config change) is a decision for the repo owner, not
  something this session made unilaterally.
- Separately: this session's local checkout of `/home/user/talon` had its
  `HEAD` go stale mid-session — it stayed pointed at an older commit
  while `origin/main` was correctly two commits ahead, apparently from an
  environment/session refresh unrelated to anything this session did.
  Caught by checking `git log`/`git status` before a commit and finding
  `HEAD` didn't match what had just been pushed; verified via
  `git ls-remote` directly against GitHub (authoritative, bypassing any
  local staleness) that nothing was actually lost, then fast-forwarded
  (`git merge --ff-only origin/main`) rather than anything destructive.
  Worth treating "does local HEAD match origin/main" as a check alongside
  the existing remote/identity checks, every session, not assumed stable
  within a session either.

**Open questions / blockers**
- The framework-based (not Llama-specific) Track 3 eligibility claim is
  still unverified from this environment — verify directly from
  `arm-ai-optimization-challenge.devpost.com/rules` in an unrestricted
  environment before finalizing the submission's model choice framing.
- Qwen2.5-0.5B-Instruct's exact license should be confirmed on its actual
  Hugging Face model card (this session could not reach `huggingface.co`
  to check) before the README's "Apache-2.0, per its model card" claim is
  treated as verified rather than expected.
- Model weights for Llama itself, a physical Arm64 device, and the
  baseline-vs-KleidiAI benchmark comparison are all still
  unobtained/undone.
- Whether local `user.name`/`user.email` should actually be set for this
  repo (see Deviations above) is unresolved — current workaround
  (per-commit `-c` scoping) works but requires remembering to do it every
  commit, every session.

**Checklist state**
- `SUBMISSION_CHECKLIST.md`: model export line updated to reflect the
  real, CI-verified Qwen2.5-0.5B-Instruct `.pte` export. Device install,
  benchmark run, and KleidiAI comparison remain open — genuinely blocked
  on hardware, not software.

**Next session should start with**
- Verify the caching design actually pays off: push a change that only
  touches `convert_qwen_checkpoint.py` or the export step and confirm
  `qwen-export-pilot` skips the install+download steps via cache hit
  rather than re-running them.
- Pursue on-device validation (emulator or physical device) using this
  session's real `.pte` artifact, now that a genuine export exists to
  test against.
- Directly check `arm-ai-optimization-challenge.devpost.com/rules` and
  the Qwen2.5-0.5B-Instruct Hugging Face model card from an unrestricted
  environment — both are still unverified claims this session had to
  proceed on pragmatically rather than confirm.
