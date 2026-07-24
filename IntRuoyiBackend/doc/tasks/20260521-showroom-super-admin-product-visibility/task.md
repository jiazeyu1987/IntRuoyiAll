# 任务：修复超级管理员看不到展厅产品列表

## 目标

修复当前 `super_admin` 用户打开展厅 `产品管理` 页仍返回 `0` 条的问题，使超级管理员即使不具备 `showroom_publicity` 角色，也能查看展厅产品列表。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-super-admin-product-visibility\**`

## 非范围

- 不调整前端页面布局。
- 不修改测试服务器数据。
- 不改动指派业务本身，只修正管理员可见性判断。

## 上一任务检查

- 上一相关后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-hall-product-restore\task.md`
- 状态：`Completed`
- 影响：hall-product 映射已恢复，当前剩余问题已定位为管理员可见性过滤。

## 里程碑

- [x] M1：创建任务文档并确认上一任务状态。
- [x] M2：用测试复现 `super_admin` 产品页为空。
- [x] M3：做最小修复。
- [x] M4：运行定向测试验证修复。
- [x] M5：回写证据并执行 cleanup 预览。

## 预期验证

- `super_admin` 调用产品页不再被空可见范围过滤成 `0` 条。
- 非企宣普通用户仍保持原有指派范围限制。

## 当前状态

Completed.

## 完成结果

- 已定位根因：`ShowroomAdminController` 仅把 `showroom_publicity` 当作全量查看权限，导致系统 `super_admin` 用户也被误当成受限用户，产品列表会按空指派范围过滤成 `0` 条。
- 已补回归测试 `superAdminShouldBypassScopedVisibilityForProductPage`。
- 已做最小修复：展厅管理的高权限判断改为“企宣角色或超级管理员”。
- 已重打本地 `yudao-server.jar` 并重启运行时；当前 `48081` 进程为新 jar：
  - `PID 49380`
  - `backend-20260521-111450.jar`

## 最终验证

- RED：`ShowroomHttpApiIntegrationTest#superAdminShouldBypassScopedVisibilityForProductPage` -> FAIL
- GREEN：`ShowroomHttpApiIntegrationTest#superAdminShouldBypassScopedVisibilityForProductPage` -> PASS
- GREEN：`ShowroomHttpApiIntegrationTest#wholeProductAssignmentShouldExposeFillingStatusAndAssignedEditorAccess` -> PASS
- GREEN：`mvn -pl yudao-server -am -DskipTests package` -> PASS
- GREEN：本地运行时重启后 `http://127.0.0.1:48081/actuator/health` -> `200`
- GREEN：`task_closeout.py --mode preview` -> PASS

## Cleanup Keep

- `doc/tasks/20260521-showroom-super-admin-product-visibility/task.md`
- `doc/tasks/20260521-showroom-super-admin-product-visibility/execution-log.md`
