# 20260630 自动排产权限拆分租户包发布阻塞修复执行日志

BDD: 自动排产动作菜单进入租户包 -> Given 20260624 自动排产权限拆分会新增 900180/900181/900182 子菜单 When required SQL 执行完成 Then 含智能排产父菜单 900120 的租户包必须同步包含这 3 个动作菜单。

BDD: 智能排产递归授权 SQL 不再被前置菜单缺口阻断 -> Given 测试租户排产员角色会执行 20260617_mes_scheduler_role_smart_scheduling_tab.sql When 发布 required SQL 继续执行 Then 不因租户包缺少 900180/900181/900182 而触发 `Missing MES smart scheduling menu tree in tenant package`。

GREEN: experience-preflight -> PASS，已读取 `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`。
INFO: publish-test-failure-evidence -> 维护仓真实测试服发布 `op-2026-06-30T121019283302800Z-2df9fdb7-7d01-43f9-bb22-9c4c63ec405c` 在执行 required SQL `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 时失败；日志证据为 `ERROR 1644 (45000) at line 166: Missing MES smart scheduling menu tree in tenant package; cannot grant scheduler role tab`，且发布锁已 `LOCK_RELEASED`。
INFO: readonly-root-cause-check -> 只读检查测试服真实库后确认：`system_menu` 下智能排产递归树存在 35 条有效菜单；测试租户 `122` 的排产员角色为 `910235 / mes_scheduler`；测试租户租户包 `114 / mes-release-20260618-0056` 已包含 `900120/5590/5580/5262/5540`，但缺少 `900180/900181/900182`。同时仓库 SQL `20260624_mes_auto_schedule_permission_split.sql` 只新增菜单与 `system_role_menu`，未同步租户包 `menu_ids`，因此后续 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 的递归树校验被前置契约缺口阻断。
RED: auto-schedule-tenant-package-contract -> FAIL，真实测试服发布已证明旧版 `20260624_mes_auto_schedule_permission_split.sql` 未把 `900180/900181/900182` 同步进包含 `900120` 的租户包，导致 `20260617_mes_scheduler_role_smart_scheduling_tab.sql` 在测试租户 `122` 上触发 `Missing MES smart scheduling menu tree in tenant package`。
GREEN: auto-schedule-tenant-package-test -> PASS，`python -X utf8 -m pytest script\tests\test_mes_auto_schedule_permission_sql.py -q` 在后端仓根目录通过，`3 passed in 0.10s`；新增门禁已覆盖“自动排产动作菜单同步租户包”契约。
GREEN: migration-policy-gate -> PASS，`python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql` 通过；修复后 `20260624_mes_auto_schedule_permission_split.sql` 的迁移元数据与依赖契约仍有效。
