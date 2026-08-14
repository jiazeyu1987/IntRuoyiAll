# C00 Execution Log

## 2026-08-12 启动与 BDD

- 用户授权范围：仅在 C00 worktree 内修改任务文档、后继 SQL migration/script 和 `MesQaPqcSchemaTest`；不得提交、合并、删除 worktree、启动服务、push、部署或修改共享业务数据。
- 读取规则：已读取 AGENTS、后端开发规则、数据库规则、任务收尾规则、PowerShell 编码/编排规则、C00设计包、共享接口合同和迁移契约。
- BDD: 最小增量 schema -> Given 已执行 20260811 QA-DCC migration, When 执行后继 migration, Then 只补路线关系和活跃订单快照且可重复执行。
- BDD: 历史版本证据回填 -> Given 历史 activeOrder 的 task 只有一个锁定 version, When 运行 backfill, Then 按 version 所属 regulation/DCC 回填并与路线关系交叉验证；零 task 或多 version 进入阻塞清单。
- BDD: 路线关系版本不复用 -> Given 同一路线已有软删除 version 历史, When 执行 schema 约束和 DF03 原子更新测试, Then 当前关系唯一且 tenant/route/version 历史零重复。
- BDD: task规则身份可迁移 -> Given 历史 task 含 FIRST、AM巡检、PM巡检和旧合并 PATROL, When 回填 inspectionRuleKey, Then 前三者唯一映射且旧合并 PATROL 进入阻塞清单。
- BDD: 正式提交一task一event -> Given 历史已提交 task 存在零条、一条或多条 PQC 正式 event, When 回填 hash 和 event 指针, Then 只有唯一且可重建 CanonicalPqcSubmissionV1 者通过，其余阻塞。
- Experience gate: 命中数据库迁移漂移、PowerShell Maven `-D` 引号、Maven reactor `-am`、静态源码合同工作目录和 Windows Maven 卡住门禁；已摘入 `task.md`。

## 2026-08-12 RED precheck

- RED PRECHECK: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS unexpectedly, 6 tests run, BUILD SUCCESS.
- 判定：目标测试进入 Surefire，但当前 `MesQaPqcSchemaTest` 只覆盖既有 20260802/20260811 合同，尚未表达 C00 必需的路线-DCC关系表、active-order QA快照、task rule key/hash/event 和 PQC event 生成唯一键。继续先补 RED 断言，再实现 SQL。

## 2026-08-12 RED/GREEN/Regression

- RED: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: C00断言要求 `20260812_mes_pqc_dcc_qa_c00_schema.sql`，当前文件不存在；目标测试进入 Surefire，7 tests run, 1 error。
- Implemented: 新增 `20260812_mes_pqc_dcc_qa_c00_schema.sql`、`preflight.sql`、`backfill.sql`、`postflight.sql`、`rollback.sql`，并保留 C00 静态 schema 断言。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests run, BUILD SUCCESS。
- Regression: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，14 tests run, BUILD SUCCESS。
- SQL static evidence: `rg -n -g '20260812_mes_pqc_dcc_qa_c00*.sql' '<contract markers>' IntRuoyiBackend/sql/mysql` -> PASS，命中 route-DCC、active-order QA快照、task rule/hash/event、PQC event generated key、metadata/hash/row/blocker markers。
- Forbidden SQL scan: `rg -n -g '20260812_mes_pqc_dcc_qa_c00*.sql' '<forbidden table names>' IntRuoyiBackend/sql/mysql` -> no matches。
- Diff check: `git diff --check` -> no whitespace errors；仅提示 Windows LF-to-CRLF warning。

## 2026-08-12 Changed Paths

- `IntRuoyiBackend/sql/mysql/20260812_mes_pqc_dcc_qa_c00_schema.sql`
- `IntRuoyiBackend/sql/mysql/20260812_mes_pqc_dcc_qa_c00_preflight.sql`
- `IntRuoyiBackend/sql/mysql/20260812_mes_pqc_dcc_qa_c00_backfill.sql`
- `IntRuoyiBackend/sql/mysql/20260812_mes_pqc_dcc_qa_c00_postflight.sql`
- `IntRuoyiBackend/sql/mysql/20260812_mes_pqc_dcc_qa_c00_rollback.sql`
- `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/MesQaPqcSchemaTest.java`
- `doc/tasks/20260812-frontline-pqc-dcc-qa-c00/**`

## 2026-08-12 Supervisor Return Fix

- BLOCKER: 主管复核退回，原因是 preflight 错误依赖本次 schema、preflight 在 schema 前直接查询本次新建结构、schema 在 backfill/postflight 前提前添加历史数据相关唯一约束。
- BDD: 维护窗口顺序 -> Given 只读 preflight 必须先于 C00 schema 执行, When 本次新表或新列尚不存在, Then preflight 只能用 information_schema 与动态 SQL 安全盘点，不得因新结构缺失失败。
- BDD: 约束收紧分层 -> Given 历史 PQC task/event 可能存在重复或歧义, When schema 执行, Then schema 只新增 nullable 列、生成列和基础表结构；唯一约束与 NOT NULL 必须在 postflight 零阻塞后动态执行。
- RED: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，expected reason: 新断言要求 preflight 起始依赖、schema 不提前创建收紧唯一约束、postflight 零阻塞后创建/收紧唯一约束；当前 SQL 未满足，`MesQaPqcSchemaTest.java:296`。
- Implemented: preflight 改为 `dependsOn=20260811_mes_qa_dcc_project_scope`，并用 `@c00_route_dcc_binding_ready` + `PREPARE stmt FROM @sql` 在新表缺失时跳过重复关系盘点；schema 移除 `uk_mes_pro_process_pool_event_pqc_task`、`uk_mes_pqc_task_rule_identity`、`uk_mes_pqc_task_submitted_event` 和旧索引替换；postflight 增加重复/歧义 blocker，并在 `c00_postflight_blocker_count = 0` 后动态执行 NOT NULL 与唯一约束收紧。
- GREEN: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests run, BUILD SUCCESS。
- Regression: `mvn -pl yudao-module-mes -am "-DskipITs" "-Dtest=MesQaInspectionRegulationServiceTest,MesQaPqcSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，14 tests run, BUILD SUCCESS。
- SQL marker scan: PASS，命中 preflight 起始依赖、动态安全分支、postflight blocker、postflight 唯一约束创建、input hash、affected row count 和 blocker markers。
- Forbidden scan: PASS，未命中重复 DCC-QA 关系表、item-type 表、active-order context 表、fallback/default success/silent downgrade，也未命中 preflight 依赖本次 schema。
- Schema premature unique scan: PASS，`schema.sql` 未命中三个收紧唯一约束和旧索引替换。
- Database evidence validator: PASS，`database-schema-evidence.md` valid。
- Diff check: PASS，无 whitespace error；仅 Windows LF-to-CRLF warning。
