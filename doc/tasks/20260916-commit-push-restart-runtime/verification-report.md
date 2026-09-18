# Verification Report

Status: blocked

## Verification Evidence

- PASS: `scripts\preflight\branch-runtime-port-guard.ps1` -> Branch runtime port guard passed for `int_main/int_main`, frontend 8081, backend 48081.
- PASS: `mvn.cmd -pl yudao-module-erp,yudao-module-mes -am '-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderCreateServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`.
- PASS: ERP test class `ErpKingdeeProductionOrderClientImplTest` -> 13 tests, 0 failures, 0 errors.
- PASS: MES test class `MesKingdeeProductionOrderCreateServiceImplTest` -> 10 tests, 0 failures, 0 errors.
- PASS: `git diff --check`.
- PASS: Code baseline commit created -> `15d72252e`.
- PASS: task-closeout-cleanup preview/apply -> no delete, no blocked, no warnings.
- PASS: standard local full restart -> backend package BUILD SUCCESS and restart dispatched for `int_main` on 8081/48081.
- PASS: backend health -> `UP`; listener PID 37360 on 48081; `Started YudaoServerApplication` recorded at 2026-09-16 08:39:27.
- PASS: frontend HTTP -> 200; listener PID 42512 on 8081; Vite ready at 2026-09-16 08:39.

## Blocker

- FAIL: `git push origin int_main` -> `TLS connect error: error:0A000126:SSL routines::unexpected eof while reading`.
- FAIL: `git ls-remote --heads origin int_main` -> same GitHub HTTPS TLS connect error.
- Impact: local branch has the code commit but `origin/int_main` is not updated, so the task cannot be marked completed under project closeout rules.
