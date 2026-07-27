# Verification Report

## Summary

- 结果：本地前后端代码已拆分为 3 个基线提交；cleanup preview/apply 已通过，本任务收尾记录待提交并推送。
- 范围：`E:\IntRuoyi` 根仓库，当前分支 `int_main`，远端 `origin`。

## Commands

- `git branch --show-current` -> PASS，当前分支 `int_main`。
- `git remote -v` -> PASS，存在 `origin` fetch/push remote。
- `git diff --check` -> PASS，无 whitespace error。
- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main` 端口为前端 `8081`、后端 `48081`。
- 大文件扫描 -> PASS，未发现超过 100MB 的待提交文件。
- 敏感关键词扫描 -> REVIEW 后 PASS，未发现阻塞提交的真实凭据。
- `task_closeout.py --task-id 20260727-commit-frontend-backend-code --mode preview` -> PASS，无 delete/blocked/warnings。
- `task_closeout.py --task-id 20260727-commit-frontend-backend-code --mode apply` -> PASS，无 deleted_paths。

## Commits

- `3f3e22465e5086db113142fa77e68f3892e00627`：`chore: baseline frontend backend updates`。
- `38bfcc90`：`chore: baseline remaining codex runner update`。
- `d083d962`：`chore: baseline remaining batch record report updates`。

## Open Items

- `doc/tasks/20260727-schedule-calendar-cross-month-data/` 为并行出现的未跟踪任务目录，本任务未触碰。
- 待完成本任务收尾提交和 `git push origin int_main`。
