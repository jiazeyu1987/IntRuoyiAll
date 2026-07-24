# 执行日志: 20260621-post-release-role-e2e-gate-backup-scheduler-role

BDD: 备份服 admin 租户缺排产员角色时发布 gate 必须自补齐 -> Given tenant_id=1 芋道源码租户存在 showroom_publicity 与 wenkong 角色但缺少启用中的排产员角色 / When 执行 20260618_post_release_role_e2e_gate.sql / Then 迁移必须正式创建或启用可用的排产员角色，并继续完成 zhaojie 账号绑定与菜单授权。
BDD: 已存在排产员角色时发布 gate 仍保持幂等 -> Given tenant_id=1 已存在启用中的排产员角色 / When 重复执行 20260618_post_release_role_e2e_gate.sql / Then 迁移只保持既有角色、用户和菜单契约，不重复创建脏角色，也不放宽其他 fail-fast 门禁。

- GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md` 命中的 `release-backup-restore.md` 与 `server-access.md`；当前只允许继续做真实环境只读审计、业务仓 SQL 契约修复和本地回归测试，不允许手工写库绕过备份服发布阻塞。
- GREEN: task-created -> PASS，已创建任务目录并补齐 `task.md` 的目标、里程碑、经验门禁、设计约束检查与当前状态，可进入 RED。
- GREEN: maintenance-flow-blocker-read -> PASS，只读核对维护仓 `D:\ProjectPackage\Int\IntRuoyiMaintance\runtime\runtime-control\op-2026-06-21T075931265053700Z-7d81cab2-c347-4f49-8618-1427c837cf2f.json` 与对应 log 后确认：备份服真实页面发布失败于 `20260618_post_release_role_e2e_gate.sql`，错误为 `Missing enabled scheduler role; cannot prepare zhaojie smart scheduling E2E account`。
- GREEN: backup-vs-test-admin-role-audit -> PASS，只读查询测试服/备份服真实库后确认：两端 `tenant_id=1 / 芋道源码` 都有 `showroom_publicity` 与 `wenkong`；测试服额外存在启用中的 `排产员` 角色，而备份服不存在任何启用中的 `排产员/计划员/mes_scheduler/scheduler` 角色。这说明当前阻塞不是页面参数漂移，而是 `20260618_post_release_role_e2e_gate.sql` 对备份环境基线假设过强。
- RED: `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` -> FAIL，新增合同 `test_post_release_role_gate_bootstraps_missing_scheduler_role_for_admin_tenant` 后，旧 SQL 稳定缺少 `INSERT INTO system_role` 自补齐逻辑，并继续保留 `Missing enabled scheduler role; cannot prepare zhaojie smart scheduling E2E account` 的硬失败语义。
- GREEN: scheduler-role-bootstrap-fix -> PASS，已将 `sql/mysql/20260618_post_release_role_e2e_gate.sql` 修复为：先按 admin 租户已有角色候选选出 `scheduler_role_id`；缺角色时插入新的 `排产员` 角色并用 `LAST_INSERT_ID()` 回填；命中历史角色时统一启用、规范基础字段并继续复用该角色；不再把“缺排产员角色”直接当成发布阻塞。
- GREEN: `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py -q` -> PASS，11 passed。
- GREEN: `python -X utf8 -m pytest script\tests\test_post_release_role_e2e_gate_sql.py script\tests\test_mes_scheduler_role_smart_scheduling_tab_sql.py -q` -> PASS，16 passed。
- GREEN: `python -X utf8 script\release\run-release-migration-policy-gate.py --sql-root sql\mysql` -> PASS，返回 `status=passed`。
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260621-post-release-role-e2e-gate-backup-scheduler-role\bug-regression-evidence.md` -> PASS，缺陷回归证据结构有效。
- 当前结论：业务仓已按严格 TDD 收口“备份服 admin 租户缺排产员角色导致 post-release gate 失败”的根因，且相关 SQL/迁移契约回归已通过。下一步必须回填维护仓主任务：由于发布包输入已变化，当前 `release-20260621-page-full-flow-v5` 不能继续使用，必须从新的 releaseTag 重新走页面全链路。
- GREEN: maintenance-rerun-followup -> PASS，维护仓后续已使用新的 `release-20260621-page-full-flow-mainmerge-v2` 重新通过真实页面 `build-release`，并成功推进到测试服部署阶段；页面链路当前新的阻塞已变为 `20260619_mes_edhr_deployment_license_interface.sql` 的菜单 ID 冲突，说明本任务修复的“缺排产员角色导致 post-release gate 失败”已不再阻断当前发布链路。
