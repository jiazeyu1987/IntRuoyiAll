# Execution Log: 球囊/棘突球囊工艺路线工序导入

## 2026-07-08

- BDD: 普通球囊路线挂入 23 道工序 -> Given Excel Sheet1 包含 `球囊扩张导管` 23 道唯一工序 / When 执行迁移 / Then `球囊扩张导管工艺路线` 拥有 23 条按 Excel 顺序排列的路线工序。
- BDD: 棘突球囊路线挂入 26 道工序 -> Given Excel Sheet1 包含 `棘突球囊扩张导管` 26 道唯一工序 / When 执行迁移 / Then `棘突球囊扩张导管工艺路线` 拥有 26 条按 Excel 顺序排列的路线工序。
- BDD: 跨产品同编码不串线 -> Given 两个产品都包含 `Z2630` / When 迁移挂载路线 / Then 工序按 `tenant_id + product_name + code + name` 唯一定位，不通过名称 fallback。
- BDD: 异常数据 fail fast -> Given 目标工序缺失、路线重复或已有路线工序顺序冲突 / When 执行迁移 / Then SQL 失败并暴露明确错误，不静默覆盖。
- GREEN: experience-preflight -> PASS，已读取 PowerShell、项目经验索引、database-schema-delivery、BDD/TDD 验收结构；本任务不涉及服务器写入、发布、真实 E2E 或 worktree 合并。
- RED: `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q` -> FAIL，新增路线工序契约断言失败，当前迁移缺少 `tmp_balloon_route_seed`、两条路线编码/名称、23/26 道工序数量和 fail-fast 路线挂载校验。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q` -> PASS，6 tests；迁移已包含两条路线 seed、路线工序 seed、`process_id/next_process_id` 挂载和缺失/冲突 fail-fast 校验。
- BLOCKER: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260708-balloon-route-process-import/database-schema-evidence.md` -> FAIL，evidence 缺少校验脚本要求的 `BDD:` / `RED:` / `GREEN:` 标记，已补充固定标记。
- BLOCKER: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，H2 测试表 `mes_pro_process` 缺少 `product_name/manual_shift_capacity` 字段，已同步测试 schema。
- BLOCKER: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，H2 测试表 `mes_dv_machinery_process` 缺少 `process_code` 字段，已同步测试 schema。
- BLOCKER: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，复用工位人工产能为空时 mapper 保留既有 `singleStandardHourlyCapacity=12.50`，旧测试断言期望清空为 `null`，已对齐当前服务行为。
- GREEN: `python -X utf8 -m pytest script/tests/test_mes_balloon_process_device_capacity_sql.py -q` -> PASS，6 tests。
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260708-balloon-route-process-import/database-schema-evidence.md` -> PASS，Database schema evidence is valid。
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProRouteProcessControllerTest,MesProRouteServiceImplTest,BalloonProcessDeviceMappingImportServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，10 tests / 0 failures / 0 errors。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260708-balloon-route-process-import --mode preview` -> PASS，keep `task.md`、`execution-log.md`、`database-schema-evidence.md`，delete/blocked/warnings 均为 `<none>`。
- BLOCKER: git commit -> BLOCKED，`sql/mysql/20260708_mes_balloon_process_device_capacity.sql` 与 `script/tests/test_mes_balloon_process_device_capacity_sql.py` 在本任务开始前已是未跟踪文件，本轮只是在其中补路线挂载；若整文件提交会夹带前序未提交的工序/设备产能导入内容，违反“只提交本任务直接改动”门禁。

## 运行库修复证据（2026-07-08 21:06:24）

- 修复目标：现有两条工艺路线 `ROUTE-XLSX-00001` / `ROUTE-XLSX-00002` 的 `mes_pro_route_process.process_id` 指向当前 `mes_pro_process` 主数据，并维护 `next_process_id` 链路。
- 修复范围：仅 tenant_id=1 的两条目标路线；未改历史排产、批记录、任务快照；普通球囊路线多余 sort=24 行仅软删除。
- 执行环境：连接 `127.0.0.1:23306/ruoyi-vue-pro`；MySQL 服务端 `@@port` 回报 `3306`。
- GREEN: experience-preflight -> PASS, 已按 PowerShell/数据库变更门禁使用 UTF-8 Python 脚本执行并后置断言。
- GREEN: db-route-process-ROUTE-XLSX-00001 -> PASS, route_id=900025 active_count=23 blank_count=0 sort=1..23 broken_next_count=0。
  - endpoint sort=1: Z2630 吹球囊成型 -> next Z3710 球囊裁剪
  - endpoint sort=23: Z830 纸塑袋封口（包装） -> next NULL
- GREEN: db-route-process-ROUTE-XLSX-00002 -> PASS, route_id=900026 active_count=26 blank_count=0 sort=1..26 broken_next_count=0。
  - endpoint sort=1: Z2630 吹球囊成型 -> next Z3710 球囊裁剪
  - endpoint sort=26: Z2620 球囊测漏及全检 -> next NULL


## 接口验证证据（2026-07-08 21:08:54）

- GREEN: api-list-by-route-ROUTE-XLSX-00001 -> PASS, route_id=900025 count=23 blank_process_code_name=0，首道 `Z2630 吹球囊成型`，末道 `Z830 纸塑袋封口（包装）`。
- GREEN: api-list-by-route-ROUTE-XLSX-00002 -> PASS, route_id=900026 count=26 blank_process_code_name=0，首道 `Z2630 吹球囊成型`，末道 `Z2620 球囊测漏及全检`。
