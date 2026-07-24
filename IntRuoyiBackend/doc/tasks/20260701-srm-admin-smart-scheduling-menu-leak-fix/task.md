# 任务：修复 SRM管理员 残留智能排产菜单授权

- Task ID: `20260701-srm-admin-smart-scheduling-menu-leak-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-07-01`
- Current Status: `completed`
- User Request: `继续`

## Task Goal

修复测试服 `SRM管理员 / srm_admin` 角色错误持有 `智能排产` 菜单的问题，并从 SQL 合同上同时解决两个根因：

1. `20260629_srm_admin_role_visibility.sql` 不能再依赖固定 `role_id=910240` 改写或复用历史非 SRM 角色；
2. `srm_admin` 角色的菜单范围必须正式收口为纯 SRM 菜单树，不能保留历史遗留的非 SRM 菜单授权。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-role-config-package-cross-env-menu-identity\task.md`
- 状态：`completed`
- 处理说明：上一个后端任务已完成；本轮独立修复测试服角色菜单泄漏问题。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 本轮命中 PowerShell / Windows shell 经验；按 UTF-8、严格 TDD 与任务台账执行。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文源码、SQL、任务文档统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 本轮当前阶段仅允许测试服只读核对；若后续需要正式应用到测试服，必须在独立高风险门禁下执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。菜单范围必须显式收口，缺前置则 fail fast。
- `是否从根因和长期维护角度解决`：是。既修当前测试服已落地的错误授权，也修 future migration 合同，避免同类角色 ID 复用再次污染。
- `是否存在临时补丁或绕过`：否。不采用手工删测试服单条 `system_role_menu` 记录作为唯一修复方案。

## BDD 场景

- `BDD: srm_admin 角色只能拥有 SRM 菜单树 -> Given 数据库中已存在 srm_admin 角色且角色上挂有历史非 SRM 菜单 / When 执行 SRM 管理员菜单收口 migration / Then 角色仅保留 SRM 菜单树授权，非 SRM 菜单授权被正式回收。`
- `BDD: 历史 role_id=910240 被其他角色占用时不应被改写成 srm_admin -> Given tenant 1 中 role_id=910240 已被非 SRM 历史角色占用且尚不存在 code=srm_admin 的角色 / When 执行 SRM 管理员 migration / Then 系统必须为 srm_admin 分配独立安全角色记录，而不是直接把 910240 改名复用。`
- `BDD: admin 继续通过 srm_admin 角色获得 SRM 菜单 -> Given tenant 1 存在启用 admin 用户 / When 执行修正后的 SRM 管理员 migration / Then admin 仍会绑定 srm_admin 角色并且仅获得 SRM 菜单树。`

## Milestones

1. M1：建立任务文档并确认测试服根因与影响范围。`completed`
2. M2：先写 RED SQL 契约测试，锁定角色 ID 复用与菜单泄漏。`completed`
3. M3：实现最小正式 SQL 修复与补偿 migration。`completed`
4. M4：运行 GREEN 验证并补证据。`completed`
5. M5：更新任务文档并执行 closeout 预览。`completed`

## Expected Verification

- RED：
  - `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py`
  - `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py`
- GREEN / REGRESSION：
  - `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py`
  - `python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-admin-smart-scheduling-menu-leak-fix\bug-regression-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260701-srm-admin-smart-scheduling-menu-leak-fix\database-schema-evidence.md`

## Current Blockers

- 无代码阻塞；完成仓库修复后，如需真正消除测试服现象，还需要把包含补偿 migration 的发布包应用到测试服。

## Final Verification Result

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py -q` -> FAIL（RED，旧 SQL 仍固定依赖 `910240`，且缺少 cleanup migration）
- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_visibility_sql.py D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_srm_admin_role_scope_cleanup_sql.py -q` -> PASS（10 passed）

## Final Result

- `20260629_srm_admin_role_visibility.sql` 已正式收口为：
  - 动态解析 `srm_admin` 角色 ID；
  - 不再固定依赖 `role_id=910240`；
  - 在补 SRM 菜单前先软删除该角色上所有非 SRM 菜单授权。
- 新增 `20260701_srm_admin_role_menu_scope_cleanup.sql`，用于补偿已落地环境中 `srm_admin` 的非 SRM 菜单遗留授权。
- 本轮未直接修改测试服数据库；若要关闭现场，需要后续把包含本次 SQL 的发布包应用到测试服并复核 `wangmin2` 权限返回。
