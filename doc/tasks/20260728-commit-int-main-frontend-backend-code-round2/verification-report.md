# Verification Report

## Summary

PASS。当前根仓库中的前端、后端和相关任务证据已完成基线提交，并与最新 `origin/int_main` 非重写合并；合并后聚焦验证、首次推送和 cleanup 全部通过。

## Git Evidence

- Repository: `E:\IntRuoyi`
- Branch: `int_main`
- Baseline commit: `6b47dc8d chore: baseline current frontend backend changes`
- Merge commit: `8fdf586a Merge remote-tracking branch 'origin/int_main' into int_main`
- Merge base remote head: `6cadc18d`
- First push result: `HEAD` = `origin/int_main` = `8fdf586abcecd8dfe394a4babd42068729c2c507`。

## Verification

- Backend focused Maven tests -> PASS，34 tests，0 failures，0 errors，0 skipped。
- Frontend focused static contracts before merge -> PASS，11 contracts。
- Frontend focused static contracts after merge -> PASS，12 contracts。
- `pnpm ts:check` before and after merge -> PASS。
- Real E2E and task E2E script syntax checks -> PASS。
- `git diff --check` / `git diff --cached --check` -> PASS。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` frontend `8081`，backend `48081`。
- UTF-8 task document read check -> PASS。
- Secret scan -> PASS；only environment/local-config credential loading and redaction code was found, with no hardcoded secret identified.

## Merge Resolution

- Three add/add task-record conflicts retained the remote completed records and stronger clean-worktree evidence.
- `docs/experience-index.md` retained both local and remote durable routes.
- Conflict marker scan -> PASS，no unresolved markers。

## Experience Consolidation

- Added `docs/powershell-memory.md#Git-indexlock-陈旧锁恢复门禁`.
- Added the corresponding `docs/experience-index.md` route.
- No new long-term document was created.

## Cleanup

- Preview -> PASS，delete/blocked/warnings all empty.
- Apply -> PASS，deleted_paths/blocked/warnings all empty.

## Outgoing Object Scan

- First push scan -> PASS。
- Largest outgoing new blob: `229153` bytes。
- GitHub 100 MB blocker: not triggered。

## Final Status

- Implementation and merge commits pushed.
- Closeout commit `791513bc` pushed.
- Task cleanup applied.
- Task status: `completed`.
- `HEAD` and `origin/int_main` matched at `791513bc6cd8baaf813754f57a821c6975b3feed` after the closeout push.
