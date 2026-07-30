# Execution Log

## 2026-07-30

- User intent: 删除测试租户所有工艺路线，从芋道源码导出，导入测试租户，并分析是否一致。
- Preflight: 已读取 `docs/database-rules.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`。
- Dirty baseline: 发现非本任务文档改动，已提交 `238961af chore: baseline file upload task notes before route tenant copy`。
- BDD: 测试租户清空 -> Given 已唯一确认测试租户 ID When 删除工艺路线数据 Then 只删除该租户工艺路线相关表数据且删除后目标租户路线数为 0。
- BDD: 源租户导出导入 -> Given 源租户“芋道源码”存在工艺路线 When 用正式全量导出导入链路迁移 Then 测试租户获得同等路线数据。
- BDD: 一致性分析 -> Given 导入完成 When 比对源/目标租户路线相关表 Then 报告一致项、差异项和阻塞原因。

## 2026-07-30 Continued

- Preflight: 已复读 `docs/database-rules.md`、`docs/login-access.md`、`docs/e2e-rules.md`、`docs/local-runtime.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`。
- Tenant confirmation: DB 只读确认 `system_tenant` 中 `1=芋道源码`、`122=测试租户`，均唯一且未删除。
- Current target state: 删除后复核 `测试租户 tenant_id=122` 活跃 `mes_pro_route/mes_pro_route_process/mes_pro_route_product_bom` 均为 `0`。
- Export: 使用正式后端接口 `GET /admin-api/mes/pro/route/export-import-xlsx`，`tenant-id=1`，刷新导出 `artifacts/source_tenant_1_route_export_latest.xlsx`，大小 `26885` bytes。
- Import probe 1: 使用 `tenant-id=1 + visit-tenant-id=122` 导入，接口返回 `code=403`、`msg=没有该操作权限`，未落库。
- Import probe 2: 使用测试租户公开 E2E 账号线索 `测试租户/aoteman` 登录成功后导入，接口返回 `code=1040501417`、`msg=工艺路线导入导出 Excel 的 工序BOM 第 2 行 工序编码 不能为空`，结果写入 `artifacts/import_result_tenant_122.json`。
- Workbook evidence: `artifacts/workbook_sheet_summary.json` 记录导出工作簿 11 个 Sheet，包含 `工序BOM` 50 行。
- Blocker evidence: `artifacts/source_bom_missing_process.tsv` 显示源租户 46 条 `mes_pro_route_product_bom` 的 `process_id=0`，导出后 `工序编码` 为空。
- No partial import: `artifacts/route_related_counts_after_failed_import.tsv` 显示 `tenant_id=122` 的 13 个工艺路线相关表活跃记录均为 `0`。
- Experience consolidation: 已将“工艺路线跨租户导入导出数据包完整性门禁”合并到 `docs/database-rules.md`，并在 `docs/experience-index.md` 增加关键词路由。
- Verification: `git diff --check -- docs\database-rules.md docs\experience-index.md doc\tasks\20260730-route-tenant-export-import-consistency` -> PASS；UTF-8 读取检查 -> PASS。
- Status: blocked。导入导出链路无法完成一致性比对；继续前需确认修复源租户 BOM 正式工序绑定后重试，或先用备份 SQL 恢复测试租户删除前数据。

## 2026-07-30 Recovery Continuation

- User authorization: 用户确认“恢复芋道源码的就可以，遇到问题解决问题”。
- Source repair: 已备份源租户 BOM 行到 `artifacts/source_tenant_1_route_product_bom_before_repair.sql`，执行 `artifacts/repair_source_bom_process_bindings.sql`，修复 `45` 条活跃 `mes_pro_route_product_bom.process_id`，并确认源租户活跃 BOM 不再缺正式工序绑定。
- Re-export: 使用正式后端接口重新导出 `artifacts/source_tenant_1_route_export_repaired.xlsx`。
- RED: 正式导入 `source_tenant_1_route_export_repaired.xlsx` 到 `tenant_id=122` -> FAIL，接口返回 `code=500`、`msg=系统异常`；后端日志显示 `TooManyResultsException`，阻塞在 `MesProRouteWorkbookImportServiceImpl.validateProcesses` 解析测试租户工序编码。
- Duplicate check: 待导入工序编码中仅 `Z3710` 在测试租户存在两条活跃 `mes_pro_process`：`922795 / 球囊裁剪 / creator=1 / 2026-05-16` 与 `922865 / 球囊裁剪 / creator=codex / 2026-07-06`。
- Reference check: 全库含 `process_id` 的租户表扫描显示 `922865` 活跃引用仅存在于 `mes_dv_machinery_process` 和 `mes_md_workstation`，两条引用均带 `SPIKE/spike-route-excel-resource-split` 测试种子特征；`mes_pro_route_process`、`mes_pro_route_product_bom` 对 `922865` 无活跃引用。
- Repair plan: 先备份 `922795/922865` 及其 `mes_dv_machinery_process`、`mes_md_workstation` 引用行；随后在单事务内软删除 `922865` 和仅属于该重复工序的 SPIKE 引用行，保留正式 `922795`。

## 2026-07-30 48081 Completion

- User authorization: 用户明确要求“直接在正式环境48081里面测试”。本次使用本机 `http://127.0.0.1:48081` 正式后端链路。
- Runtime check: `Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> `UP`；目标租户导入前路线相关表仍为 `0`。
- Root cause: 源租户两条启用路线 `ROUTE-XLSX-00001`、`ROUTE-XLSX-00002` 当前工序和 ACTIVE/DRAFT 关系图快照均缺关键工序，导出如实写出 `关键工序=false`，导入启用时触发 `1040501003 / 工艺路线必须要有关键工序`。
- Repair evidence: 备份 `artifacts/source_tenant_1_key_process_before_repair.sql`；修复脚本 `artifacts/repair_source_key_process_flags.sql` 首次因临时表 collation 触发 `ERROR 1267` 并回滚，按 `mes_pro_route.code=utf8mb4_unicode_ci` 修正后重跑通过，结果见 `artifacts/repair_source_key_process_flags_result.tsv`。
- Key process result: `ROUTE-XLSX-00001` 关键工序为末道/END `Z830 / 纸塑袋封口（包装）`；`ROUTE-XLSX-00002` 关键工序为末道/END `Z2620 / 球囊测漏及全检`；`RT000028` 保持 `ER1561D10C0138 / 大包装工序`。
- Export: 使用 `.env` 默认本机账号在 `tenant-id=1` 正式导出 `artifacts/source_tenant_1_route_export_key_repaired.xlsx`，大小 `26933` bytes；工作簿抽查三条路线均有且仅有一个关键工序。
- GREEN: `POST /admin-api/mes/pro/route/import-workbook-xlsx`，`tenant-id=122` -> PASS，`code=0`，导入 `3` 条路线、`63` 条工序、`16` 条关联产品、`49` 条 BOM，结果见 `artifacts/import_result_tenant_122_after_key_repair_valid_export.json`。
- Consistency verification: `artifacts/route_import_consistency_report.json` 显示 `route_basic`、`route_processes`、`flow_edges`、`boundary_edges`、`layouts`、`products`、`product_boms`、`schedule_configs`、`flow_configs`、`flow_process_configs`、`batch_record_bindings` 全部 `match=true`；`artifacts/route_import_consistency_diffs.tsv` 仅表头，无差异行。
- Experience consolidation: 已将“启用路线关键工序预检”合并到 `docs/database-rules.md#工艺路线跨租户导入导出数据包完整性门禁`，并在 `docs/experience-index.md` 增加关键词路由。
- Status: ready_for_closeout。实现和验证完成，剩余 closeout/提交推送按项目收尾门禁另行处理。
