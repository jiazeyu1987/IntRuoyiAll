# 任务：ERP生产管理员角色隔离与 admin 绑定（后端/SQL）

- Task ID: `20260630-erp-production-admin-role`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在 `tenant_id=1` 下新增正式角色 `ERP生产管理员`，只允许该角色与 `super_admin` 看到 `ERP系统 -> 生产管理` 菜单树，并把 `admin` 绑定到该角色。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-showroom-product-import-create-missing\task.md`
- 状态：`blocked`
- 处理说明：上一后端任务已因用户切换需求暂停；本次为新的正式角色权限 SQL 交付。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md` 与 `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、任务文档与测试输出统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 真实登录验证发生在 `芋道源码/admin`，必须先走官方最小路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式幂等 SQL 与 pytest 契约测试维护角色/菜单/用户绑定。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: tenant 1 缺失 ERP生产管理员 时正式创建 -> Given 芋道源码租户没有该角色 / When 执行正式 SQL / Then 自动创建启用角色并写入稳定 code。`
- `BDD: 非 super_admin/ERP生产管理员 不再拥有 ERP 生产管理菜单 -> Given tenant 1 其他角色历史上可能被授过该树菜单 / When 执行正式 SQL / Then 这些绑定被软删除，不再暴露菜单。`
- `BDD: admin 拥有 ERP生产管理员 绑定 -> Given admin 需要访问 ERP 生产管理 / When 执行正式 SQL / Then system_user_role 中存在 admin 与新角色的启用绑定。`

## Milestones

1. M1：建立后端任务文档并确认 menu tree / role tree 现状。`completed`
2. M2：补 RED 契约测试。`completed`
3. M3：实现正式 SQL 并跑到 GREEN。`completed`
4. M4：支撑根仓真实登录验证。`completed`

## Expected Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q`
- `Get-Content -Encoding utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260630_erp_production_admin_role.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro`

## Final Verification

- `python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q` -> `PASS (6 passed)`
- `python -X utf8` 二进制管道执行 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260630_erp_production_admin_role.sql | docker exec -i int-ruoyi-mysql mysql --default-character-set=utf8mb4 -uroot -p123456 ruoyi-vue-pro` -> `PASS`
- 本机库回查 -> `PASS`，`ERP生产管理员(role_id=910295)` 已创建且 `admin(user_id=1)` 已绑定；`6020/6021/6022/6023/6024/6025/6026` 仅 `role_id=910295` 保留有效绑定，`super_admin` 对应绑定已软删除。

## Current Blockers

- 无。
