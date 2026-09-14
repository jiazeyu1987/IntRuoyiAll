# Execution Log

## Intent

- 用户要求修复测试服务器排程日历错误：`排程工单生产用料清单子项未映射本地物料: 881MO093613 / A003.017.01.004.2008`。
- 用户提供测试服登录身份标签：租户 `芋道源码`、用户名 `zhaojie`；密码不写入任务记录。

## BDD

- BDD: 排程日历加载不应因生产用料清单缺本地物料映射失败 -> Given 测试服租户 `芋道源码` 存在排程工单 `881MO093613` 且用料清单包含子项 `A003.017.01.004.2008`，When 用户 `zhaojie` 打开排程日历，Then 页面不再提示该子项未映射本地物料，相关接口返回业务成功。
- BDD: 数据修复必须保持同租户正式引用 -> Given 目标子项需要补齐 `child_material_id`，When 写入测试服数据，Then 引用必须指向同租户未删除的正式 `mes_md_item`，不得跨租户、不得默认成功、不得删除业务行。

## Gate Log

- 2026-08-01：读取 `bug-regression-fix-loop`、`database-schema-delivery`、`backup-disaster-recovery-readiness`、`playwright` 技能。
- 2026-08-01：读取 bug/database/recovery evidence contract。
- 2026-08-01：读取 `docs/server-access.md`、`docs/database-rules.md`、`docs/release-backup-restore.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/login-access.md`、`docs/e2e-rules.md`。
- 2026-08-01：确认本轮仅授权测试服务器 `172.30.30.58` 与目标租户/用户路径，不涉及代码发布或其它服务器。

## Evidence

- RED: `GET /admin-api/mes/pro/schedule-calendar/month?month=2026-08` with tenant `芋道源码` / user `zhaojie` -> FAIL, HTTP 200 but business code `1040260005`, message `排程工单生产用料清单子项未映射本地物料: 881MO093613 / A003.017.01.004.2008`.
- Read-only schema and tenant check: target tenant `id=1, name=芋道源码`; target tables present: `mes_kingdee_production_material_list`, `mes_md_item`, `mes_pro_work_order`, `mes_pro_work_order_bom`.
- Target table row counts in tenant `1`: PML `2593`, item `16317`, work order `1984`, work-order BOM `193`.
- Root cause: PML rows `7477/7478/7479` under work order `925868`, production order `881MO093613`, have `child_material_id IS NULL`.
- Same-tenant formal item resolution is unique: `7477 -> 902266`, `7478 -> 902260`, `7479 -> 901957`; matching work-order BOM rows for these items are absent, so `work_order_bom_id` remains unchanged.
- Backup: `/opt/intruoyi/runtime/task-backups/20260801-fix-test-schedule-material-item-mapping/pml_related_20260801020129.sql.gz`, bytes `870121`, SHA256 `d0003f77ee2aa048068c1e571cd20f6d3654927fdaf5d11c2856a937e36d0cee`, `gzip -t` PASS.
- GREEN: target PML mapping update -> PASS; first transaction updated rows `7477/7478/7479`, then same-tenant unique PML mapping update filled remaining `153` resolvable tenant-1 rows; remaining unique-resolvable null mappings `0`, unresolved null mappings `305` left untouched.
- Formal PML sync attempt for `CODEX-FACTOR-20260708093210`: item sync by code returned permission `403` for user `zhaojie`; PML by production order sync returned business success but duplicate work-order code still left PML on non-scheduled work order `925779`.
- Data repair for `CODEX-FACTOR-20260708093210`: inserted missing item master rows `923985/923986` after confirming no target ID/code conflicts and parent unit/type rows existed; moved `29` PML rows from unscheduled work order `925779` to scheduled work order `925877`, with `child_material_id` fully resolved and `work_order_bom_id` left nullable when no matching BOM existed.
- GREEN: API final verification -> PASS; `GET /admin-api/mes/pro/schedule-calendar/month?month=2026-08` returned HTTP `200`, business code `0`, data present, `dayCount=31`.
- GREEN: production material list API samples -> PASS; detail list `SIM-881MO093613` returned `27` rows, detail list `PPBOM00309004` returned `29` rows.
- GREEN: token-bootstrap page rendering -> PASS; `http://172.30.30.58:8081/mes/pro/schedule-calendar` loaded with month API code `0`, title visible, no `未映射本地物料` / `缺少生产用料清单` / `系统异常`, console/page error counts `0`.
- E2E blocker: real login page with user `zhaojie` was blocked by slider captcha text `请完成安全验证 / 向右滑动完成验证`; page rendering verification is therefore explicitly token-bootstrap, not a claim that the real login captcha path passed.
- Validator: bug regression evidence -> PASS, `Bug regression evidence is valid.`
- Validator: database schema evidence -> PASS, `Database schema evidence is valid.`
- Validator: backup disaster recovery evidence -> PASS, `Backup disaster recovery evidence is valid.`
- Experience consolidation: merged repeated work-order/PML ownership and post-first-fix calendar verification lessons into `docs/database-rules.md#生产用料清单跨环境白名单-upsert-门禁`; added keywords in `docs/experience-index.md`.
- Status: implementation and required verification complete; task moved to `ready_for_closeout` before cleanup preview/apply.
- Cleanup preview: PASS; kept `task.md`, `execution-log.md`, `verification-report.md`; planned deletion only for task-local evidence files `bug-regression-evidence.md`, `database-schema-evidence.md`, `recovery-evidence.md`.
- Cleanup apply: PASS; deleted only the three task-local evidence files after validator PASS summaries were copied into retained reports.
- Git closeout: BLOCKED; `git status --short --branch` reported `int_main...origin/int_main [ahead 10]` plus many unrelated dirty files across backend, frontend, other task docs, and the prior production material list task. To avoid mixing unrelated concurrent work, no baseline/commit/push was performed for this task.

## Blockers

- Real login-page E2E is blocked by slider captcha on the deployed test frontend. Data/API repair is complete; token-bootstrap page rendering passed.
- Git commit/push is blocked by unrelated dirty worktree state and an already-ahead shared branch. Task remains `ready_for_closeout` rather than `completed`.
