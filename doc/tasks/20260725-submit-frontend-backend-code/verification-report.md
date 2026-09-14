# Verification Report

## Scope

- 本报告覆盖用户要求的“提交前后端代码”任务。
- 前后端目录共享根仓库 `D:\ProjectPackage\IntRuoyi\IntRuoyiAll`，当前分支 `int_main`。

## Results

- `git diff --check`：PASS。
- `scripts\preflight\branch-runtime-port-guard.ps1`：PASS，`int_main/int_main_d` 前端 `8101`，后端 `48101`。
- GitHub 推送前历史对象扫描：PASS，最大 blob `4,177,309` bytes，未超过 100 MB。
- `git fetch origin int_main`：PASS，获取远端新提交 `223482e3`。
- `git rebase origin/int_main`：PASS，无冲突。
- `git push origin int_main`：PASS，远端 `origin/int_main` 更新到 `e12e865c`。
- `git status --short --branch`：PASS，显示 `## int_main...origin/int_main`，无 ahead/behind。
- `task_closeout.py --task-id 20260725-submit-frontend-backend-code --mode preview`：PASS，delete/blocked/warnings 均为 `<none>`。
- `task_closeout.py --task-id 20260725-submit-frontend-backend-code --mode apply`：PASS，deleted_paths 为 `<none>`。

## Submitted Commits

- `e30c30a4 chore: baseline d main runtime port contract`
- `1893af79 docs: baseline d main runtime startup task`
- `6c2f99c2 docs: record frontend backend submit task`
- `5525e763 docs: update d main runtime verification`
- `e12e865c fix: restore d main runtime source packages`

## Remaining Non-Task State

- `doc/tasks/20260725-dcc-controlled-file-logs-import/` remains untracked and belongs to another in-progress DCC fix task; it was not submitted by this task.