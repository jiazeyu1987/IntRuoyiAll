# 任务：电子签名左侧菜单显示子页签

## 任务目标

修复电子签名一级菜单下看不到子页签的问题。动态菜单迁移必须在 `电子签名` 下创建可见子菜单：`总览`、`文件签名记录`、`批记录签名记录`、`用户授权`、`长期留存`、`周期复核`、`CSV质量包`、`统一策略`。旧 `DCC电子签名管理`、`eDHR签名记录` 不恢复为独立入口，只保留为统一菜单体系下的权限项。

## 里程碑

- [x] M1：复现并用 RED 静态契约锁定“必须有可见子菜单”的菜单 SQL 要求。
- [x] M2：修改统一电子签名菜单迁移，创建 8 个可见子菜单并迁移角色菜单关系。
- [x] M3：运行菜单 SQL 契约测试和迁移安全验证。
- [x] M4：将迁移应用到本机真实库后，用测试租户真实登录确认左侧菜单显示子页签。
- [x] M5：完成证据、清理预览与提交。

## 预期验证

- `pytest script/tests/test_unified_electronic_signature_menu_sql.py`
- 本机 MySQL 只读核对 `system_menu` / `system_role_menu` 中电子签名子菜单与测试租户角色绑定。
- Playwright 登录 `http://localhost:8081`，使用 `测试租户/aoteman/111111`，确认左侧 `电子签名` 下显示 8 个子页签。

## 当前状态

已完成。菜单 SQL、真实库应用和真实 Playwright E2E 均已通过，任务证据已校验，临时截图已清理。

## Current Status

completed

## 前一任务检查

- 后端上一电子签名任务 `20260624-signature-governance-route-subtabs` 已标记完成。
- 当前后端仓库存在 DCC/MES 其他任务脏改动；本任务只修改电子签名菜单 SQL、对应测试和本任务文档。

## 经验门禁

- `docs/login-access.md`：真实 E2E 默认本机 `http://localhost:8081`，使用测试租户 `测试租户/aoteman/111111`；登录失败直接阻塞，不切换环境或账号。
- `docs/server-access.md`：本次只操作本机源码、本机 MySQL 与本机前端入口，不访问测试服、备份服或正式服。
- `docs/worktree-memory.md`：当前主工作区有非本任务脏改动，提交时只暂存本任务相关文件，不整仓暂存。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。根因是动态菜单只创建一级入口、未创建可见子菜单；修复菜单数据契约和角色绑定。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 电子签名显示可见子菜单 -> Given 用户拥有电子签名菜单权限 / When 登录后台并展开左侧电子签名 / Then 电子签名下显示 总览、文件签名记录、批记录签名记录、用户授权、长期留存、周期复核、CSV质量包、统一策略。`
- `BDD: 旧签名入口不恢复为独立菜单 -> Given 已执行统一电子签名菜单迁移 / When 查询旧 DCC 与 eDHR 菜单 / Then 6815 和 900026 不作为独立可见菜单，只作为统一菜单下的权限项保留原权限码。`
- `BDD: 子菜单角色授权随统一菜单迁移 -> Given 角色已拥有统一电子签名菜单 / When 迁移执行完成 / Then 角色同时拥有 8 个电子签名子菜单，登录后可在左侧看到子页签。`

## Cleanup Keep

- `doc/tasks/20260624-signature-governance-visible-child-menu/task.md`
- `doc/tasks/20260624-signature-governance-visible-child-menu/execution-log.md`
- `doc/tasks/20260624-signature-governance-visible-child-menu/bug-regression-evidence.md`
- `doc/tasks/20260624-signature-governance-visible-child-menu/database-schema-evidence.md`
