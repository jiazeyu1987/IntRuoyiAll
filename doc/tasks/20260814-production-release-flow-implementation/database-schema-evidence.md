# Database Schema Evidence

## Data Goal

为生产放行申请增加 PQC 决策、报告快照和唯一乐观锁版本；为 eDHR 工作待办增加仅约束 `PQC_PRODUCTION_RELEASE` 申请作用域的生成列，保证一个申请只有一个有效 PQC 待办。

受影响实体：`mes_pro_process_pool_active_order_release_application`、`mes_pro_edhr_work_task`。

## Migration

- 数据库引擎：MySQL 8，项目 SQL 迁移文件按日期顺序执行。
- 迁移文件：`IntRuoyiBackend/sql/mysql/20260814_mes_production_release_flow.sql`。
- 变更：申请表新增 PQC 决策字段、报告快照、`version` 和两个唯一索引；工作待办 `batch_execution_id` 改为可空，新增 PQC 申请作用域存储生成列及唯一索引。
- 不自动迁移旧状态，不自动认领或删除既有批次执行。

## Safety

- 结构变更前检查旧申请状态、旧批次执行、PQC 待办作用域和重复关系。
- 命中无法正式证明的数据时使用 `SIGNAL SQLSTATE '45000'` 阻塞。
- 所有列和索引重复执行时校验定义；定义漂移直接失败。
- 本任务不连接或修改真实业务数据库，只执行静态合同和 Java 映射验证。

## Rollback

上线前可在确认没有目标流程数据后，按反向顺序删除本迁移新增的唯一索引、生成列和申请字段，并恢复工作待办 `batch_execution_id` 原定义。产生目标业务数据后禁止破坏性回滚，必须使用备份恢复或经批准的前向修复迁移。

## BDD Scenarios

- BDD: 旧申请不允许自动推断 -> Given 存在 `BLOCKED` 或 `PENDING_RELEASE_APPROVAL` 申请 / When 执行 MIG-RF-1 / Then 迁移明确失败并返回 `LEGACY_RELEASE_APPLICATION_MIGRATION_REQUIRED`。
- BDD: 申请与 PQC 待办唯一 -> Given 同租户同一申请 / When 创建 PQC 放行待办 / Then 生成列唯一索引只允许一个有效 `PQC_PRODUCTION_RELEASE` 待办。
- BDD: 申请状态并发更新 -> Given 相同申请版本和状态 / When 两个请求竞争更新 / Then 只有一个 CAS 成功并把版本加一。

## TDD Evidence

- RED: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 MIG-RF-1、DO 字段、CAS Mapper 和共享合同。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesProductionReleaseRequiredCandidateResolverTest,MesReleaseFlowCoreContractTest,MesReleaseFlowSchemaContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS；15 tests，0 failures，0 errors，其中 schema 合同 2/2 PASS。
- GREEN: 迁移策略依赖闭包门禁 -> PASS；从 MIG-RF-1 递归包含 10 个正式前置迁移，共 11 个迁移，metadata、依赖环境和 checksum 合同通过，证据见 `migration-policy-gate.json`。
- BASELINE BLOCKER: 全 SQL 目录迁移门禁在无关既有文件 `20260814_mes_batch_record_repeat_row_group.sql` 处失败，原因为缺少 release-migration metadata；本任务未修改该文件，失败证据见 `migration-policy-gate-full-baseline-failure.json`。

## Verification

schema/core/role 定向 Maven、角色 SQL 静态合同和 MIG-RF-1 完整依赖闭包门禁已通过；不执行未获授权的真实数据库迁移。提交前继续执行 `git diff --check`、暂存区精确清单和本证据校验器。

## Blockers

本任务迁移依赖闭包无前置阻塞；全目录门禁仍受无关既有 SQL 元数据缺口阻塞。真实数据库迁移执行和回滚演练留到获授权的发布阶段。
