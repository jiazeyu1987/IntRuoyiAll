# Task: DCC 分类治理页签菜单拆分

## Goal

为前端治理职责拆分补齐后端菜单种子，使 `DCC文控中心` 下新增 `DCC下发` 与 `DCC培训` 页签，并让本地/后续环境能正确映射到新增前端页面组件。

## Scope

- 先检查同仓库上一条后端任务状态；若未完成，则先显式阻塞后再启动本任务。
- 在修改生产文件前创建本任务文档与执行日志。
- 仅调整 DCC 菜单种子，不改后端业务接口与数据模型。
- 为 `DCC下发` / `DCC培训` 新增菜单记录并调整排序。
- 保持现有 `DCC文件类别` / `DCC审批路线` 菜单路径和组件映射可继续使用。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-category-matrix-derived-route/task.md`
- Status before this task: blocked by user priority switch.
- Impact: the paused matrix-derived-route backend task does not block this narrower menu-seed change.

## Milestones

- [x] M1: Confirm previous backend task status and create this task directory.
- [x] M2: Record BDD scenarios and RED evidence for the missing DCC distribution/training tabs.
- [x] M3: Update DCC menu seed SQL for the split governance tabs.
- [x] M4: Verify menu seed definitions are internally consistent and ready for runtime use.

## Expected Verification

- DCC menu seed SQL contains entries for:
  - `DCC文件类别`
  - `DCC审批路线`
  - `DCC下发`
  - `DCC培训`
- New menu paths and component mappings align with the new frontend pages.

## Current Status

Completed. The backend seed now includes dedicated DCC distribution/training menu definitions, and the same menu updates were applied to the local runtime database so the real frontend sidebar can render both new tabs.

## Blocker And Impact

- Blocker: none remains for the menu seed split.
- Impact:
  - local and fresh environments now have an explicit SQL patch for `DCC下发` / `DCC培训`
  - local runtime sidebar order is adjusted so the new tabs sit between `DCC审批路线` and `DCC受控上传`

## Final Verification

- Repo verification:
  - `sql/mysql/20260515_dcc_governance_split_menu.sql` exists and adds `controlled-file/distribution` plus `controlled-file/training`
- Local runtime verification:
  - `system_menu` rows `6808` / `6809` exist
  - sort order updated for sibling DCC menu items
  - sidebar under `DCC文控中心` displays `DCC下发` and `DCC培训`
