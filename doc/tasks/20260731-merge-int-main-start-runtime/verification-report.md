# Verification Report

## Summary

- Merge status: local merge commit `2c277c09f5c00fb33ee6a5181c4ce738fbdee252` created on `int_batch`.
- Port guard: PASS, `int_batch` kept frontend `8041` and backend `48041`.
- Runtime dependency precheck: PASS, `8041/48041` free; Docker MySQL `23306` and Redis `26379` listening.
- Backend build: BPM module passes with the explicit Java 17 toolchain; the first full package exposed and led to repair of an incomplete merge tree.
- Runtime startup: PASS, backend `48041` returned `UP` and frontend `8041` returned HTTP `200`.

## Evidence

- `git fetch origin int_main` -> PASS, `origin/int_main` at `e9eca0b3`.
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS, reported `Branch runtime port guard passed for int_batch/int_batch: frontend 8041, backend 48041.`
- `mvn.cmd -pl yudao-server -am -Dmaven.test.skip=true package` -> FAIL in `yudao-module-bpm`.
- `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile` -> FAIL with the same missing Lombok-generated methods/builders.
- `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` -> BLOCKED/TIMEOUT; stopped task-owned Maven process.
- `C:\Users\BJB110\.jdks\jdk-17.0.20+8\bin\java.exe -version` -> PASS, Microsoft OpenJDK 17.0.20.
- `mvn.cmd -pl yudao-module-bpm -DskipTests -Dmaven.compiler.proc=full -Dmaven.compiler.useIncrementalCompilation=false clean compile` with `JAVA_HOME=C:\Users\BJB110\.jdks\jdk-17.0.20+8` -> PASS, 382 source files compiled and `BUILD SUCCESS`.
- Full Java 17 package -> FAIL in `yudao-module-dcc`; `NasRecursiveScanHandler`, `NasRecursiveScanService`, `NasRecursiveScannedFile`, and `NasRecursiveSkippedDirectory` were absent from the merge tree.
- Normal three-way merge recomputation -> PASS, only `docs/e2e-rules.md` and `docs/experience-index.md` conflicted as expected.
- Merge-tree comparison -> original merge omitted 1922 additions and 483 modifications; resolved tree `d4fa4b4d9e5ed6e2d87a1e84d5a56062093fdd65` restores all 2405 paths.
- Pre-existing ignored source preservation -> PASS, 4 differing ERP runtime files were retained by exact blob ID rather than overwritten.
- Full Java 17 package after merge-tree restoration -> PASS, all 30 reactor modules succeeded and `yudao-server-exec.jar` was generated with size `501172025` bytes.
- Branch runtime script regression -> PASS, `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_branch_runtime_profile.py -q` reported `12 passed`.
- Backend listener -> PASS, final PID `52848`, Java executable `C:\Users\BJB110\.jdks\jdk-17.0.20+8\bin\java.exe`, runtime Jar under `output\runtime\int_batch`, target Jar not referenced by the running command.
- Backend health -> PASS, `http://127.0.0.1:48041/actuator/health` returned `{"status":"UP"}`.
- Backend startup marker -> PASS, stable runtime log contains `项目启动成功！` under the Windows runtime encoding.
- Frontend listener -> PASS, final PID `40340`, command line belongs to `E:\IntRuoyiBranch\BatchRecord\IntRuoyiAll\IntRuoyiFronted` and includes port `8041`.
- Frontend entry -> PASS, `http://127.0.0.1:8041/` returned HTTP `200`.
- Runtime Jar immutability -> PASS, a second Java 17 Maven package finished at `2026-07-31 18:44:51 +08:00`; backend stayed healthy and continued using the independent runtime Jar created before the rebuilt target Jar.
- Branch runtime port guard -> PASS after final restart, `int_batch` remained on frontend `8041` and backend `48041`.
- Implementation commit -> PASS, `179de5e0`.
- Cleanup preview/apply -> PASS, only the task-owned `.runtime\20260731-merge-int-main-start-runtime` directory was deleted; all four required task records were kept.
- Temporary recomputation worktree -> PASS, `D:\IntRuoyiWorktree\recompute-int-main-merge-20260731` was removed after its merge-tree evidence was committed; Git registration and physical path are both absent.

## Resolved Blocker Detail

- Root cause 1: the first compile attempts used the only pre-existing local JDK 21 even though the repository baseline requires Java 17. The representative Lombok errors disappeared under Java 17 without source changes.
- Root cause 2: merge commit `2c277c09` recorded both parents but did not contain the normal three-way merge result, leaving callers without their newly added dependencies.
- All required packaging and runtime verification has completed.

## Residual Observation

- At `2026-07-31 18:45:00 +08:00`, the running backend logged a non-fatal DCC scheduled temporary-file cleanup error for tenant `122` because a referenced file was already absent.
- This did not change backend health or frontend availability. The task did not add a fallback, repair unrelated data, or expand scope into DCC cleanup behavior.
