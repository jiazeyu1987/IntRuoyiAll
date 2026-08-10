# Verification Report

## Result

The active-order PQC process lookup now follows the frozen process snapshot. Active order `39` no longer evaluates current route process `980632/922986`, so its missing QA regulation cannot trigger this order's PQC context error.

## Code And Test Verification

- Regression test: `MesFrontlinePqcContextServiceTest#shouldLoadOnlyProcessesFrozenInActiveOrderSnapshot`.
- Targeted regression: PASS, 1 test.
- Full `MesFrontlinePqcContextServiceTest`: PASS, 30 tests.
- Direct `javac` compilation of `MesFrontlinePqcContextServiceImpl.java`: PASS.
- The standard full reactor compile was not used as the final gate after a concurrent MES Maven process began writing the same workspace output and the task compile stalled in Windows file output. No Java compile error was emitted.

## Database Evidence

- Active order `39`: work order `980026`, route `980091`, published route version `622`, product `924008`.
- Frozen active-order process snapshot: only `980631/922985`.
- Pending PQC tasks: `211`-`214`, bound to `980631/922985`, published regulation version `36`.
- Current route also contains `980632/922986`, but that pair is not frozen into active order `39` and has no published QA regulation.

## Runtime Verification

- Old runtime: PID `6360`, Jar `backend-latest-20260807-2215-pqc-missing-task-active-order-45.jar`, health `UP`.
- New runtime: PID `68664`, Jar `backend-latest-20260807-2338-pqc-active-order-snapshot.jar`, port `48081`, health `UP`.
- New Jar SHA256: `377106779A4C0ADA83276A1C698563A956BECE3E63362E47540D2135E80C94D0`.
- Outer `BOOT-INF/lib/yudao-module-mes-2026.04-SNAPSHOT.jar` entry is unique and stored with `compress_type=0`.
- The nested MES Jar contains `MesFrontlinePqcContextServiceImpl.class` plus its five companion classes.

## Scope And Blockers

- No fallback regulation, default route, empty-success response, or swallowed exception was introduced.
- Authenticated UI/API verification was not run because no task-owned logged-in tenant/account session was available. Unauthenticated `401` would not prove the business path.
- Bug regression evidence validation passed before cleanup.
- Task cleanup preview/apply passed with no blocked paths or warnings; only the three core task records remain.
- Final runtime recheck after cleanup: PID `68664`, port `48081`, health `UP`.
