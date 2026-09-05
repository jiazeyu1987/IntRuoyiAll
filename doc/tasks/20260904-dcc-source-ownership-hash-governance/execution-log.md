# 执行记录

## Supervisor Initialization

- task_id：`20260904-dcc-source-ownership-hash-governance`
- 用户授权：由主 Agent 启动子 Agent，分阶段执行并由主 Agent 负责 review 和推进。
- 当前阶段：planning。
- 已知起点：测试库只读盘点显示 AUTO_MAP 为 0，18,065 条文件缺少 ownership/hash 证据，43 组共享源文件，7 条软删除源文件引用。
- 当前执行边界：先规划和只读核对，不执行数据库写入、DDL、服务重启、E2E 或 Git 提交。

## BDD Scenarios

- BDD: 源文件证据完整才能进入迁移候选 -> Given DCC 受控文件存在明确源文件归属和 SHA-256, When 重新计算迁移资格, Then 该文件才可进入确定性候选，否则进入明确 blocker。
- BDD: 共享或缺失源文件失败关闭 -> Given 源文件被多个受控文件共享、已删除或归属不确定, When 执行治理, Then 系统不得猜测补齐，必须保留 BLOCKED 原因和原始证据。
- BDD: 证据治理可复核 -> Given 治理完成并生成证据, When 独立 tester 按测试计划复核, Then ownership、hash、来源关系和 AUTO_MAP 结果可以由只读查询重现。

## Milestone Updates

- M1：in_progress。Planner 第一轮已产出 `request-analysis.md` 和 `prd.md`，主 Agent 评审为 needs_revision。

## Planner Review 1

- 结果：needs_revision。
- 已通过部分：目标、范围、零猜测、清单确认、漂移检测、幂等、postflight 和 AC-01 至 AC-20 的总体结构可测试；“共享组内全部有效引用生成独立副本”作为确定性规则予以接受。
- 缺陷 1：`infra_file`/`FileDO` 是 `@TenantIgnore` 全局文件表，没有 `tenant_id`；方案中的“源文件租户一致/源文件包含另一租户记录”不是可执行合同。
- 缺陷 2：现有代码只按当前 `tenant_id` 统计共享 source，而物理 `source_file_id` 可能被不同租户的有效 DCC 记录引用；仅做租户内分组会把跨租户共享误判为独占。
- 修订要求：把共享组定义为冻结任务范围内按全局 `source_file_id` 聚合；发现任务范围外租户引用时整组 BLOCKED，不得部分保留原源；明确全局查询的受控权限边界；调整原因码、场景、FR、AC、unknown、risk 和 blocker，删除不存在的 infra_file 租户校验。

## Planner Review 2

- 结果：approved。
- Planner 已按要求修订 `request-analysis.md` 与 `prd.md`：明确 `infra_file` 无 `tenant_id`；共享按全局 `source_file_id` 聚合；范围外有效引用整组 `SOURCE_GLOBAL_REFERENCE_OUT_OF_SCOPE`；受控全局核验不可用时 `SOURCE_GLOBAL_REFERENCE_CHECK_UNAVAILABLE`；不扩大普通业务租户可见性。
- AC-01 至 AC-20 编号稳定且可验证；UTF-8、章节、弱标记和 PRD 自检通过。
- M1：completed。进入 M2 拆解任务图与测试计划阶段。

## M2 Decomposition Handoff

- 已启动拆解 Agent，限定只修改 `dev-plan.md` 和 `test-plan.md`。
- 输入为已批准的 `request-analysis.md` 和 `prd.md`。
- 拆解要求：任务必须有稳定 ID、依赖图、明确写入范围、AC 映射、任务级验证和完成定义；测试计划必须覆盖只读盘点、schema/服务变更、全局跨租户 source 引用、共享复制、幂等/漂移、postflight 与回归。

## Decomposition Review 1

- 结果：needs_revision。
- 缺陷 1：部分 `affected_paths` 使用省略号或泛化目录，无法作为执行者边界。
- 缺陷 2：系统级测试用 `AC-01 至 AC-20` 作为映射值，不符合逐项稳定验收 ID 合同。
- 修订要求：改为真实文件/模块路径，并逐项列出 AC-01 至 AC-20；保留现有任务依赖图，不扩大到 Revision/Iteration 实施。

## Decomposition Review 2

- 结果：approved。
- 已将省略路径改为实际模块/文件边界，并将 TP-09 和 T6 的验收映射展开为 AC-01 至 AC-20 独立 ID。
- 依赖图为 T1 -> T2 -> T3 -> T4 -> T5 -> T6；未发现并行写范围安全条件。
- M2：completed；T1 已标记 ready，后续任务保持 pending。

## T1 Execution Pass 1

- 状态：in_progress。
- 执行边界：只读仓库/schema/Mapper 合同和数据库盘点设计；不执行数据库写入、DDL、对象存储复制、服务重启、E2E 或 Git。
- 目标：核对当前 `infra_file` 全局表事实，补齐全局 `source_file_id` 引用盘点所需的查询合同，并将有效记录、软删除、缺失定位、正文不可读和范围外引用分类为可验证结果。

## T1 Execution Result

- 状态：completed，主 Agent review：approved。
- 生产改动：`DccControlledFileMapper` 新增 `selectEffectiveUnownedSourceReferences(tenantId, snapshotMaxControlledFileId, limit)`，统一限定 `deleted = 0` 与冻结 ID；新增 `@TenantIgnore selectGlobalEffectiveSourceReferences(sourceFileId, snapshotMaxControlledFileId)`，按全局 source_file_id 读取有效受控引用。
- 兼容边界：未修改现有 `selectUnownedSourceReferences`、旧 readiness/count 查询及历史迁移语义；未改 original/published/stamped、审批、签名或平台生命周期。
- BDD: 冻结边界与全局 source 引用 -> Given 有效受控记录和跨租户全局 source 引用, When 查询治理候选, Then 仅返回 deleted=0 且不超过冻结 ID 的记录，并可识别范围外租户引用。
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileMapperTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL, 新增测试引用的两个 Mapper 方法和 `GlobalSourceReference` 尚不存在。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileMapperTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS, 11 tests, 0 failures。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileMapperTest,DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS, 12 tests, 0 failures。
- GREEN: 测试库只读复核 -> PASS，`transaction_read_only=1`；有效记录 18,072；全局跨租户共享源 2 组、26 条有效引用；未执行写入。
- 覆盖验收：AC-01、AC-04、AC-05、AC-06、AC-08、AC-14、AC-19、AC-20。
- 已知限制：查询合同已具备全局读取能力，但治理清单、稳定 blocker 原因、幂等键和写批次门禁尚未实现，由 T2/T3 处理。

## T2 Schema / DO / Mapper Execution

- 状态：completed，等待主 Agent review；本轮未执行 DDL、数据库写入、对象存储复制、服务重启、E2E 或 Git。
- 复核范围：`20260905_dcc_source_governance.sql`、治理批次/明细 DO、治理批次/明细 Mapper 和 schema 合同测试。
- 复核确认：治理批次表是跨租户清单表且没有 `tenant_id`，`DccControlledFileSourceGovernanceBatchDO` 已使用 `@TenantIgnore`，避免通用 MyBatis CRUD 被租户插件追加不存在的列条件；治理明细批量查询已使用 `selectByBatchAndTenant(batchId, tenantId)` 并显式限定租户，防止跨租户明细误读。
- BDD: 治理清单租户与幂等合同 -> Given 批次清单可包含多个授权租户且 task_key 必须全局唯一, When 读写批次或查询明细, Then 批次 CRUD 忽略租户插件、task_key 查询全局且保留 request_sha256 供同键冲突判断、明细查询必须显式带 tenant_id。
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，新增合同测试先验证 `@TenantIgnore`、显式租户批次查询及状态计数字段，基线实现尚未满足。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，4 tests, 0 failures；模块主代码和测试代码均以 Java 17 编译通过。
- Schema 合同覆盖：AC-07、AC-09、AC-12、AC-13、AC-17、AC-19、AC-20 所需的 blocker/action/status、manifest/rule/schema/request 摘要、全局 task_key 唯一、完成/阻塞/失败计数、处理人/时间和明细唯一键字段均存在。
- 租户边界结论：批次表使用 DO 级 `@TenantIgnore`；明细表保留 `tenant_id` 并由 Mapper 显式限定；`selectByTaskKey` 仅按全局 task_key 读取未删除批次，服务层必须比较 `request_sha256`，同键不同摘要应冲突。
- 未解决产品实现边界：清单确认服务、同键冲突业务判断、BLOCKED 不入批和续跑状态机仍由 T3/T4 实现；本 T2 仅提供可持久化合同，不宣称写批次已可运行。
- 主 Agent 复核补充：新增全局 `dcc_controlled_file_source_global_claim` 表，使用全局 `source_file_id` 唯一键防止跨租户重复认领；T3 必须补齐对应 DO/Mapper/服务使用，不得只建表不接入。

## T2 Review Result

- 状态：completed，主 Agent review：approved。
- `DccSourceOwnershipSchemaTest`：4 tests, 0 failures。
- `database-schema-evidence.md` validator：PASS；已补充字面 `RED:`/`GREEN:` 证据标记。
- T2 不执行 DDL；测试库未创建新表，T3 仍需在未应用 schema 的状态下先完成单测和服务合同。
- T3 已标记 ready；全局 claim 表的 DO/Mapper/服务接入是 T3 的强制完成项。

## T3 Execution Pass 1

- 状态：in_progress。
- 目标：将全局 source claim 接入历史迁移提交事务，先于 source 指针更新和租户级 ownership 写入执行。
- BDD: 跨租户全局 claim -> Given 全局 source_file_id 已被其它租户受控记录 claim, When 当前记录尝试认领该源, Then 立即返回 source ownership conflict，不写指针或 ownership。
- RED: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileSourceMigrationCommitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，`DccControlledFileSourceGlobalClaimService` 不存在。
- GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGlobalClaimServiceTest,DccControlledFileSourceMigrationCommitServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，5 tests, 0 failures。
- 覆盖验收：AC-03、AC-08、AC-14、AC-16 的全局 claim 子边界。
- 当前限制：READY/BLOCKED 清单消费、全局引用范围外整组阻塞、清单幂等和共享组全部复制仍未完成；现有 `migrateBatch` 旧入口不能作为正式治理入口，由后续 T3/T4 收口。

## T3 Review Checkpoint

- 全局 claim 子任务：主 Agent review approved；服务单测 3/3，提交事务单测 2/2。
- 全局 claim 迁移表仍未在测试库执行；当前运行态旧迁移入口必须等待治理清单门禁完成，不能直接用于全量历史治理。

## T3 Execution Pass 2

- 状态：in_progress。
- 新增 `DccControlledFileSourceGovernanceClassifier` 与 `DccControlledFileSourceGovernanceDecision`，将 source 引用缺失、文件不可用、全局引用越界、当前记录不在全局索引、SHA-256 异常和 ownership 指针/hash 不一致映射为稳定 `BLOCKED` 原因；有效单源返回 `CLAIM_SOURCE`，有效共享源返回 `COPY_SHARED_SOURCE`。
- BDD: READY/BLOCKED 稳定分类 -> Given 全局引用、正文可读性、hash 和 ownership 证据输入, When 计算治理决定, Then 只在所有证据满足时返回 READY，否则返回稳定 blocker，不根据弱字段猜测。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceClassifierTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，分类器尚不存在。
- GREEN: 同命令 -> PASS，6 tests, 0 failures。
- 回归 GREEN: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceClassifierTest,DccControlledFileSourceGlobalClaimServiceTest,DccControlledFileSourceMigrationCommitServiceTest,DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceOwnershipServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，18 tests, 0 failures。
- 覆盖验收：AC-02、AC-03、AC-04、AC-05、AC-06、AC-07、AC-08、AC-10、AC-14、AC-15、AC-16、AC-20 的分类/全局 claim 子边界。
- 主 Agent review：分类顺序已明确为先证据存在性、再全局引用范围、再当前记录索引、再 hash、再 ownership；未引入 fallback。
- 未完成：分类器尚未由治理批次服务消费，旧 `migrateBatch` 入口仍未被清单确认门禁替代；T3/T4 继续处理。

## T3 Execution Pass 3

- 新增 `DccControlledFileSourceGovernanceManifestService`，提供 `CONFIRMED + manifest_sha256 + request_sha256` 门禁、READY 明细门禁和 COMPLETED 幂等识别。
- 新增治理清单无效、治理明细阻塞错误码；未改变旧 DCC 上传/发布错误语义。
- BDD: 清单和明细门禁 -> Given 批次未确认、摘要不一致或明细为 BLOCKED, When 请求治理执行, Then fail-fast，不复制文件、不切换指针；COMPLETED 明细可识别为幂等结果。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceManifestServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，门禁服务不存在。
- GREEN: 同命令 -> PASS，3 tests, 0 failures。
- 回归 GREEN: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceClassifierTest,DccControlledFileSourceGovernanceManifestServiceTest,DccControlledFileSourceGlobalClaimServiceTest,DccControlledFileSourceMigrationCommitServiceTest,DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceOwnershipServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，21 tests, 0 failures。
- 主 Agent review：T3 当前完成全局 claim、稳定分类器和清单/明细门禁纯规则；尚未把新清单读取接到旧 `migrateBatch`，也未实现共享组批量全复制。

## T3 Execution Pass 4 / T4-T5 Slice

- 新增 `DccControlledFileSourceGovernanceBatchService`：确认 `PREPARED/READY` 批次为 `CONFIRMED`，执行时重新校验任务摘要、当前租户是否在冻结范围，并且只消费 `READY` 明细；按明细状态刷新批次完成/阻塞/失败计数，支持同一批次续跑。
- 新增治理接口：`POST /source-governance/batches/{taskKey}/confirm`、`POST /source-governance/batches/{taskKey}/execute`、`GET /source-governance/batches/{taskKey}/blockers`。
- 新增只读 postflight：`GET /source-governance/batches/{taskKey}/postflight`，复核完成项的 source 指针、ownership 关系和当前 SHA-256，不写业务数据。
- BDD: 已确认批次才可执行 -> Given 批次摘要精确匹配且当前租户在冻结范围, When 执行批次, Then 只处理 READY 明细并刷新可对账状态。
- BDD: 完成证据只读复核 -> Given 明细状态为 COMPLETED, When 执行 postflight, Then 重新读取受控文件指针、ownership 和正文 hash，任何差异只进入报告。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceBatchServiceTest,DccControlledFileSourceGovernancePostflightServiceTest,DccControlledFileSourceGovernanceApiContractTest,DccControlledFileSourceGovernanceManifestServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，租户范围负向测试初始错误地 mock 掉了真实 Manifest 门禁，未能证明异常路径。
- GREEN: 同命令 -> PASS，9 tests, 0 failures。
- GREEN: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，6 tests, 0 failures。
- 主 Agent review：新批次服务已成为执行服务生产调用方；新接口不调用旧无清单迁移路径；postflight 保持只读。T3/T4/T5 仍需独立 tester 做全量回归和确认共享组失败补偿边界。

## T6 Independent Verification / Corrective Loop

- T6 独立复测第一次结论：NO-GO。48 个定向测试通过，但 AC-03、AC-09、AC-11、AC-15、AC-16、AC-18、AC-19、AC-20 的真实证据不足；报告保存在 `test-report.md`。
- 纠正代码缺口：旧 `POST /source-ownership-migration/run` 明确抛出 `CONTROLLED_FILE_SOURCE_GOVERNANCE_LEGACY_ENTRY_DISABLED`，不能绕过确认清单；批次/单项执行增加固定 `ruleVersion`/`schemaVersion` 门禁；增加共享组级执行包装和失败副本清理；postflight 增加“完成后 source 仍共享”与历史非 source 证据 hash 漂移检查。
- BDD: 共享组原子执行 -> Given 同一 `shared_group_key` 的三条 READY 明细, When 任一明细阻塞或执行异常, Then 组级执行抛出失败并清理本次新副本，不返回部分成功。
- BDD: 历史证据保护 -> Given 完成明细保存了非 source 生命周期字段摘要, When postflight 重新读取受控文件, Then 摘要漂移进入 blocker 报告，不修改历史字段。
- RED: 首次复测报告 -> FAIL/NO-GO，独立 tester 明确指出旧入口、共享组原子性、postflight 共享检查和历史证据对账缺口。
- GREEN: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceExecutionServiceTest,DccControlledFileSourceGovernanceBatchServiceTest,DccControlledFileSourceGovernancePostflightServiceTest,DccControlledFileSourceGovernanceManifestServiceTest,DccControlledFileSourceGovernanceApiContractTest,DccSourceOwnershipSchemaTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，25 tests, 0 failures。
- 当前剩余门禁：真实 MySQL 新治理表/唯一索引、对象存储复制与清理、历史非 source 前后快照、最新冻结边界 Windchill 全量只读盘点仍未执行；原因是本轮未获得数据库写入、对象存储写入和完整盘点重跑授权，不能用单测替代。

## Corrective Review Pass 2

- 修正共享组执行：组开始时冻结全局引用集合，核对三条清单明细完整且同租户；组内逐条创建不同 verified copy 并提交，任一条失败清理本组新副本并抛出异常；批大小不足以容纳完整组时返回明确 `CONTROLLED_FILE_SOURCE_GOVERNANCE_BATCH_SIZE_SPLITS_GROUP`，不再静默跳过。
- 新增三条共享明细全部完成测试、组内失败清理测试和批大小拆组拒绝测试。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，初始失败测试未配置 commit stub，Mockito 严格模式先报告参数不匹配。
- GREEN: 同命令 -> PASS，9 tests, 0 failures。
- GREEN: 真实测试库只读盘点 `windchill-readonly-inventory.sql` -> PASS，`transaction_read_only=1`；输出已归档至 `windchill-inventory-report.md`，AUTO_MAP_CURRENT=0，GOVERNANCE_SCHEMA_PRESENT=0。
- 真实盘点细分：有效 Master 18,222；有效 controlled file/source 引用 18,072；ownership 缺失 18,065；软删除 source 7；全局共享 42 组/290 引用；跨租户共享 2 组/26 引用；路线快照孤儿 24；访问日志孤儿 685；平台 ACTIVE 漂移 17,864。
- T6 第二轮独立复测：54 tests, 0 failures，两个 evidence validator 和 `git diff --check` 通过；代码性缺口已复核通过，但 AC-11 真实 MySQL/对象存储事务、AC-15 完整关联历史前后快照仍为 BLOCKED。

## T6 Independent Verification Pass 3

- 独立 tester 复测最终定向回归：12 个测试类、57 tests、0 failures/errors；schema/backend evidence validators 与 `git diff --check` 均通过。
- 独立 tester 确认 AC-03、AC-09、AC-16 的代码合同已放行：共享组冻结引用、三条 distinct copy、失败清理、旧入口拒绝、rule/schema 版本门禁和完成后共享检查均有证据。
- 独立结论仍为 NO-GO：AC-11 真实 MySQL/对象存储事务，AC-15 完整关联历史前后快照，AC-18/19/20 正式 schema 部署后的 postflight 与同口径最终重跑仍缺少授权和运行态证据。
- 详细独立矩阵与 blocker 见 `test-report.md`；最终主 Agent 审计见 `verification-report.md`。

## Final Main-Agent Audit

- 最终回归：`mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileMapperTest,DccSourceOwnershipSchemaTest,DccControlledFileSourceOwnershipServiceTest,DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceMigrationCommitServiceTest,DccControlledFileSourceGlobalClaimServiceTest,DccControlledFileSourceGovernanceClassifierTest,DccControlledFileSourceGovernanceManifestServiceTest,DccControlledFileSourceGovernanceExecutionServiceTest,DccControlledFileSourceGovernanceBatchServiceTest,DccControlledFileSourceGovernancePostflightServiceTest,DccControlledFileSourceGovernanceApiContractTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> PASS，58 tests, 0 failures/errors（T6 第四轮独立复测）。
- 最终证据 validator：database schema、backend API、`git diff --check` 均 PASS。
- 任务结论保持 `in_progress / NO-GO`，不是 `completed`：真实治理表未部署，测试库未写入，未执行对象存储副本实际复制/清理，未完成全历史关联前后快照和正式治理后的 postflight 重跑。
- 未执行 Git 提交/推送、远程发布、服务重启、E2E；工作区保留其他并行任务改动。
- 授权运行态更新：已执行本机测试库 additive DDL，三张治理表 InnoDB/唯一索引核验通过；备份文件 27,918,348 bytes，SHA-256 `7F02AA4828DB8CE944BED4D047BF32FE628475F99580A3A4FCB1979AA6C83CFC`。
- 真实独占 smoke：首次 `CLAIM_SOURCE` 完成，postflight 1/1；同摘要重试 `processedCount=0`。第一次 shared-copy smoke 发现同名物理 key 冲突并阻断，修复为 source/id/UUID 独立目录后重新执行三条 shared group，得到 3 个不同 object key、3/3 COMPLETED、postflight 3/3，所有对象和测试行随后清理为 0。
- 真实失败/篡改：全局 claim 唯一键冲突使事务退出码 1 且批次插入回滚；篡改清单快照后原摘要确认返回 `1080000307`；历史不可读源生成清单并执行为明确 `SOURCE_CONTENT_UNREADABLE` BLOCKED。
- 真实历史保护：隔离恢复库 11 张表 checksum 对账通过；历史 access log 行缺失/变更为 0，仅新增本次直链拒绝审计；隔离库已删除。
- 最终盘点：2026-09-05 18:33:03，`transaction_read_only=1`，schema=3、治理三表空、AUTO_MAP=0、业务 blocker 与前次冻结一致。

## T6 Independent Verification Pass 4

- 独立 tester 针对最后一项代码变更复测：`COPY_SHARED_SOURCE` 单条明细也强制走组级冻结引用检查。
- 定向回归：12 个测试类、58 tests、0 failures/errors；schema/backend evidence validators 与 `git diff --check` PASS。
- AC-03、AC-09、AC-16 代码合同继续 PASS；AC-11、AC-15、AC-18、AC-19、AC-20 继续 BLOCKED，原因仍是未授权真实 MySQL/对象存储写入、完整历史关联前后快照和正式治理后 Windchill 重跑。
- 独立结论：NO-GO；不得创建或回填 Revision/Iteration。

## Authorized Runtime Validation Pass

- 已授权执行本机测试环境 DDL、对象存储 smoke、任务专属数据库写入和本地 backend 重启；未访问远程服务器。
- 首次真实 shared-copy 发现固定同名对象 key 会物理覆盖；修复后每次副本目录使用 `source_file_id/UUID/date`，三条副本对象 key、infra_file ID、ownership/global claim 均唯一，字节和 SHA-256 一致。
- 真实 prepare/confirm/execute：历史不可读源生成 `SOURCE_CONTENT_UNREADABLE` BLOCKED；清单篡改后确认返回 `1080000307`；游标 `2054545668044052029 -> 2054545668044052031`；同摘要终态重试 `processedCount=0`。
- 真实共享执行：3 条 COMPLETED，3/3 postflight，副本 SHA-256 `cb3f40ac2ca8cebd85c5a895a64588e5b6298517a2061c8227c62e715a73a71f`；对象、测试 DCC 记录、治理批次和 claims 已全部清理。
- 真实事务失败：全局 claim 唯一键冲突退出码 1，事务内批次插入回滚为 0；相关表 checksum 与备份一致。
- 最终回归：DCC 定向测试 64 tests, 0 failures；database/backend/backup evidence validators PASS；最新只读盘点 schema=3、AUTO_MAP=0。

## T6 Independent Verification Pass 5

- 独立 tester 已复测新增清单生成、manifest 内容重算、游标、真实运行态 smoke 证据与最终 DCC 回归。
- 最终定向回归：新增 Preparation/Hasher 后共 64 tests, 0 failures/errors；schema、backend、backup evidence validators 均 PASS。
- AC-02、AC-03、AC-09、AC-11（小批真实范围）、AC-12、AC-13、AC-16 代码/运行态证据通过；全量历史治理仍受 `AUTO_MAP=0` 和 18,065 条 source ownership blocker 阻断。
- AC-15 全量关联快照、AC-18/19/20 全量治理后的最终清零仍未满足，不得宣称全部历史文件已治理。

## Authorized Full Batch Execution Pass

- BDD: 有游标的租户批次治理 -> Given 固定 `snapshot_max_controlled_file_id`、tenant scope 和 manifest/request 摘要, When 按受控文件 ID 游标执行 prepare/confirm/execute, Then 每条记录都有明确 `READY`/`BLOCKED`/`COMPLETED` 状态，游标尾部不重复且不遗漏。
- GREEN: tenant 1 在本机测试库执行 90 个批次（batchSize=200，含 1 个空尾批次），17,607 条明细中 17,600 条为 `BLOCKED`、7 条为 `COMPLETED`、0 条 `FAILED`；7 条完成项 postflight 7/7，无发现。
- GREEN: tenant 122 执行 3 个批次，465 条明细全部为 `BLOCKED`，0 条 `COMPLETED`、0 条 `FAILED`；最后游标 `2054545668044070295` 后剩余候选 0。
- GREEN: 两个租户批次均通过真实 API 的 prepare/confirm/execute，治理批次和明细审计记录保留；未执行远程服务器、E2E、Revision/Iteration 创建或回填。
- 只读重跑（2026-09-05 22:37:00）：`transaction_read_only=1`，有效 Master/file/source 为 18,222/18,072/18,072；ownership/hash blocker 18,065；软删除 source 7；全局共享 42 组/290 条、跨租户共享 2 组/26 条；平台 ACTIVE 漂移 17,864；路线快照孤儿 24；访问日志孤儿 685；AUTO_MAP=0；治理 schema=3。
- 结果：批次执行证实了游标、清单、阻塞和审计链路，但没有消除业务 blocker；全历史仍保持 `NO-GO`，不得进入 Revision/Iteration。

## T3 Corrective Review Pass 3

- 独立 reviewer 指出：执行服务可伪造 tenant scope、真实 `COPY_VERIFIED` 重试来源关系判断错误、批次外层事务可能让已创建对象与回滚数据库脱节、共享组失败留痕会随组事务回滚、清理首个失败会中断后续副本清理、Mapper 返回 0 行仍可能报告成功。
- BDD: 冻结租户范围不可扩展 -> Given batch 保存 `tenant_scope_json` 与 SHA-256, When 执行服务收到调用方传入的更宽 tenant set, Then 必须按持久化范围拒绝，不能只信调用参数。
- BDD: COPY_VERIFIED 可验证续跑 -> Given migration 保存旧源、独立副本、`COPY_VERIFIED` 和快照 hash, When 中断后重试, Then 重新读取副本正文并由 migration 恢复 origin；状态或摘要不一致则 BLOCKED。
- BDD: 共享组独立原子提交 -> Given 一个批次包含多个共享组, When 后续组技术失败, Then 之前已完成组保持已提交，失败组回滚并在新事务写入 FAILED，不留下无数据库记录的对象副本。
- BDD: 审计写入必须真实落库 -> Given 任一治理 insert/update 返回 0 行, When 服务准备返回结果, Then 明确抛错并由事务回滚，不能返回 COMPLETED/CONFIRMED。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceManifestServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，9 tests 中 2 failures：伪造 item tenant scope 和篡改 tenant scope hash 未被拒绝。
- RED: `mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileSourceGovernanceExecutionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=true" test` -> FAIL，新增 COPY_VERIFIED、已有 ownership 共享源、全副本清理和空组门禁暴露 4 个失败。
- RED: 6 个治理写入服务定向测试 -> FAIL，39 tests 中 6 failures：global claim、batch、item、prepare、migration、ownership 的 0 行写入均未 fail-fast。
- GREEN: manifest scope/hash 测试 9 tests，0 failures；execution/batch/manifest 30 tests，0 failures；6 个写入服务 39 tests，0 failures。
- GREEN: 最终 13 个 DCC 定向测试类共 78 tests，0 failures/errors；database/backend/backup evidence validators 和 `git diff --check` 全部 PASS。
- 代码结果：批次编排不再持有跨组外层事务；每组/单项独立事务；失败组用 `REQUIRES_NEW` 写入 FAILED；`COPY_VERIFIED` 只按完整迁移证据复用；已有 ownership 的共享源明确 BLOCKED；所有关键写入必须 exactly-one。
- 运行态 GREEN: 标准本地 backend 重建/重启成功，`/actuator/health` 为 UP；完成批次幂等重试 processed=0、postflight 7/7；篡改 task-owned 空批次的 tenant scope hash 返回 `1080000309`，smoke 批次已清理为 0。

## T3 Corrective Review Pass 4

- 最终独立 reviewer 复核 78 tests 后确认 P0 为 0，并指出两个 P1：调用范围虽包含 item tenant，但可宽于冻结范围；共享组业务 BLOCKED 在组回滚后被批次层统一改写为 FAILED。
- BDD: 调用范围必须等于冻结范围 -> Given batch 冻结范围为 `[31]`, When 直接调用执行服务并传入 `{31,32}`, Then 返回 scope invalid，不能让 tenant 32 的全局引用变成“范围内”。
- BDD: 共享组保留业务 blocker -> Given 组内任一条因 `SNAPSHOT_DRIFTED` 返回 BLOCKED, When 组事务为保持原子性回滚, Then 批次层以独立事务将整组写为同一稳定 BLOCKED 原因，不得改写成技术 FAILED。
- RED: manifest/execution/batch 三类测试在新异常和 `recordGroupBlocked` 尚不存在时 testCompile FAIL；这是预期 RED。
- GREEN: manifest 10 tests、execution 15 tests、batch 10 tests，共 35 tests，0 failures；最终 13 个 DCC 定向测试类共 81 tests，0 failures/errors。
- 代码结果：`tenantScope` 必须与解析并校验 SHA-256 后的冻结范围完全相等；组内业务 blocker 使用专用异常触发组回滚，批次层捕获后调用 `REQUIRES_NEW recordGroupBlocked`；技术异常仍走 `recordGroupFailure`。
- 运行环境：尝试用标准本地重启脚本部署 81-test 版本时，被无关并行 MES 文件 `MesProcessPoolTeamLeaderController.java:1293` 的语法错误阻断。脚本已先停止 48081，因此随后明确回滚启动上一份已验证的 `backend-runtime-control-20260905-231741.jar`，健康检查恢复 UP。该运行包包含 Pass 3 修正，但不包含本 Pass 4 两项修正；不得把当前 48081 当作 81-test 最新代码的运行态证据。
- 运行环境恢复：并行任务随后修复该 MES 语法错误，`mvn -o -pl yudao-module-mes -DskipTests compile` PASS；再次运行标准重启脚本，完整 30 模块 package PASS，并启动 `backend-runtime-control-20260906-000036.jar`。健康检查 UP；最新批次幂等执行 processed=0、7 条完成、postflight 7/7 无发现。
- 最新只读盘点（2026-09-06 00:02:45）：全部指标与 22:37 冻结一致，`transaction_read_only=1`、治理 schema=3、AUTO_MAP=0。
- 并行任务于 00:17 再次运行标准本地重启，48081 的短暂不可达是进程切换窗口，不是应用自行崩溃；当前运行包为 `backend-runtime-control-20260906-001702.jar`，健康检查重新为 UP。当前包再次执行同一完成批次，processed=0、completed=7、postflight 7/7 无发现。

## T6 Independent Verification Pass 7 / Main-Agent Acceptance

- 独立 tester 复核最新 81-test 版本并仅更新 `test-report.md`。
- GREEN: 13 个 DCC 定向测试类，`81 tests, 0 failures/errors`。
- GREEN: database schema、backend API、backup/disaster recovery 三类 evidence validator 及 `git diff --check` 全部 PASS。
- GREEN: 最新运行包 `backend-runtime-control-20260906-001702.jar` 健康 UP；同一已完成批次重试 `processedCount=0`、`completed=7`；postflight `7/7`，无 findings。
- GREEN: 2026-09-06 00:02:45 同口径只读盘点完成，`AUTO_MAP=0`、ownership/hash blocker 18,065、平台 ACTIVE 漂移 17,864、路线快照孤儿 24、访问日志孤儿 685。
- 主 Agent acceptance：T3、T4、T5、T6 开发验证完成；M4 完成，task 状态进入 `ready_for_closeout`。全量业务治理和 Revision/Iteration 仍保持 `NO-GO`，不能把本任务验收误报成历史迁移完成。

## M5 Closeout

- BDD: 任务收尾清理 -> Given T3-T6 已完成且 task 状态为 `ready_for_closeout`, When 执行 cleanup preview/apply, Then 仅删除任务临时 smoke/validator 产物，保留 task records、独立报告、验证报告、正式测试和只读盘点证据。
- GREEN: `task_closeout.py --task-id 20260904-dcc-source-ownership-hash-governance --mode preview` -> PASS，keep/delete/blocked/warnings 均符合预期，blocked/warnings 为 none。
- GREEN: `task_closeout.py --task-id 20260904-dcc-source-ownership-hash-governance --mode apply` -> PASS，删除一次性 smoke、临时 SQL/样本、临时 validator evidence，保留 12 个正式任务记录/证据文件；当前为主 worktree，无 merge/remove 操作。
- GREEN: 收尾后 task state 更新为 `completed`，M1-M5 全部完成；`test-report.md` 保留 T6 Pass 7 的 81-test 独立结论。
- 经验归档：DCC Windchill 历史迁移和共享源原子治理经验已合并到现有 `docs/backend-development.md` 与 `docs/experience-index.md`，未新建重复长期经验文档。
- Git 授权已获得；任务在 cleanup apply 后进入最终收尾提交，账号凭据文件保持未跟踪且已加入本机排除。

## Git Delivery Authorization

- 用户已授权对全部前后端代码执行 Git 提交/推送；账号凭据文件 `e2e_test/registration/测试账号.txt` 未纳入版本控制，并加入本机 `.git/info/exclude`。
- 基线提交 `280df8219`：15 个当前前后端/MES/前端静态测试及配套 E2E 规则文件。
- 后续并行前端基线提交 `3fa14e179`：`IntRuoyiFronted/tests/e2e/team-leader-latest-report-material-name-real.e2e.cjs`。
- 本任务实现提交 `537d58f4d`：`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSourceGovernanceExecutionService.java`。
- 收尾记录提交在 task state 改为 `completed` 后创建；随后推送 `int_main` 并验证与 `origin/int_main` 不再 ahead。

## Final Closeout Status

- task state 已更新为 `completed`，M1-M5 和 T1-T6 均完成。
- 全量历史业务门禁仍为 `NO-GO`，这是产品数据治理结论，不影响本任务代码/验证/收尾完成。

## Final Git Closeout

- 基线提交：`280df8219`、`3fa14e179`；DCC 实现提交：`537d58f4d`；DCC 任务收尾提交：`bd63af121`；并行文档基线提交：`d90529623`；并行文档补充提交：`ec0679a4a`。
- 用户授权后已成功推送 `int_main`：`8cbc9ce2b..d90529623`，随后 `d90529623..ec0679a4a`。
- 账号凭据文件 `e2e_test/registration/测试账号.txt` 未纳入版本控制，已保持本机排除。
