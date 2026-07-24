# Task: DCC 类别审批路线恢复

## Goal

把当前 `产品技术要求` 类别在 live MySQL 中被 E2E 改写过的审批路线恢复到改动前的稳定版本，并确认前端读取的 active 路线重新回到该版本，而不是继续使用后续 E2E 生成的路线版本。

## Scope

- 检查当前后端仓最近任务状态后再开始本次恢复。
- 先创建当前任务文档和执行日志，再进行 live 数据修复。
- 仅恢复 `dcc_category_approval_route` / `dcc_category_approval_route_node` 的 active 版本选择。
- 不切换数据库，不回滚文件类别、目录、岗位、权限或文件数据。
- 恢复后验证当前 active 路线节点内容与目标版本一致。

## Previous Task Check

- Previous backend task:
  `doc/tasks/20260516-dcc-upload-name-version-linkage/task.md`
- Status before this task: completed.
- Impact: the latest backend task is already closed and does not block this live route-restore action.

## Milestones

- [x] M1: Create task package and inspect current route history.
- [x] M2: Identify the pre-E2E stable route version to restore.
- [x] M3: Switch the active route back to that version in the live MySQL runtime.
- [x] M4: Verify the active route content after restore and update evidence.

## Expected Verification

- Read-only route history query for `category_id = 1`
- Live active route after restore matches the chosen historical version

## Current Status

Completed. The live route for category `产品技术要求` has been switched back to the earlier stable version.

## Blocker And Impact

- Blocker: none.
- Impact: the active route no longer points at the later multi-account/four-approver E2E variants.

## Restore Target

- Restored route id: `28`
- Restored route version: `2`
- Restored remark: `codex e2e runtime setup`

## Final Verification Result

- Database remained the same runtime target:
  - schema: `ruoyi-vue-pro`
  - host/port: `127.0.0.1:23306`
- Active route after restore:
  - `id=28`
  - `version_no=2`
  - `active=1`
- Active route nodes after restore:
  - stage 1 `文控审核` -> candidate ids `31`
  - stage 2 `审核会签` -> candidate ids `1,2,4,5,31`
  - stage 3 `批准` -> candidate ids `900333,900334`
  - stage 4 `文控批准` -> candidate ids `31`
- Live route preview API now reads the restored route content again for `categoryId=1`.
