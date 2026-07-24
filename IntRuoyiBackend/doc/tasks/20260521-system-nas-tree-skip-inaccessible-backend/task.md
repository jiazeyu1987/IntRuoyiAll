# 任务：NAS 目录树跳过无权限目录（后端）

## Goal

修复当前 `NAS管理 -> 刷新目录` 在真实 NAS 上因为根目录下存在无权限目录（如 `#recycle`）而整次失败的问题，改为：

- 跳过无权限目录
- 继续返回其余可访问目录树
- 显式返回被跳过的目录路径与原因，供前端展示

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\main\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\controller\admin\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-infra\src\test\java\cn\iocoder\yudao\module\infra\service\file\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-tree-skip-inaccessible-backend\**`

## Non-Scope

- 不更改 NAS 参数保存逻辑。
- 不更改普通 `nas-files` 列表接口的 fail-fast 行为。
- 不隐藏跳过结果。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-system-nas-directory-tree-backend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 目录树接口已存在；本任务只修复真实 NAS 环境下的无权限目录回归。

## Milestones

- [x] M1: 创建任务文档并记录真实失败症状。
- [ ] M2: 补 RED 测试，锁定“跳过无权限目录仍返回可访问树”的契约。
- [ ] M3: 做最小后端修复并返回 skipped 结果。
- [ ] M4: 跑定向测试与证据校验，记录 GREEN。
- [ ] M5: 运行 closeout preview 并完成收尾。

## Expected Verification

- `mvn -pl yudao-module-infra "-Dtest=NasBrowserServiceImplTest,FileControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260521-system-nas-tree-skip-inaccessible-backend/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-system-nas-tree-skip-inaccessible-backend --mode preview`

## Current Status

Completed with blockers on 2026-05-21. 后端已实现“跳过无权限目录并返回 skipped 列表”，定向测试已通过；但真实整棵共享目录递归在本地环境下仍会长时间运行，存在接口超时风险。

## Blockers And Impact

- Blocker:
  - 真实 `GET /admin-api/infra/file/nas-tree` 在当前本地 NAS 共享上不再因 `#recycle` 立即失败，但整棵共享递归扫描超过 `180s` 仍未完成。
- Impact:
  - 无权限目录问题已被修复为 skipped 语义。
  - 但页面如果继续按“一次性全量递归树”调用该接口，仍可能因共享体量过大而超时。
