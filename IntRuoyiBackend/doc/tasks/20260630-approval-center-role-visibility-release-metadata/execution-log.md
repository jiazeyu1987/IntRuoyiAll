# 20260630 审批中心角色可见性 SQL 发布元数据修复执行日志

BDD: 审批中心角色可见性 SQL 必须满足发布契约 -> Given 主分支真实发布前 migration policy gate 扫描到 `20260630_approval_center_role_visibility.sql` When 执行发布门禁 Then 该 SQL 必须具备合法 `release-migration` 元数据，且门禁不再因缺少元数据失败。

INFO: task-created -> 已创建审批中心角色可见性 SQL 发布元数据修复任务文档与执行日志。
GREEN: experience-preflight -> PASS，已读取并命中 `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-agent-checklist.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
RED: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> FAIL, missing release-migration metadata: D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260630_approval_center_role_visibility.sql
GREEN: release-migration-metadata-added -> PASS，已为 `sql/mysql/20260630_approval_center_role_visibility.sql` 补充 `-- release-migration: allowedEnvironments=test,backup,prod; dependsOn=; type=data; riskLevel=medium`。
RED: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> FAIL, missing release-migration metadata: D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql\20260630_dcc_admin_full_config_managed_scope.sql
GREEN: migration-metadata-followup-added -> PASS，已继续为 `sql/mysql/20260630_dcc_admin_full_config_managed_scope.sql` 与 `sql/mysql/20260630_mes_pro_work_order_erp_snapshot_fields.sql` 补充最小 `release-migration` 元数据，准备重新执行 migration policy gate。
RED: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> FAIL, dependsOn missing migration '20260513_dcc_base_schema.sql' for migrationId '20260630_dcc_admin_full_config_menu'
GREEN: dependsOn-format-fixed -> PASS，已将 `sql/mysql/20260630_dcc_admin_full_config_menu.sql` 的 `dependsOn` 从带 `.sql` 后缀的旧格式改为 migrationId 形式。
GREEN: python -X utf8 D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\script\release\run-release-migration-policy-gate.py --sql-root D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\sql\mysql -> PASS
