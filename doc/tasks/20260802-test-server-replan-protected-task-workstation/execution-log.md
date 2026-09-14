# Execution Log

## User Intent

用户要求修复测试服务器手动重排报错。截图显示多条阻塞原因为 `受保护任务未绑定工作站`；前置上下文为第三方报工导入后报工列表和排产进度已恢复，但重排仍失败。

## BDD / TDD

- BDD: protected feedback task can be replanned -> Given 测试服排产工单已有正式报工且重排会保护已报工任务, When 手动重排同一排产工单, Then 受保护任务必须有正式工作站和产线资源，不应因 `受保护任务未绑定工作站` 或 `受保护任务未绑定产线` 阻塞。
- RED: 测试服只读 SQL -> FAIL, 目标受保护任务存在正式报工工作站但 `mes_pro_task.workstation_id` 为空，触发截图同类阻塞。

## Milestone Updates

- 2026-08-02: 创建任务审计目录，准备执行只读 schema 与目标数据复核。
- 2026-08-02: RED 复现完成。目标任务 `925854/925855/925964/926006` 均存在正式报工，且每个任务只有一个 `feedback.workstation_id`；但 `mes_pro_task.workstation_id` 全部为 `NULL`。
- 2026-08-02: 进一步发现目标工作站 `922058/922066/922068/922073` 的 `production_line_id` 均为 `NULL`。重排代码确认在任务工作站修复后还会校验工作站产线。
- 2026-08-02: 正式父资源核对完成：测试服租户 1 存在启用产线 `900040/AUTO-LINE-01`、启用车间 `900011/车间1`、启用日历计划 `900030/AUTO-PLAN-01`。
- 2026-08-02: GREEN 数据修复完成。事务断言通过并提交，4 条任务工作站和 4 个测试工作站产线/车间绑定已补齐。
- 2026-08-02: Playwright 真实页面复测完成。两个目标排产工单均通过页面“手动重排 -> 确认应用重排”进入 `replanApply`，且 `blockingIssueCount=0`。
- 2026-08-02: 数据库后验完成。目标任务绑定仍存在，排产工单已产生最新 `REPLAN_APPLY` 操作日志，目标阻断消息扫描计数为 0。

## Verification Evidence

- Schema 核对：`mes_pro_task` 不存在 `schedule_order_id`，实际排产关联在 `mes_pro_feedback.schedule_order_id` 与 `mes_pro_schedule_order_process`；后续 SQL 已按真实 schema 调整。
- RED 查询结论：4 条目标任务均为 `workstation_id=NULL`、`start_time/end_time` 非空、正式报工工作站单一可追溯。
- 产线前置：`mes_md_production_line.id=900040` 启用，且其 `workshop_id=900011`、`calendar_plan_id=900030` 均可解析。
- GREEN SQL：`task_updated=4`、`workstation_updated=4`、`post_validation_ok`。
- GREEN E2E：`node doc/tasks/20260802-test-server-replan-protected-task-workstation/replan-real-e2e.cjs` -> PASS，证据文件 `doc/tasks/20260802-test-server-replan-protected-task-workstation/replan-real-e2e-result.json`。
- Post DB：`target_blocker_count=0`，目标任务 `925854/925855/925964/926006` 均绑定工作站，工作站均绑定 `900040/AUTO-LINE-01`。

## Blockers

- 无业务阻塞。收尾阻塞：当前工作区有大量既有脏改动且分支已 ahead，本任务未做提交/推送。

## 2026-08-02 Workstation Capacity Follow-up

- User intent: 用户要求如果本地有对应工作站，则把本地工作站数据复制到测试服务器，修复测试服排产员工作台工序列表班次产能为 0 的问题。
- BDD: process WIP shift capacity uses copied workstation -> Given 测试服 `Z2772/Z2510/Z2775/Z2971` 当前路线工序班次产能为 0, When 本地存在同租户同 `process_id` 的正式工作站并同步到测试服, Then 工作台四个目标工序应按路线排产配置和工作站 `shift_hours` 算出非 0 班次产能。
- RED: 测试服只读 SQL -> FAIL, `process_id=922920/922921/922919` 无启用未删除工作站，`process_id=922925` 仅有已删除工作站；本地存在 `922726/922727/922725/922731` 四个启用工作站。
- Scope: 仅同步 `mes_md_workstation.id IN (922725,922726,922727,922731)`、其本地 `mes_md_workstation_machine` 绑定 `610/611/612/613/614/615/618`，以及这些设备对应的正式 `mes_dv_machinery_process` 小时产能 `954/955/956/957/958/959/962`；不覆盖测试服已有非 0 工序工作站。
- Pre-write checks: 测试服目标工作站 ID/编码无冲突，目标工作站设备绑定 ID 无冲突；目标设备主数据 `47/48/49/50/51`、车间 `900010`、产线 `900040` 均存在且未删除。
- GREEN: 测试服事务 SQL -> PASS, 插入 `mes_md_workstation=4`、`mes_md_workstation_machine=7`、`mes_dv_machinery_process=7`。
- GREEN: 测试服工作台产能 SQL -> PASS, `Z2772=420.000000`、`Z2510=2340.000002`、`Z2775=270.000003`、`Z2971=254.999997`，四个目标工序均非 0。
- GREEN: `validate_database_schema.py --evidence .../database-schema-evidence.md` -> PASS。
- GREEN: `validate_bug_regression.py --evidence .../bug-regression-evidence.md` -> PASS。
- GREEN: 经验沉淀 -> PASS, 已将跨环境补工作站需核对当前 `process_id`、`shift_hours`、工作站设备绑定和 `mes_dv_machinery_process` 的门禁合并到 `docs/backend-development.md#第三方报工直报正式链路门禁`，并更新 `docs/experience-index.md` 关键词。
- GREEN: `git diff --check -- <本任务文档与经验文档>` -> PASS；UTF-8 读取检查 -> PASS。

## 2026-08-02 Temporary Workstation Alignment Follow-up

- User intent: 用户要求把测试服临时工作站改成与本机一致。
- BDD: temporary workstations align with local capacity -> Given 测试服截图中 8 个工序使用 `TPFB-WS-20260802-*` 临时工作站且班次产能低于本机, When 将这些工作站资源链路调整为本机同工序正式工作站口径, Then 12 个截图工序班次产能应与本机一致。
- RED: 测试服只读 SQL -> FAIL, `Z2600/Z2530/Z2630/Z3810/Z2570/Z2975/Z2976/Z2776` 的测试服工作站 `shift_hours=7.00` 且无设备绑定，本机同工序工作站 `shift_hours=10.50` 且有设备绑定。
- Safety check: 临时工作站已被正式数据引用，`mes_pro_feedback` 引用 3 条、`mes_pro_task` 引用 6 条；因此不得改主键或删除工作站，本次只更新临时工作站字段并补设备资源链路。
- Pre-write checks: 目标工作站编码无冲突，目标工作站设备绑定 ID 无冲突，目标设备工序产能 ID 与组合键无冲突。
- GREEN: 测试服事务 SQL -> PASS, 保留 9 条既有正式引用，更新临时工作站 8 行，插入 `mes_md_workstation_machine=14`、`mes_dv_machinery_process=14`。
- GREEN: 测试服工作台产能 SQL -> PASS, 12 个截图工序班次产能与本机一致：`Z2600=959.999996`、`Z2772=420.000000`、`Z2530=599.999999`、`Z2630=493.500000`、`Z2510=2340.000002`、`Z3810=1950.000003`、`Z2775=270.000003`、`Z2570=310.000005`、`Z2975=119.999996`、`Z2976=959.999996`、`Z2971=254.999997`、`Z2776=480.000003`。
- GREEN: `validate_database_schema.py --evidence .../database-schema-evidence.md` -> PASS。
- GREEN: `validate_bug_regression.py --evidence .../bug-regression-evidence.md` -> PASS。
- GREEN: `git diff --check -- <本任务文档>` -> PASS；UTF-8 读取检查 -> PASS。

## 2026-08-02 Full WIP Workstation Alignment Follow-up

- User intent: 用户要求继续检查还有没有不一致的，并改成一致。
- BDD: all current test WIP processes align with local workstation resources -> Given 测试服当前 WIP 工序列表有 26 条, When 与本机同 `route_code + process_code + process_id` 工序资源口径对比, Then 测试服工作站编码、班次小时、设备绑定数和班次产能应与本机一致。
- RED: 全量 WIP 差异 SQL -> FAIL, 测试服 26 条当前 WIP 中剩余 14 条不一致：`Z3710/Z5200/Z2972/Z2973/Z2974/Z2550/Z2580/Z2490/Z2774/Z2773/Z3850/Z2560/Z5600/Z2620`。
- Root cause: 测试服仍有 10 个临时工作站保持 `shift_hours=7.00` 或资源绑定缺失，另有 4 个工序没有启用工作站；本机对应工序均有 `WS-XLSX-00002-*` 正式口径。
- Safety check: `mes_md_workstation` 目标正式 ID/编码冲突为 0，`mes_md_workstation_machine` 目标 ID 冲突为 0，`mes_dv_machinery_process` 目标 ID 冲突为 0；车间 `900010`、产线 `900040`、14 个设备主数据均存在。
- Collation gate: 第一次事务因临时表默认 `utf8mb4_general_ci` 与真实表 `utf8mb4_unicode_ci` 比较失败而回滚，后验确认目标工作站/设备绑定/设备工序插入计数均为 0；重跑时临时表显式 `DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`，中文字段使用 UTF-8 HEX 还原。
- GREEN: 测试服事务落库成功，更新 10 个临时工作站字段，插入 4 个缺失工作站、23 条工作站设备绑定、23 条设备工序产能。
- GREEN: 全量 WIP 差异 SQL -> PASS, `TEST_WIP_ROWS=26`、`LOCAL_WIP_ROWS=40`、`MISMATCH_COUNT=0`。
- Final capacities: 测试服 26 个当前 WIP 工序均为 `WS-XLSX-00002-01` 到 `WS-XLSX-00002-26`，`shift_hours=10.50`，班次产能覆盖 `493.500000/1550.000004/270.000003/420.000000/2340.000002/1950.000003/739.999995/599.999999/254.999997/959.999996/520.000005/200.000000/109.999995/119.999996/1560.000005/310.000005/959.999996/585.000003/500.000004/200.000000/311.000004/2950.000001/1199.999997/480.000003/1649.999999/420.000000`。
- GREEN: 清理守卫 -> PASS, `codex_align_wip_workstations_20260802` 存储过程残留计数为 0，临时 `TPFB-WS-20260802-*` 编码残留计数为 0。
- GREEN: 经验沉淀 -> PASS, 已将数据修复 SQL 的 `ERROR 1137 Can't reopen table` 临时表重复读取门禁合并到 `docs/database-rules.md#数据修复临时表排序规则门禁`，并更新 `docs/experience-index.md` 关键词。
