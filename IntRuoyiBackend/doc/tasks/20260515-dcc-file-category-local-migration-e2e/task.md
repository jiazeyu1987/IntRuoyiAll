# Task: DCC 文件类别本地化迁移并完成真实 E2E 验证

## Goal

将 IntAuth 当前文件类别主数据一次性迁移到本地当前运行租户可见的 `dcc_file_category`，纠正 live 本地库中“48 条导入类别落在 `tenant_id=0`、当前租户列表为空”的问题，并验证 `GET /dcc/file-categories` 与真实前端 `DCC文件类别` 页面都能稳定展示这批本地文件类别。

## Scope

- 先检查上一条后端任务文档状态。
- 在 live 数据变更前创建本任务文档、执行日志和证据文件。
- 复用现有 `POST /dcc/file-categories/import-intauth` 一次性导入能力，不设计旁路写入。
- 核对 live 本地 MySQL `127.0.0.1:23306/ruoyi-vue-pro` 中 `dcc_file_category` 的租户分布。
- 确保 live 后端 `48081` 已加载文件类别导入实现并带上可访问 IntAuth 的内部令牌。
- 通过 live 导入把当前 IntAuth 文件类别落到当前运行租户，并验证 `GET /dcc/file-categories` 不再返回空数组。
- 记录真实前端 `http://127.0.0.1:8081/dcc/controlled-file/categories` 的通过证据。

## Previous Task Check

- Previous backend task: `doc/tasks/20260515-dcc-file-category-import-from-intauth/task.md`
- Status before this task: completed.
- Impact: the backend import capability already exists, so this task focuses on live tenant-visible migration and verification rather than new API delivery.

## Milestones

- [x] M1: Confirm previous backend task status and create this task directory before live changes.
- [x] M2: Record BDD scenarios and RED evidence for the live tenant-visibility gap.
- [x] M3: Verify live runtime prerequisites and migrate file categories into the current tenant view.
- [x] M4: Recheck local database tenant distribution and API output after migration.
- [x] M5: Confirm the real frontend list shows file-category rows and update final evidence.

## Expected Verification

- Read-only precheck confirms local `dcc_file_category` has 48 active rows in `tenant_id=0` and the current live API returns an empty list for the tenant-1 admin token.
- `POST /dcc/file-categories/import-intauth` completes against the live backend and creates/adopts tenant-visible local file categories.
- Post-migration `GET /dcc/file-categories` returns non-empty data for the admin tenant token.
- Real frontend route `/dcc/controlled-file/categories` visibly shows file-category rows.

## Current Status

Completed. The live tenant-visibility gap is resolved: the current admin tenant now has 48 active local file categories, `GET /dcc/file-categories` no longer returns an empty list, and the real frontend `DCC文件类别` page visibly renders imported rows.

## Blocker And Impact

- Blocker: none remains for the live migration itself.
- Impact:
  - Before migration, local DCC file categories existed only in `tenant_id=0`, so the current tenant-1 admin API view was empty.
  - After migration, `tenant_id=1` now contains 48 active `INTAUTH:*` rows and the live frontend can display them.

## Final Verification Result

- Read-only precheck:
  - `dcc_file_category` active rows by tenant: `tenant_id=0 -> 48`, `tenant_id=1 -> 0`
  - live `GET /dcc/file-categories` with the current admin token -> `data=[]`
- Live migration:
  - `POST /dcc/file-categories/import-intauth` with the current admin token -> `totalCount=48`, `createdCount=48`, `adoptedCount=0`, `updatedCount=0`
- Read-only postcheck:
  - `dcc_file_category` active rows by tenant: `tenant_id=0 -> 48`, `tenant_id=1 -> 48`
  - live `GET /dcc/file-categories` with the current admin token -> non-empty `data` array containing `INTAUTH-1` through `INTAUTH-48`
- Real frontend confirmation:
  - `http://127.0.0.1:8081/dcc/controlled-file/categories` shows visible rows beginning with `INTAUTH-1 / 产品技术要求`
