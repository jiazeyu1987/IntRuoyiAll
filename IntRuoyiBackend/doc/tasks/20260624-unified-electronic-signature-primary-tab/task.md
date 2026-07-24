# 任务：统一电子签名一级页签后端与菜单迁移

## 任务目标

将电子签名从 DCC/eDHR 分散入口收敛为统一一级菜单 `电子签名`，后端门户入口返回统一页内子页签路径，并通过菜单迁移保留原权限码但不再暴露 `DCC电子签名管理`、`eDHR签名记录` 独立菜单。

## 里程碑

- [x] M1：创建独立 worktree，确认前一任务完成并记录经验门禁。
- [x] M2：先写后端与菜单 RED 测试，锁定统一一级菜单与统一 tab 路径。
- [x] M3：实现 portal adapter 路径调整与菜单迁移 SQL。
- [x] M4：运行后端 targeted 测试与迁移静态验证。
- [x] M5：配合前端真实 E2E 验证并提交。

## 预期验证

- `mvn -pl yudao-module-dcc "-Dtest=SignatureGovernancePortalServiceTest,SignatureGovernanceControllerTest" test`
- `python -X utf8 script/tests/test_unified_electronic_signature_menu_sql.py`
- `mvn -pl yudao-server -Dtest=SignatureGovernancePolicySourceConfigTest test`
- 前端真实 E2E 通过后在合并结果上复验。

## 当前状态

已完成。后端 adapter、菜单迁移 SQL、targeted 单元测试、迁移静态验证、融合后真实 E2E 均已通过。

## 前一任务检查

- 后端前一相关任务 `20260623-unified-electronic-signature-tab` 已标记完成，允许继续本任务。
- 主工作区存在非本任务脏改动，本任务仅在独立 worktree 内实现和提交。

## 经验门禁

- `docs/worktree-memory.md`：前后端成对 worktree，分支同名；运行与 E2E 必须显式确认 FE/BE 端口、数据库、Redis 与当前 worktree 归属；合并后必须在合并结果复验，通过前不得清理 worktree。
- `docs/login-access.md`：真实 E2E 使用本机测试租户 `测试租户/aoteman/111111`；登录失败、权限缺失或电子签名前置缺失必须阻塞，不得静默切换账号或环境。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：统一页签保持密集操作台风格，表格、工具栏、状态标签与页面结构需保持清晰克制。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。缺菜单、权限、策略或接口数据时显式失败或展示阻断，不做默认成功。
- `是否从根因和长期维护角度解决`：是。通过统一菜单与统一 tab 路径收口后续模块电子签名入口，保留原权限码作为后端鉴权契约。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 电子签名一级菜单承载全部签名入口 -> Given 系统菜单已执行统一菜单迁移 / When 用户拥有电子签名相关权限登录 / Then 侧边菜单只暴露一级电子签名入口，不再暴露 DCC电子签名管理 或 eDHR签名记录。`
- `BDD: 门户入口返回统一页内子页签 -> Given DCC/eDHR adapter 已接入统一门户 / When 请求 portal overview / Then DCC、eDHR 主入口路径分别指向 /signature-governance?tab=file-signatures 与 /signature-governance?tab=batch-signatures。`
- `BDD: 原权限码继续用于后端鉴权 -> Given 原 DCC 签名管理或 eDHR 签名查询权限仍被页面和接口使用 / When 菜单迁移执行 / Then 原权限码作为统一菜单下权限项保留，角色授权不丢失。`

## Cleanup Keep

- `doc/tasks/20260624-unified-electronic-signature-primary-tab/task.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/execution-log.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/backend-api-evidence.md`
- `doc/tasks/20260624-unified-electronic-signature-primary-tab/database-schema-evidence.md`
