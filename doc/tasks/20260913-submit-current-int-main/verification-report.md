# Verification Report

## Current Result

READY_FOR_CLOSEOUT. 当前代码批次已提交并推送到 `origin/int_main`，等待 cleanup preview/apply 和最终收尾提交。

## Evidence

- `git branch --show-current` -> `int_main`。
- `git rev-parse --short HEAD` -> `6c6487c9f`。
- `git rev-parse --short origin/int_main` -> `6c6487c9f`。
- `git diff --check` -> PASS，仅 LF/CRLF warning。
- `git commit -m "chore: submit current int_main changes"` -> PASS，最终经 rebase 后 commit 为 `ccf7fe08c`。
- `git push origin int_main` -> PASS，`5f22cf5f7..ccf7fe08c int_main -> int_main`。
- `git status --short --branch` after push -> local `int_main` equals `origin/int_main`; only excluded untracked files remained plus two later-arriving tracked MES edits to be included in closeout commit.

## Excluded Local Files

- `resource/`
- `IntRuoyiFronted/tsconfig.route-production-migration.tmp.json`
