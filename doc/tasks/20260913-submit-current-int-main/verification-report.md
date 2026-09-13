# Verification Report

## Current Result

IN_PROGRESS. 等待提交前静态检查、commit 和 push 结果。

## Evidence

- `git branch --show-current` -> `int_main`。
- `git rev-parse --short HEAD` -> `6c6487c9f`。
- `git rev-parse --short origin/int_main` -> `6c6487c9f`。
- `git diff --check` -> PASS，仅 LF/CRLF warning。

## Excluded Local Files

- `resource/`
- `IntRuoyiFronted/tsconfig.route-production-migration.tmp.json`
