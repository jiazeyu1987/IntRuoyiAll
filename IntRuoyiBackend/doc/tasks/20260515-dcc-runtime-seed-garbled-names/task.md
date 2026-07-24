# Task: DCC runtime 种子中文名称乱码修复

## Goal

修复 DCC runtime / E2E 种子目录与类别在真实后端接口中返回问号名称的问题，确保 `DCC 目录管理` 页面及相关治理页面显示规范简体中文而不是 `?` 占位符。

## Scope

- 先检查同仓库上一条后端任务状态；若未完成，则显式阻塞后再启动本任务。
- 在生产代码或数据脚本修改前创建本任务文档和执行日志。
- 用真实运行中的 `/admin-api/dcc/directories/tree` 与相关本地数据库记录复现乱码。
- 仅修复当前 runtime / E2E 种子中已写坏的 DCC 目录和类别中文名称，以及对应的可追踪脚本化修复。
- 不引入运行时 fallback、静默替换或前端侧掩盖逻辑。
- 若无法确认实际数据库连接、写入路径或受影响记录，必须记录阻塞并停止。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-kingdee-material-sync-page-until-empty/task.md`
- Status before this task: blocked by user priority switch
- Impact: the paused ERP pagination task does not block this DCC runtime seed garbled-name repair.

## Milestones

- [x] M1: Check the previous backend task state and block it explicitly.
- [x] M2: Create this task document and execution log before production changes.
- [ ] M3: Reproduce the garbled-name symptom with RED evidence against the real backend/API data.
- [ ] M4: Add the minimal persistent fix for the broken DCC runtime seed names and apply it to the running database.
- [ ] M5: Run GREEN verification, update evidence, and prepare a scoped backend commit.

## Expected Verification

- `GET /admin-api/dcc/directories/tree` no longer returns `?`-filled names for `DCC_RUNTIME_ROOT` and `DCC_RUNTIME_PDF`.
- The related runtime category seed no longer returns a garbled `name`.
- Task evidence records exact BDD / RED / GREEN commands and results.
- The fix is represented in tracked backend files and also applied to the running local database used by the current backend process.

## Current Status

Completed. The runtime seed rows were repaired in the live local MySQL used by the running backend, and a tracked SQL fix plus regression check script were added to the backend repo so the same question-mark names can be detected and corrected repeatably.

## Blocker And Impact

- Blocker: none currently.
- Impact:
  - `DCC_RUNTIME_ROOT` no longer renders as `DCC???????`.
  - `DCC_RUNTIME_PDF` no longer renders as `??PDF??`.
  - `DCC_RUNTIME_CATEGORY` no longer renders as `????????`.

## Root Cause Summary

- The real backend API `GET /admin-api/dcc/directories/tree` was returning question-mark names directly from MySQL, so the DCC directory page rendered garbled data rows even though the Vue page copy itself was correct.
- The affected rows were the runtime / E2E seed family in `dcc_file_directory` and `dcc_file_category`.
- The table charset and connection charset are both `utf8mb4`, so the corruption did not happen during read or JSON serialization; the Chinese labels had already been persisted as literal `?` placeholders when the seed data was written.
- Because the original Chinese labels were irreversibly lost in the database rows, the fix restores readable names based on the stable code semantics of `DCC_RUNTIME_ROOT`, `DCC_RUNTIME_PDF`, and `DCC_RUNTIME_CATEGORY`.

## Final Verification Result

- `python doc\\tasks\\20260515-dcc-runtime-seed-garbled-names\\scripts\\check_dcc_runtime_seed_names.py` -> PASS
- Read-only DB check after applying `sql/mysql/20260515_dcc_runtime_seed_name_fix.sql`:
  - `DCC_RUNTIME_ROOT` -> `DCC运行时根目录`
  - `DCC_RUNTIME_PDF` -> `运行时PDF目录`
  - `DCC_RUNTIME_CATEGORY` -> `运行时文件类别`
- Real page verification through the running frontend at `http://127.0.0.1:8081/dcc/controlled-file/directories` -> PASS, the directory table now shows readable Chinese names instead of question marks.
