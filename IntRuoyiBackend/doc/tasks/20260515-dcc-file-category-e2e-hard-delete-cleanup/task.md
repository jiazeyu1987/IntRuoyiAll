# Task: DCC 文件类别 E2E 历史链物理清理

## Goal

物理清理 live 本地库中旧 `E2E` DCC 文件类别历史链，移除 `DCC_RUNTIME_CATEGORY / 运行时文件类别` 及其关联的测试目录、访问规则、审批路线、受控文件、路线快照和盖章记录，同时保持当前租户下新迁入的 48 条正式 IntAuth 文件类别可继续正常展示。

## Scope

- 先检查上一条后端任务文档状态。
- 在执行 live 物理删除前创建本任务文档和执行日志。
- 只清理明确属于旧 `E2E` 文件类别链路的数据：
  - category `900201`
  - directory `900002`
  - route `900401`
  - 关联 `dcc_controlled_file*`、route snapshot、stamp、directory access rule 等测试残留
- 删除前先做全量引用扫描，确认待删对象范围。
- 在事务中按依赖顺序执行删除。
- 删除后验证：
  - `GET /dcc/file-categories` 仍返回 48 条正式类别
  - 真实前端 `DCC文件类别` 列表不再出现 `DCC_RUNTIME_CATEGORY`

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-file-category-local-migration-e2e/task.md`
- Status before this task: completed.
- Impact: the live tenant-visible migration is done, so this task only removes the obsolete E2E history chain.

## Milestones

- [x] M1: Confirm previous backend task status and create this task directory before live cleanup.
- [x] M2: Record BDD scenarios and pre-delete evidence for the E2E history chain.
- [x] M3: Enumerate all live references to the E2E category chain.
- [x] M4: Delete the E2E chain in one transaction.
- [x] M5: Verify API and real frontend no longer show the E2E row while the 48 formal categories remain.

## Expected Verification

- Read-only precheck identifies the exact live references belonging to the old `E2E` file-category chain.
- Post-delete MySQL checks show no remaining references to category `900201`, directory `900002`, or route `900401`.
- `GET /dcc/file-categories` still returns 48 current-tenant formal categories.
- Real frontend `DCC文件类别` page shows only the formal imported categories and no `DCC_RUNTIME_CATEGORY`.

## Current Status

Completed. The old `E2E` file-category history chain has been physically removed from the live database. The current tenant still retains 48 active formal categories, and the obsolete `DCC_RUNTIME_CATEGORY` row no longer exists.

## Blocker And Impact

- Blocker: none remains for this cleanup.
- Impact:
  - The old E2E category/directory/route/test-file history chain no longer pollutes live DCC data.
  - The current tenant keeps the 48 formal `INTAUTH:*` categories created by the earlier live migration.

## Final Verification

- Read-only pre-delete evidence:
  - one E2E category row `900201`
  - one E2E directory row `900002`
  - one E2E route row `900401`
  - 6 controlled-file rows
  - 6 route-snapshot rows
  - 4 stamp rows
  - 4 directory-access-rule rows
- Transaction cleanup result:
  - `dcc_controlled_file_stamp` deleted `4`
  - `dcc_controlled_file_route_snapshot` deleted `6`
  - `dcc_controlled_file` deleted `6`
  - `dcc_controlled_file_master` deleted `6`
  - `dcc_category_approval_route_node` deleted `1`
  - `dcc_category_approval_route` deleted `1`
  - `dcc_directory_access_rule` deleted `4`
  - `dcc_category_directory_binding` deleted `1`
  - `dcc_file_directory` deleted `1`
  - `dcc_file_category` deleted `1`
- Post-delete checks:
  - `tenant1_not_deleted=48`
  - `runtime_category_rows=0`
