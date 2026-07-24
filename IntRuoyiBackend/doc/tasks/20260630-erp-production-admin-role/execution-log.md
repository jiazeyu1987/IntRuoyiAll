# Execution Log：ERP生产管理员角色隔离与 admin 绑定（后端/SQL）

- `2026-06-30 任务创建`：建立后端/SQL 任务文档，目标是在 `tenant_id=1` 下新增 `ERP生产管理员` 并收敛 ERP 生产管理菜单。
- `BDD: tenant 1 缺失 ERP生产管理员 时正式创建 -> Given 芋道源码租户没有该角色 / When 执行正式 SQL / Then 自动创建启用角色并写入稳定 code。`
- `BDD: 非 super_admin/ERP生产管理员 不再拥有 ERP 生产管理菜单 -> Given tenant 1 其他角色历史上可能被授过该树菜单 / When 执行正式 SQL / Then 这些绑定被软删除，不再暴露菜单。`
- `BDD: admin 拥有 ERP生产管理员 绑定 -> Given admin 需要访问 ERP 生产管理 / When 执行正式 SQL / Then system_user_role 中存在 admin 与新角色的启用绑定。`
RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q -> FAIL, 初次合同断言未覆盖显式排序规则声明，暴露 SQL 幂等变量声明需要收敛。
GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q -> PASS
- `GREEN: tenant1-scope-audit -> PASS`，已确认菜单树 `6020/6021/6022/6023/6024/6025/6026` 当前只授予 `super_admin(role_id=1)`，适合按最小范围新增独立角色。`
- `GREEN: experience-preflight -> PASS`，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `powershell-memory.md` 与 `login-access.md`；本轮高风险动作限定为本机 Docker MySQL `int-ruoyi-mysql` 下 `tenant_id=1` 的角色/菜单/用户绑定幂等写入，以及随后对 `芋道源码/admin` 的本机真实登录验证，不访问远端服务器。
- `RED: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q -> FAIL`，初次合同断言未覆盖显式排序规则声明，暴露 SQL 幂等变量声明需要收敛。
- `GREEN: python -X utf8 -m pytest D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\tests\test_erp_production_admin_role_sql.py -q -> PASS`，`6 passed`。
- `GREEN: local-sql-apply -> PASS`，使用 `python -X utf8` 二进制管道将 `sql\mysql\20260630_erp_production_admin_role.sql` 应用到本机 Docker MySQL，最终重放 `returncode=0`。
- `GREEN: local-db-verification -> PASS`，`ERP生产管理员(role_id=910295, code=erp_production_admin)` 已创建且 UTF-8 十六进制与预期一致；`admin(user_id=1)` 已新增 `system_user_role.id=244` 绑定。
- `GREEN: local-role-scope-verification -> PASS`，`tenant_id=1` 下 `menu_id in (6020,6021,6022,6023,6024,6025,6026)` 的有效角色仅剩 `910295(erp_production_admin)`；`role_id=1(super_admin)` 的对应绑定已软删除。
