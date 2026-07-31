# 20260725 Codex Test Management Admin E2E

## Task Goal

让本机 `int_main` 环境中的 `芋道源码/admin` 在真实前端看到 `系统管理 > 测试管理` 页签，并完成覆盖权限响应、动态菜单、侧边栏入口、页面加载和关键权限按钮的 Playwright E2E 验证。

## Milestones

1. 创建独立任务边界并读取前端、数据库、登录、E2E、运行态和收尾门禁。
2. 用真实前端 E2E 复现 `芋道源码/admin` 缺少 `测试管理` 页签。
3. 只读核对本机数据库中的菜单、角色、角色菜单和 admin 角色绑定缺口。
4. 执行已交付的幂等迁移 `20260724_system_codex_test_management.sql`，补齐本机权限数据。
5. 复跑真实前端 E2E，验证页签可见、页面可进入、测试项接口返回成功。
6. 记录验证报告、当前状态和剩余非本任务并发改动边界。

## Expected Verification

- 后端 `http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- 前端 `http://127.0.0.1:8081/login?redirect=/index` 返回 HTTP 200。
- RED：修复前权限响应缺少 `system:codex-test:query`，真实 E2E 失败。
- GREEN：修复后权限响应包含 `system:codex-test:query`，动态菜单包含 `system/codex-test-management/index`。
- GREEN：真实侧边栏显示 `测试管理`，进入页面后 `codex-test-case/page` 返回业务码 0，并可见自然语言测试方法、检查点和新增按钮。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；根因是本机权限迁移未落库，已按既有幂等迁移补齐菜单、角色、租户套餐和 admin 绑定。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

### 菜单权限稳定业务键

- Trigger: 动态菜单页签缺失、`system_menu`、`system_role`、`system_role_menu`、`system_user_role` 或租户套餐菜单合并。
- Preflight check: 以 `permission='system:codex-test:query'`、`role.code='codex_test_admin'` 和 `tenant_id + username` 核对真实库。
- Verification: 迁移合同、只读 SQL 计数、登录后权限响应和真实前端菜单共同验证。
- Forbidden action: 不按固定 ID 猜测角色/菜单，不手工删除环境数据，不用 API-only 代替前端路径。

### 真实 E2E 门禁

- Trigger: 运行 `系统管理 > 测试管理` 真实前端验证。
- Preflight check: 确认 8081/48081 属于 `E:\IntRuoyi`，登录身份为本机 `芋道源码/admin`，凭据不写入任务文档。
- Verification: Playwright 操作真实登录页、侧边栏和页面；API 仅用于登录后权限响应与页面加载的只读/最终核验。
- Forbidden action: 不切换账号、租户、端口或远端环境，不使用 mock 或静态合同冒充真实 E2E。

## Cleanup Keep

- doc/tasks/20260725-codex-test-management-admin-e2e/system-codex-test-management-real-summary.json
## Final Verification

- Cleanup preview/apply -> PASS，删除当前任务临时截图，保留 task.md、execution-log.md、verification-report.md 和 E2E 摘要 JSON。
- Final result: `芋道源码/admin` 已可通过真实前端看到并进入 `系统管理 > 测试管理`。