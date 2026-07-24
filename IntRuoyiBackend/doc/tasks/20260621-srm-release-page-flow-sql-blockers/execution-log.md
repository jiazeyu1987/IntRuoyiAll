# 执行日志: 20260621-srm-release-page-flow-sql-blockers

- BDD: D7-2 路由守卫不得把查询按钮当页面路由 -> Given 测试库可能只残留 type=3 查询按钮而缺少对应 type=2 页面路由 / When 执行 20260619_srm_d7_2_supplier_access_risk / Then SQL 必须补齐缺失页面路由，而不是误判页面已存在后再被 fail-fast 校验拦住。
- BDD: Phase 1 access profile 幂等加列必须兼容真实发布目标库 -> Given 测试服真实 MySQL 不接受 ALTER TABLE ... ADD COLUMN IF NOT EXISTS 语法 / When 执行 20260620_srm_phase1_supplier_access_profile / Then SQL 必须用兼容目标库的正式幂等写法补齐缺失列，而不是因 1064 语法错误阻断页面发布。
- BDD: mark-tested 必须识别 Phase 1 portal 迁移元数据 -> Given 页面真实执行 mark-tested 时会扫描发布 SQL 的 release-migration metadata / When 读取 20260621_srm_phase1_supplier_portal.sql / Then 文件首行必须声明正式 metadata，不能被误判为迁移元数据缺失。

- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`；本任务只在源码层修复真实页面链路暴露的发布输入阻塞，不做环境绕过。
- GREEN: previous-task-check -> PASS，上一后端任务 `20260620-post-release-role-e2e-gate-backend-closeout` 已 `COMPLETED`。
- GREEN: page-flow-blocker-evidence -> PASS，已从维护仓真实页面链路证据确认三处阻塞：
  - `op-2026-06-21T002749215627300Z-ceee763a-3578-4831-8ff5-c075d7295215` 在 `20260619_srm_d7_2_supplier_access_risk.sql` 阶段报 `Missing SRM supplier-profile route menu for get-permission-info`
  - `op-2026-06-20T231058149703700Z-a3e87030-a2c9-45fb-bbea-01cf147a8da9` 在 `mark-tested` 阶段报 `Release migration metadata missing: ...20260621_srm_phase1_supplier_portal.sql`
  - `op-2026-06-21T030252892482500Z-0cf0dc70-5cbe-48bd-8d3c-740fbc777f96` 在 `20260620_srm_phase1_supplier_access_profile.sql` 阶段报 `ERROR 1064 (42000) ... ADD COLUMN IF NOT EXISTS`
- RED: python -X utf8 -m pytest script\tests\test_srm_d7_d10_sql_contract.py -q -> FAIL，新增断言 `test_srm_t1_route_menu_guards_do_not_confuse_query_buttons_with_pages` 命中旧守卫，证明 D7-2 SQL 仍把 `type=3` 查询按钮误当成页面路由。
- GREEN: d7-2-route-guard-fix -> PASS，已将 `20260619_srm_d7_2_supplier_access_risk.sql` 中 `supplier-access`、`supplier-risk`、`supplier-profile` 三个页面路由守卫统一收窄为只接受页面菜单自身，或接受 `permission=<query>` 且 `type=2` 的页面菜单。
- GREEN: python -X utf8 -m pytest script\tests\test_srm_d7_d10_sql_contract.py -q -> PASS，返回 `13 passed`。
- RED: python -X utf8 -m pytest script\tests\test_srm_phase1_schema_sql.py -q -> FAIL，新增断言后先复现 `20260621_srm_phase1_supplier_portal.sql` 缺少 `release-migration` 首行元数据，`mark-tested` 会误判迁移元数据缺失。
- GREEN: srm-phase1-portal-release-metadata-fix -> PASS，已为 `20260621_srm_phase1_supplier_portal.sql` 补齐 `-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=20260620_srm_phase1_supplier_access_profile; type=schema; riskLevel=medium`。
- RED: python -X utf8 -m pytest script\tests\test_srm_phase1_schema_sql.py -q -> FAIL，进一步将 access-profile 契约收窄为“必须使用正式幂等写法而非 ADD COLUMN IF NOT EXISTS”后，旧 SQL 稳定命中断言。
- GREEN: srm-phase1-access-profile-syntax-fix -> PASS，已将 `20260620_srm_phase1_supplier_access_profile.sql` 重写为 `ensure_srm_phase1_supplier_access_profile()` 存储过程；按列查询 `information_schema.COLUMNS`，仅在缺列时执行对应 `ALTER TABLE ... ADD COLUMN ...`，执行后 `CALL` 并回收过程定义。
- GREEN: python -X utf8 -m pytest script\tests\test_srm_phase1_schema_sql.py -q -> PASS，返回 `10 passed`。
- GREEN: python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql -> PASS，返回 `status=passed`，且依赖链正确包含 `20260620_srm_phase1_supplier_access_profile -> 20260621_srm_phase1_supplier_portal -> 20260621_srm_phase3_purchase_order`。
