# Task: DCC 文件类别治理职责拆分

## Goal

把当前 `DCC文件类别` 页从“类别 + 审核 + 下发 + 培训”的综合治理页，拆成 4 个明确页签：

- `DCC文件类别`：只保留类别主数据与基础维护
- `DCC审批路线`：承载审核入口与派生路线预览
- `DCC下发`：承载类别分发规则
- `DCC培训`：承载类别培训规则

## Scope

- 先检查同仓库上一条前端任务状态；若未完成，则先显式阻塞后再启动本任务。
- 在生产代码修改前创建任务目录、任务文档、执行日志与前端证据文件。
- 拆分 `DCC文件类别` 页，移除其中的审核、下发、培训治理内容。
- 将审批矩阵入口迁移到 `DCC审批路线` 页。
- 新增独立 `DCC下发` 与 `DCC培训` 页面。
- 完成真实前端路径回归验证。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260515-tool-header-search-always-visible/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused top-header task does not block this DCC governance split work.

## Milestones

- [x] M1: Confirm previous frontend task status and create this task directory.
- [x] M2: Record BDD scenarios and add RED evidence for the current over-coupled category page.
- [x] M3: Refactor the category page to keep only category master-data maintenance.
- [x] M4: Move the approval-matrix entry out of the category page and into the approval-routes page.
- [x] M5: Add dedicated distribution and training pages plus menu/routing integration.
- [x] M6: Run GREEN verification, update evidence, and prepare scoped frontend commit(s).

## Expected Verification

- `DCC文件类别` no longer renders approval-matrix, route, distribution, or training governance content.
- `DCC审批路线` renders the approval-matrix entry plus derived route preview.
- `DCC下发` and `DCC培训` pages open and show only their corresponding rule editors.
- Real frontend paths under `DCC文控中心` can open all 4 pages without blank states.

## Current Status

Completed. The DCC governance responsibilities are now split across 4 tabs: category master data, approval route/matrix entry, distribution rules, and training rules.

## Blocker And Impact

- Blocker: none remains for the frontend split itself.
- Impact:
  - `DCC文件类别` no longer carries approval/distribution/training governance content.
  - `DCC审批路线` now owns the approval-matrix entry plus derived route preview.
  - `DCC下发` and `DCC培训` are available as independent DCC center tabs.

## Final Verification

- Real browser verification on `http://127.0.0.1:8081`:
  - `DCC文件类别` page title: `瑛泰管理系统 - DCC文件类别`
  - `DCC文件类别` body no longer contains `审批矩阵` / `审批路线列表` / `分发部门规则` / `培训部门规则`
  - `DCC审批路线` page title: `瑛泰管理系统 - DCC审批路线`
  - `DCC审批路线` body contains `审批矩阵` entry and `派生四层预览`
  - `DCC下发` page title: `瑛泰管理系统 - DCC下发`
  - `DCC下发` body contains `分发部门规则`
  - `DCC培训` page title: `瑛泰管理系统 - DCC培训`
  - `DCC培训` body contains `培训部门规则`
- Static scope verification:
  - `categories/index.vue` now only keeps category master-data behaviors
  - `routes/index.vue` owns the matrix entry and route preview
  - `distribution/index.vue` and `training/index.vue` are isolated rule pages
