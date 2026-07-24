# 任务：DCC 审阅矩阵 route node stage_type 运行时 schema 漂移修复

## 任务目标

修复旧版 DCC 运行时库缺少 `dcc_category_approval_route_node.stage_type` 及同批规则元数据列时，后端查询审阅矩阵节点直接抛出 `Unknown column 'stage_type'` 的问题；要求通过正式 schema repair 覆盖历史库，不引入查询降级、字段兜底或吞异常。

## 里程碑

- [x] M1：创建任务文档，确认前一后端任务状态并记录经验门禁、设计约束检查与 BDD 场景。
- [ ] M2：先补 RED 回归，锁定 runtime schema repair 必须覆盖 `stage_type` 及同批规则元数据列。
- [ ] M3：最小修改 DCC runtime schema repair，使旧库可幂等补齐缺失列。
- [ ] M4：运行定向验证、补齐缺陷证据与收尾预览。

## 预期验证

- `python -X utf8 -m pytest script/tests/test_dcc_route_node_rule_metadata_runtime_repair_sql.py -q`
- `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-route-node-stage-type-schema-drift\bug-regression-evidence.md`

## 当前状态

已完成。

## 最终验证结果

- `python -X utf8 -m pytest script/tests/test_dcc_route_node_rule_metadata_runtime_repair_sql.py -q`：PASS
- `mvn -pl yudao-module-dcc -Dtest=DccBaseSchemaTest#mysqlRuntimeRepairSchemaShouldUpgradeLegacyDccTables "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260625-dcc-route-node-stage-type-schema-drift\bug-regression-evidence.md`：PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260625-dcc-route-node-stage-type-schema-drift --mode preview`：PASS，`delete/blocked/warnings` 均为 `<none>`

## 前一任务检查

- 后端前一任务 `ruoyi-vue-pro/doc/tasks/20260625-dcc-browser-current-directory-only/task.md` 已标记“已完成”，无未关闭阻塞，允许继续本任务。
- 当前后端仓库存在其他未归属脏改动；本任务只修改 DCC runtime schema repair、DCC schema 回归测试与本任务文档，不覆盖其他改动。

## 经验门禁

- `docs/experience-index.md`：本任务仅做本机源码、SQL 脚本与定向单测，不执行真实 E2E、数据库写入、服务器联调、发布、备份恢复或其他高风险动作，因此不触发 `experience-preflight` 门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。根因是运行时库 schema 漂移，必须正式补齐列，不允许通过查询裁剪字段或 try/catch 掩盖真实缺陷。
- `是否从根因和长期维护角度解决`：是。统一把 `stage_type` 同批元数据列纳入 DCC 运行时 repair 脚本，覆盖历史库初始化与升级路径。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 旧版 route node 表缺少审阅矩阵元数据列时可正式升级 -> Given 历史 DCC 运行时库只具备旧版 dcc_category_approval_route_node 字段 When 执行 runtime schema repair Then stage_type、subject_label、marker、subject_type、subject_id、subject_name、subject_department_path、rule_remark 均被幂等补齐。`
- `BDD: 审阅矩阵节点查询不再因缺列直接报错 -> Given route node 运行时表已通过 runtime schema repair 补齐规则元数据列 When 后端按 routeId 查询 dcc_category_approval_route_node Then SQL 可按完整字段集正常查询，不再抛出 Unknown column stage_type。`

## Cleanup Keep

- `doc/tasks/20260625-dcc-route-node-stage-type-schema-drift/task.md`
- `doc/tasks/20260625-dcc-route-node-stage-type-schema-drift/execution-log.md`
- `doc/tasks/20260625-dcc-route-node-stage-type-schema-drift/bug-regression-evidence.md`
