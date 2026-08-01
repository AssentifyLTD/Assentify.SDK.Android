## Build check-and-fix workflow

This workflow runs fully autonomously. Do not pause to report a failure or ask for
permission mid-loop — only communicate with the user once the workflow reaches a
terminal state (success, or the 5-attempt cap reached for a variant). A failing
build on attempt 1-4 is expected and normal; it is not a reason to stop and report.

Trigger phrases and what they mean:
- **"solomon debug"** → run the Debug workflow only.
- **"solomon release"** → run the Release workflow only.
- **"solomon build"** / **"solomon check the build"** (no variant specified) → run both,
  Debug first, then Release.
- **"solomon lib"** → run the Lib Conversion task only (see below), then run the Debug
  workflow to confirm the module still builds as a library.

**Lib Conversion (one-time gradle edit, not a build-and-fix loop):**
1. Open `build.gradle`.
2. Replace the line:
   `apply plugin: 'com.android.application'`
   with:
   `apply plugin: 'com.android.library'`
3. Find the line:
   `applicationId "com.assentify.sdk"`
   and comment it out (don't delete it), e.g.:
   `// applicationId "com.assentify.sdk"`
4. Save the file. Do not touch any other lines in `build.gradle` as part of this task —
   only these two edits.
5. After making both edits, proceed straight into the Debug workflow below (no
   confirmation needed) to verify the module still assembles correctly as a library.
   If Debug fails, treat it as attempt 1 of the normal 5-attempt Debug loop and fix
   root causes as usual — but do not revert or second-guess the two gradle edits above
   unless the error output specifically implicates one of those exact lines.

**Debug:**
1. Run `./gradlew assembleDebug`.
2. If it fails, read the actual compiler/lint error output (don't guess) and fix the
   root cause in the source, then immediately re-run — no confirmation needed.
3. Repeat until it succeeds or you hit 5 attempts.

**Release:**
1. Run `./gradlew assembleRelease`.
2. If it fails, read the actual error output and fix the root cause (R8/ProGuard
   issues, missing `-keep` rules, resource shrinking, signing config). Then
   immediately re-run — no confirmation needed.
3. Repeat until it succeeds or you hit 5 attempts.

Rules for this loop:
- Cap at 5 fix attempts per variant. If still failing after 5, STOP and report: what
  you tried each attempt, the current error, and your best hypothesis.
- Only change what the error actually points to — no unrelated refactors or cleanup.
- `build.gradle` / `proguard-rules.pro` are allowed to be edited mid-loop **if and only
  if** the error output specifically names a rule or config in that file as the cause.
  This is not an ask-first case — make the targeted edit, note it in the final report,
  and keep looping. Only stop early if the fix would require a change unrelated to
  what the error implicates (i.e. you're guessing, not diagnosing).
- Never modify, regenerate, or print `release-key.jks` or any signing credentials —
  this is the one hard stop that overrides "keep looping." If a failure traces to
  signing config/keystore itself, stop immediately and report — don't attempt a fix.
- If a fix for release would change public API behavior or break something debug
  relies on: still apply the minimal fix needed to get the build green (that's the
  point of this workflow), but flag it prominently in the final report as something
  to review — don't stop the loop to ask about it in the moment.

At the end, report clearly: which variant(s) succeeded, how many fix attempts each
took, and a short list of what was actually changed (file + one-line reason per fix).