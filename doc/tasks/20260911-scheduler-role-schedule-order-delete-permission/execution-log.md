# Execution Log

BDD: 排产员获得删除权限 -> Given 每个目标租户只有一个活动排产员角色且删除菜单唯一 / When 前向迁移执行 / Then 只新增目标角色到删除菜单的活动绑定并可重复执行。

BDD: 权限前置异常阻断 -> Given 排产员角色或删除菜单缺失/重复 / When target preflight 或迁移执行 / Then 明确失败且不产生部分授权。

BDD: 非目标角色与用户绑定保持不变 -> Given 数据库还有管理员和其它业务角色 / When 迁移执行 / Then 其角色菜单及所有用户角色绑定保持不变。

BDD: 目标账号页面验收 -> Given 测试服迁移完成并精确刷新权限缓存 / When `芋道源码/zhaojie` fresh 登录排产工单页 / Then 删除 permission 与删除按钮可见但不执行删除。

GREEN: experience-preflight -> PASS, 已读取数据库迁移合同、角色权限差异同步、危险按钮诊断、服务器、PowerShell、worktree 与收尾规则；目标范围固定为精确角色和 permission，不修改旧迁移、不扩大其它角色、不清空 Redis、不触碰非测试环境。

GREEN: worktree-freeze -> PASS, `D:\IntRuoyiWorktree\20260911-scheduler-delete-permission` 分支 `codex/20260911-scheduler-delete-permission` 从 `int_main@2e949e115d967a38246b3375ad6ab8ff2eb1182a` 创建，初始 clean；登记 `int_main slot=42`、frontend `8257`、backend `48257`，本任务不启动服务。

RED: `python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_scheduler_schedule_order_delete_permission_sql.py -q` -> FAIL, `5 failed`，预期原因是正式迁移和 target preflight 尚不存在。

GREEN: scheduler-delete-permission-static-and-mysql -> PASS, 静态合同与 MySQL 8 隔离验证 `6 passed`；覆盖首次执行、租户 122 两条历史软删除只恢复最小 ID、租户 1 新增、重复执行、非目标角色不变、重复活动绑定阻断和精确回滚，临时测试库已删除。

RED: release-policy-target-preflight-directory -> FAIL, 当前主线迁移策略把 `sql/mysql/target-preflight/*.preflight.sql` 误当正式迁移，报告缺少 `release-migration` 元数据；这是目标预检目录候选过滤缺失。

GREEN: release-policy-target-preflight-directory -> PASS, 复用已验证 r44 候选判定规则，manifest/policy 统一通过 `is_release_migration_candidate` 排除 `target-preflight`；`test_release_migration_policy_gate.py` 使用任务独立 basetemp 为 `10 passed`。

GREEN: diff-check-before-commit -> PASS, `git diff --check` 无错误，仅有 Windows 行尾转换提示。

GREEN: database-schema-evidence-validator -> PASS, `validate_database_schema.py --evidence ...\database-schema-evidence.md` 通过，validator self-test 通过；RED/GREEN、数据安全与回滚均已归档。

GREEN: project-experience-consolidation -> PASS, 现有 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-09-10-安装包构建前必须执行结构迁移演练与目标只读预检` 已明确非结构迁移必须位于 `sql/mysql/target-preflight` 且为只读合同；本次复用 r44 已验证候选过滤规则，无新的独立长期经验，未新建或重复追加经验文档。

GREEN: branch-runtime-port-guard-before-commit -> PASS, 分支 `codex/20260911-scheduler-delete-permission` 使用登记 slot 42，guard 返回 frontend 8257/backend 48257；未启动本地服务。
