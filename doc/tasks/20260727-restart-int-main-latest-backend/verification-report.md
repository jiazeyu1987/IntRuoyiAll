# Verification Report

## Result

Runtime verification: PASS.

Task closeout: PENDING.

## Source And Artifact

- Build commit: `97ecf51a1c1f930a6c9307646614418d4ab811dc`.
- Latest verified `origin/int_main`: `177ebefbb9195835ac47d55067306454c17644da`.
- Matching `IntRuoyiBackend` tree: `7c5ffc135ce21d4905b7b46d9747dee382578c51`.
- Build and deployed Jar SHA-256: `89EB3023737BD704B92AB129C2D9176C392A6B7CE4D1E2DF2199128D02FCD98D`.

## Automated Verification

- Focused MES regression: PASS, 4 tests, 0 failures, 0 errors, 0 skipped.
- Backend package: PASS, Maven `BUILD SUCCESS`, 30 reactor modules succeeded.
- CI/CD evidence validator: PASS.

## Runtime Verification

- Old PID: `61040`, ownership confirmed before stop.
- New PID: `44372`.
- Listener: `48081`.
- Command line: E-main Jar with local profile and explicit `48081`.
- Health: `UP`.
- Protected API response: HTTP `200`, business code `401`, response time `164 ms`.
- Previous Jar backup: `E:\IntRuoyi\.runtime\int-main-backend\backups\yudao-server-exec-20260727-202427-7A3F2A015A08.jar`.

## Residual Closeout Risk

`task-closeout-cleanup` preview kept the four declared task records and selected no deletion candidates, but blocked because `E:\IntRuoyi` contains unrelated concurrent dirty files. They are outside this task's ownership and must not be committed, stashed, reverted, or deleted. Linked-worktree removal and slot `6` release remain gated until the main worktree is clean.

Task evidence commit `3a7f795fd3c17e43cc00e60e3a2ef4b283e5d396` is pushed to `origin/codex/restart-int-main-latest-backend-20260727`. The task remains `ready_for_closeout`; it is not marked completed and the worktree/slot are intentionally retained.
