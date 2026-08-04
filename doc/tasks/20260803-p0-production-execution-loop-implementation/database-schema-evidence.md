# Database Schema Evidence - P0 M4 主提交幂等根事件字段

## Data

- Data change goal：数量片段必须持久化正式 `production_submit_event_id`，使 FIFO 分配、质量数量闸和批记录追溯都能从数量片段回到唯一 `PRODUCTION_SUBMIT` 根事件。
- Affected entities：`mes_pro_process_pool_quantity_fragment`、`MesProProcessPoolQuantityFragmentDO`、H2 测试 schema `create_tables.sql`、P0 schema 合同测试 `MesProcessPoolSchemaTest`。
- Database engine and migration tool：MySQL release SQL under `IntRuoyiBackend/sql/mysql`，H2 用于 Maven 单元/集成测试 schema。

## Migration

- 新增迁移：`IntRuoyiBackend/sql/mysql/20260803_mes_process_pool_quantity_fragment_submit_root.sql`。
- 迁移元数据：`allowedEnvironments=test,backup,prod; dependsOn=20260803_mes_process_pool_event_idempotency; type=schema; riskLevel=medium`。
- 迁移动作：新增 `production_submit_event_id`，从同租户、未删除、`event_type='PRODUCTION_SUBMIT'` 的正式父事件回填为 `event_id`，缺正式根事件或非生产提交根事件时 `SIGNAL SQLSTATE '45000'` fail-fast，再改为 `NOT NULL` 并添加 `idx_mes_pro_process_pool_fragment_submit_event`。
- 测试 schema：H2 `mes_pro_process_pool_quantity_fragment` 同步 `production_submit_event_id NOT NULL` 和索引。

## Safety

- 不对缺失根事件、非 `PRODUCTION_SUBMIT` 事件或断链历史行填默认值。
- 不解析 `rawPayload`、备注、页面文案或时间接近关系来补齐根事件。
- 生产代码只允许 `PRODUCTION_SUBMIT` 事件创建数量片段；非生产提交事件携带数量片段时直接 fail-fast。

## Rollback

- 回滚策略：如目标环境迁移前发现历史数量片段不能正式绑定生产提交根事件，停止发布并先做业务 backfill 方案；不得强行删除或默认填充历史行。
- 结构回滚：若本迁移尚未应用，可移除该迁移并恢复 DO/schema 改动；若已应用，需在确认无依赖上线后用显式回滚脚本移除索引和列。

## BDD:

- Given 生产提交创建数量片段。
- When 后端持久化 `mes_pro_process_pool_quantity_fragment`。
- Then 每个数量片段必须写入正式 `production_submit_event_id=PRODUCTION_SUBMIT.id`，且重复提交幂等返回既有事件时不得新增数量片段。
- Given 历史数量片段缺正式生产提交根事件或绑定到非生产提交事件。
- When 执行 release migration。
- Then 迁移必须 fail-fast，而不是用默认值、rawPayload 或人工说明掩盖断链。

## RED:

- `mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，`MesP0FrontlineSubmitIdempotencyTest` 两个用例因 H2 `production_submit_event_id` 为 `NOT NULL` 且 insert 未写该列报 `NULL not allowed for column "production_submit_event_id"`。

## GREEN:

- `mvn -pl yudao-module-mes -am "-Dtest=MesP0FrontlineSubmitIdempotencyTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesProcessPoolSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`。
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql` -> PASS，`status=passed`，`migrationCount=419`。

## Verification

- `MesProcessPoolSchemaTest` 断言 `MesProProcessPoolQuantityFragmentDO.productionSubmitEventId` 存在，并校验迁移包含 `production_submit_event_id`、`idx_mes_pro_process_pool_fragment_submit_event` 和历史断链 fail-fast 文案。
- P0 相邻回归 `MesProcessPoolEventServiceTest,MesProcessPoolTimeSignatureTest,MesProcessPoolPqcEventTest,MesFrontlinePqcContextServiceTest,MesP0PqcQualityAllocationGateTest,MesP0ProductionExecutionTraceServiceTest,MesTeamLeaderTraceServiceTest,MesTeamLeaderReportConfirmationServiceTest,MesProcessPoolSubmitEventServiceAdapterTest,MesP0FrontlineSubmitIdempotencyTest` -> PASS，`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`。

## Blockers

- 已只读连接本机运行库 `127.0.0.1:23306/ruoyi-vue-pro`，但 P0 正式字段/索引尚未应用，不能证明本新增迁移已在运行态完成。
- 历史断链 fail-fast 尚未在目标 MySQL 数据上执行验证；当前因 schema 缺失被验证器跳过，避免对不存在字段执行 historical SQL。
- PQC 重复提交唯一性、跨确认 FIFO 消耗持久化、批记录字段审计和真实 E2E PASS 仍待后续 slice。

## P0-T00B Runtime Migration Verifier

## Data

- Data change goal：为 P0 正式 SQL 增加只读运行态迁移验证器，确保真实运行库已具备主闭环依赖的正式字段、索引和历史断链阻塞检查。
- Affected entities：`mes_pro_process_pool_pqc_record`、`mes_pro_process_pool_event`、`mes_pro_process_pool_quantity_fragment`、`mes_pro_process_pool_submission_review`，以及 `IntRuoyiBackend/script/p0/verify_p0_runtime_migration.py`。
- Database engine and migration tool：MySQL release SQL under `IntRuoyiBackend/sql/mysql`；运行态验证器通过 `information_schema.COLUMNS`、`information_schema.STATISTICS` 和只读历史断链计数核验真实库状态。

## Migration Verification Contract

- Required env：`P0_RUNTIME_DB_HOST`、`P0_RUNTIME_DB_PORT`、`P0_RUNTIME_DB_NAME`、`P0_RUNTIME_DB_USER`、`P0_RUNTIME_DB_PASSWORD`。
- Required columns：`production_submit_event_id`、`event_idempotency_key`、`recordbook_entry_id`、`review_signature_id`、`review_signature_user_id`、`review_signature_snapshot_json`。
- Required indexes：`idx_mes_pro_process_pool_pqc_submit_event`、`uk_mes_pro_process_pool_event_idem`、`idx_mes_pro_process_pool_fragment_submit_event`、`idx_mes_pp_review_signature`。
- Historical checks：PQC 记录缺生产提交根事件、生产提交缺幂等键、生产提交缺记录本入口、数量片段缺生产提交根事件或绑定非 `PRODUCTION_SUBMIT`。

## Safety

- 验证器只读连接真实运行库，不写业务表、不做 backfill、不删除历史断链行。
- 缺环境变量、缺字段、缺索引、历史断链或 MySQL client 缺失均输出机器可读 blocker，不返回默认成功。
- 验证器不读取或解析 `rawPayload`，避免用非正式载荷伪造结构化迁移完成状态。

## Rollback

- 验证器脚本本身可按任务范围移除；已应用的正式 SQL 不由该脚本回滚。
- 若运行态验证发现 blocker，发布应停止并按正式迁移/backfill 方案处理；不得通过忽略脚本、填默认值或人工说明继续放行。

## BDD:

- Given P0 主闭环依赖正式来源 ID、幂等键、签名字段和索引。
- When 开发者准备证明真实运行态可以承载闭环 trace、FIFO、PQC 和批记录追溯。
- Then 必须运行只读迁移验证器，字段、索引和历史断链检查均通过才可把运行态迁移核验记为 PASS；缺任一 `P0_RUNTIME_DB_*` 或正式来源断链必须 BLOCKED。

## RED:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增静态合同要求 `IntRuoyiBackend/script/p0/verify_p0_runtime_migration.py` 存在，初始文件缺失。
- local-config runtime verifier command using `application-local.yaml` datasource -> FAIL，输出 `status=FAIL` / `P0_RUNTIME_VERIFIER_FAILED`，MySQL 返回 `Unknown column 'production_submit_event_id' in 'where clause'`，证明验证器在 schema 缺失时仍继续执行 historical SQL，没有按字段/索引缺口 fail-fast。
- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增合同 `test_p0_runtime_migration_verifier_stops_history_when_schema_is_missing` 失败于缺少 `schema_blockers` 分支。

## GREEN:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`。
- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py --print-contract` -> PASS，输出包含 required env、7 个 required columns、4 个 required indexes 和 4 个 historical checks。
- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`；验证器已在字段/索引缺失时返回 `P0_RUNTIME_SCHEMA_BLOCKED` 并跳过 historical SQL。

## Migration Verification

- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py` -> BLOCKED，输出 `status=BLOCKED` 和 `P0_RUNTIME_ENV_MISSING`，缺 `P0_RUNTIME_DB_HOST/P0_RUNTIME_DB_PORT/P0_RUNTIME_DB_NAME/P0_RUNTIME_DB_USER/P0_RUNTIME_DB_PASSWORD`。
- local-config runtime verifier command using `application-local.yaml` datasource -> BLOCKED，已只读连接本机 MySQL `127.0.0.1:23306/ruoyi-vue-pro`，输出 `P0_RUNTIME_SCHEMA_BLOCKED`、7 个 `P0_RUNTIME_MIGRATION_MISSING_COLUMN` 和 4 个 `P0_RUNTIME_MIGRATION_MISSING_INDEX`；历史断链检查因 schema 缺失被跳过。
- 缺失字段：`mes_pro_process_pool_pqc_record.production_submit_event_id`、`mes_pro_process_pool_event.event_idempotency_key`、`mes_pro_process_pool_event.recordbook_entry_id`、`mes_pro_process_pool_quantity_fragment.production_submit_event_id`、`mes_pro_process_pool_submission_review.review_signature_id`、`mes_pro_process_pool_submission_review.review_signature_user_id`、`mes_pro_process_pool_submission_review.review_signature_snapshot_json`。
- 缺失索引：`idx_mes_pro_process_pool_pqc_submit_event`、`uk_mes_pro_process_pool_event_idem`、`idx_mes_pro_process_pool_fragment_submit_event`、`idx_mes_pp_review_signature`。
- 这些 BLOCKED 是运行态 schema 前置缺失，不是迁移合同 PASS；不得据此把 P0 真实 E2E 或 M6 收尾写成完成。

## Blockers

- 本机运行库可连接，但 P0 正式字段/索引尚未应用；需要先执行正式迁移或经授权的正式 schema 修复方案后，才能复跑运行态迁移核验。
- 目标 MySQL 历史断链结果尚未知；schema 齐备后若任一 historical check 计数大于 0，P0 运行态迁移核验必须继续 BLOCKED。

## P0-T00C Runtime Migration Apply Preflight

## Data

- Data change goal：新增只读迁移应用前置检查，判断正式 P0 SQL 在当前运行库上是否会因历史 backfill、NOT NULL 或唯一键问题被阻塞。
- Affected script：`IntRuoyiBackend/script/p0/verify_p0_runtime_migration_apply_preflight.py`。
- Scope：只读连接运行库；不执行 ALTER、UPDATE、DELETE、backfill 或迁移 SQL。

## BDD:

- Given P0 正式 SQL 会新增 `production_submit_event_id`、`event_idempotency_key`、`recordbook_entry_id` 和复核签名字段。
- When 本机运行库尚未应用 P0 schema，且历史 PQC、生产提交或数量片段可能缺正式来源。
- Then apply-preflight 必须在迁移前输出机器可读 blocker，列明需要正式 backfill 或重复清理的行数；不得用默认值、rawPayload 或直接执行迁移试错来掩盖风险。

## RED:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增 `test_p0_runtime_migration_apply_preflight_contract` 后缺 `verify_p0_runtime_migration_apply_preflight.py`。

## GREEN:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`。
- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py --print-contract` -> PASS，输出 required env、preflight checks 和 formal columns。
- `python -X utf8 -m py_compile IntRuoyiBackend\script\p0\verify_p0_runtime_migration.py IntRuoyiBackend\script\p0\verify_p0_runtime_migration_apply_preflight.py IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS。

## Runtime Apply Preflight

- local-config apply-preflight using `application-local.yaml` datasource -> BLOCKED，已只读连接 `127.0.0.1:23306/ruoyi-vue-pro`。
- `P0_RUNTIME_APPLY_PREFLIGHT_PQC_BACKFILL_REQUIRED`：`mes_pro_process_pool_pqc_record.production_submit_event_id` 需正式 backfill 行数为 77。
- `P0_RUNTIME_APPLY_PREFLIGHT_EVENT_IDEMPOTENCY_BACKFILL_REQUIRED`：`mes_pro_process_pool_event.event_idempotency_key` 需正式 backfill 的生产提交行数为 2。
- `P0_RUNTIME_APPLY_PREFLIGHT_RECORDBOOK_BACKFILL_REQUIRED`：`mes_pro_process_pool_event.recordbook_entry_id` 需正式 backfill 的生产提交行数为 2。
- `P0_RUNTIME_APPLY_PREFLIGHT_FRAGMENT_ROOT_BACKFILL_REQUIRED`：`mes_pro_process_pool_quantity_fragment.production_submit_event_id` 需正式根事件 backfill 的片段数为 5。

## Blockers

- 当前运行库不能直接执行 P0 正式迁移；迁移会被正式 backfill/NOT NULL 前置阻塞。
- 需要先制定并授权正式 backfill/历史清理方案，再复跑 apply-preflight、runtime migration verifier 和真实 E2E。

## P0-T00D Runtime Backfill Source Audit

## Data

- Data change goal：新增只读正式来源审计，判断运行库历史 blocker 是否能从正式结构化来源安全推导，而不是只知道迁移会被 NOT NULL / 唯一键阻塞。
- Affected script：`IntRuoyiBackend/script/p0/verify_p0_runtime_backfill_sources.py`。
- Scope：只读连接运行库；只查询工序池事件、PQC 记录、数量片段、正式记录本 entry/event 和 `information_schema`；不执行 schema 变更、回填、删除或修复。

## BDD:

- Given apply-preflight 已发现 PQC、生产提交幂等键、记录本条目和数量片段根事件存在历史 blocker。
- When 准备制定运行库迁移 backfill 方案。
- Then 必须先用只读 backfill source audit 逐项判断是否存在唯一、正式、结构化来源；无法唯一推导时必须 BLOCKED，不得读取 payload、填默认值或人工猜测。

## RED:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增 `test_p0_runtime_backfill_source_audit_contract` 后缺 `verify_p0_runtime_backfill_sources.py`。

## GREEN:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`。
- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py --print-contract` -> PASS，输出 required env、sourceChecks 和 formalSources。
- `python -X utf8 -m py_compile IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_sources.py IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS。

## Runtime Source Audit

- local-config backfill source audit using `application-local.yaml` master datasource -> BLOCKED，已只读连接 `127.0.0.1:23306/ruoyi-vue-pro`。
- `P0_RUNTIME_BACKFILL_PQC_SOURCE_UNDERIVABLE`：PQC 记录无法唯一绑定正式 `PRODUCTION_SUBMIT` 事件 78 行。
- `P0_RUNTIME_BACKFILL_EVENT_IDEMPOTENCY_SOURCE_UNDERIVABLE`：生产提交事件幂等键无法从唯一正式记录本来源推导 2 行。
- `P0_RUNTIME_BACKFILL_RECORDBOOK_ENTRY_SOURCE_UNDERIVABLE`：生产提交事件无法绑定唯一正式记录本 entry 2 行。
- `P0_RUNTIME_BACKFILL_FRAGMENT_ROOT_SOURCE_UNDERIVABLE`：数量片段无法绑定现有正式 `PRODUCTION_SUBMIT` 根事件 5 行。
- 同一 master datasource 复跑 apply-preflight -> BLOCKED；当前 blocker 计数为 PQC 78 行、生产提交幂等键 2 行、记录本 entry 2 行、数量片段根事件 5 行。

## Blockers

- 当前运行库历史数据缺少正式记录本 entry/event、PQC 到生产提交唯一结构化关系和数量片段根事件来源；不能直接执行 P0 SQL 或做默认回填。
- 需要业务确认正式历史数据修复/重建方案并明确写库授权后，才能生成或执行 backfill；修复后必须复跑 backfill source audit、apply-preflight、runtime migration verifier 和真实 E2E。

## P0-T00E Runtime Backfill Repair Plan Gate

## Data

- Data change goal：新增只读修复授权方案门禁，将 apply-preflight/source audit 的历史 blocker 转换为正式修复前置要求，明确可接受来源、授权、备份、回滚和修复后复验顺序。
- Affected script：`IntRuoyiBackend/script/p0/verify_p0_runtime_backfill_repair_plan.py`。
- Scope：只读连接运行库；调用 apply-preflight 和 source audit 的只读查询；不生成、不执行、不建议任何写库 SQL。

## BDD:

- Given 运行库 P0 历史数据存在 backfill blocker 且 source audit 无法唯一推导正式来源。
- When 准备进入历史数据修复或迁移应用讨论。
- Then repair plan gate 必须 BLOCKED，并输出正式来源、业务/DBA 授权、备份、回滚、dry-run 和修复后复验链路；不得把默认值、payload、人工猜测或脚本写库作为可执行方案。

## RED:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增 `test_p0_runtime_backfill_repair_plan_contract` 后缺 `verify_p0_runtime_backfill_repair_plan.py`。

## GREEN:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`。
- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py --print-contract` -> PASS，输出 repair plan checks、正式来源、授权要求、回滚要求和复验脚本链路。
- `python -X utf8 -m py_compile IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_plan.py IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS。

## Runtime Repair Plan

- local-config repair plan using `application-local.yaml` master datasource -> BLOCKED，已只读连接 `127.0.0.1:23306/ruoyi-vue-pro`，未输出数据库用户名或密码。
- `P0_RUNTIME_BACKFILL_REPAIR_PLAN_BLOCKED`：修复方案不可执行，直到正式来源、授权、备份和回滚证据齐备。
- `P0_RUNTIME_BACKFILL_REPAIR_NO_DB_WRITE`：该门禁只读，禁止执行 schema 变更、DML、默认填充或合成修复。
- `P0_RUNTIME_BACKFILL_REPAIR_AUTHORIZATION_REQUIRED`：当前迁移 blocker 合计 88 行，需要业务 owner 和 DBA 授权。
- `P0_RUNTIME_BACKFILL_REPAIR_UNDERIVABLE_SOURCE`：当前 source blocker 合计 88 行，无法从唯一正式结构化来源直接推导。
- 最新明细：PQC 无唯一正式生产提交来源 79 行；生产提交幂等键无正式记录本来源 2 行；生产提交 recordbook entry 无正式来源 2 行；数量片段无现有生产提交根事件 5 行。

## Required Repair Authorization Package

- 精确租户和行级 manifest：记录目标表、主键、旧值、新值、正式来源引用、reviewer 和 dry-run 行数。
- 业务 owner 授权：确认历史数据重建口径、来源系统和责任人。
- DBA 授权：确认运行库、维护窗口、备份、回滚和执行人。
- 任务范围备份：导出所有目标表/主键，并记录 restore 命令和 checksum。
- 可逆回滚脚本：必须按同一 manifest 恢复旧值，并在回滚后逐列只读复验。

## Post Repair Verification

- `verify_p0_runtime_backfill_sources.py` 必须 PASS。
- `verify_p0_runtime_migration_apply_preflight.py` 必须 PASS。
- `verify_p0_runtime_migration.py` 必须 PASS。
- `p0-production-execution-loop-real.e2e.js` 必须真实页面 PASS。
- `verify_p0_completion_gate.py` 必须 PASS 后才允许进入 closeout。

## Blockers

- 当前没有用户授权的写库修复范围、业务 owner 签名、DBA 窗口、行级 manifest、备份和回滚证据；不得执行任何运行库修复。
- 运行库数据已从上一轮 PQC 78 行变化为 79 行；后续必须在同一维护窗口开始前重新运行三个只读门禁并以最新输出为准。

## P0-T00F Runtime Backfill Repair Manifest Gate

## Data

- Data change goal：新增只读行级修复 manifest 校验门禁，确保任何运行库历史修复在写库前都有业务/DBA 授权、备份、回滚、dry-run 和逐行正式来源证据。
- Affected script：`IntRuoyiBackend/script/p0/verify_p0_runtime_backfill_repair_manifest.py`。
- Scope：只读取 manifest JSON；不连接运行库、不执行 schema 变更、不执行 DML、不生成修复 SQL。

## BDD:

- Given repair plan gate 已要求正式行级 manifest、备份、回滚和 dry-run 证据。
- When 用户或 DBA 准备提交 P0 运行态历史修复包。
- Then manifest gate 必须先校验 authorization、backupEvidence、rollbackEvidence、dryRun 和 entries；缺 manifest 或任一必需字段必须 BLOCKED；只有结构完整、目标字段受限且 formal source 类型受控的 fixture 才能 PASS；PASS 也只代表 manifest 结构合格，不代表真实修复已执行。

## RED:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> FAIL，新增 `test_p0_runtime_backfill_repair_manifest_contract` 后缺 `verify_p0_runtime_backfill_repair_manifest.py`。

## GREEN:

- `python -X utf8 IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS，`PASS: MES process pool SQL contract`。
- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py --print-contract` -> PASS，输出 `repairManifestSchema`、允许目标字段、允许正式来源类型和 `databaseWriteAllowed=false`。
- `python -X utf8 -m py_compile IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py IntRuoyiBackend\script\tests\test_mes_process_pool_sql.py` -> PASS。

## Manifest Verification

- `python -X utf8 IntRuoyiBackend\script\p0\verify_p0_runtime_backfill_repair_manifest.py` -> BLOCKED/exit 2，输出 `P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_MISSING` 和 `P0_RUNTIME_BACKFILL_REPAIR_MANIFEST_NO_DB_WRITE`。
- 临时英文 JSON fixture（1 行 `mes_pro_process_pool_event.recordbook_entry_id`，`oldValue=null`，formal source 为 `MES_PRO_EDHR_RECORDBOOK_ENTRY`）-> PASS/exit 0；fixture 运行后已删除临时文件。
- 旧值 `oldValue=null` 被允许，但字段本身必须存在；`newValue`、row identity、formal source、reason 和 reviewer 不得为空。

## Required Manifest Shape

- `authorization`：`businessApprovalId`、`businessOwner`、`dbaApprovalId`、`dbaOwner`、`maintenanceWindow`。
- `backupEvidence`：`backupId`、`backupLocation`、`checksum`、`capturedAt`。
- `rollbackEvidence`：`rollbackPlanId`、`restoreCommandReference`、`verificationQueryReference`。
- `dryRun`：`targetRowCount` 和 `manifestEntryCount` 必须等于 entries 数量。
- `entries[]`：`tenantId`、`table`、`primaryKey`、`targetColumn`、`oldValue`、`newValue`、`formalSourceType`、`formalSourceId`、`reason`、`reviewer`。
- 允许目标字段仅限 P0 backfill scope：PQC `production_submit_event_id`、process pool event `event_idempotency_key` / `recordbook_entry_id`、quantity fragment `production_submit_event_id`。

## Blockers

- 当前没有真实授权 manifest；缺 manifest 时门禁保持 BLOCKED。
- fixture PASS 不得作为运行库修复授权或完成证据；真实修复前仍需最新 source audit、apply-preflight、repair plan、manifest gate、备份和回滚证据全部齐备。
