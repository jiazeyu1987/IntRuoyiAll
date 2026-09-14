# 独立测试报告

## Status

FAIL - T6 最终独立复测完成（Pass 7，2026-09-06）。最新 81-test 回归、三个证据校验器、运行包健康、完成批次幂等重试和 postflight 均通过；但 `AUTO_MAP=0`、18,065 条 ownership/hash blocker、17,864 条平台 ACTIVE 漂移及全量历史快照缺口仍保持 NO-GO，不进入 Revision/Iteration 迁移。

## 1. 独立测试范围

- 只读审查 `task.md`、`prd.md`、`dev-plan.md`、`test-plan.md`、`execution-log.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md` 和 `docs/database-rules.md`。
- 只允许写入本报告；未修改生产代码、测试代码、SQL、任务状态或数据库。
- 本轮未重复执行 DDL、业务写入、对象存储复制、服务重启、E2E 或 Git；仅独立复核已授权运行证据。其它 Maven/Java 进程属于 `D:\\IntRuoyiWorktree` 的并行任务，未干预。
- 已核对 tenant 1 处理 17,607 条（7 条 COMPLETED、17,600 条 BLOCKED）和 tenant 122 处理 465 条（全部 BLOCKED）的有界运行证据；全量历史中未完成项未被猜测回填。
- 已核对最新运行包 `E:\\IntRuoyi\\output\\runtime\\int_main\\backend-runtime-control-20260906-001702.jar`，进程实际使用该包，`curl http://127.0.0.1:48081/actuator/health` 返回 `{"status":"UP"}`。

## 2. 命令证据

### 2.1 DCC 定向回归

命令：

```text
mvn -o -pl yudao-module-dcc "-Dtest=DccControlledFileMapperTest,DccSourceOwnershipSchemaTest,DccControlledFileSourceOwnershipServiceTest,DccControlledFileSourceMigrationServiceTest,DccControlledFileSourceMigrationCommitServiceTest,DccControlledFileSourceGlobalClaimServiceTest,DccControlledFileSourceGovernanceClassifierTest,DccControlledFileSourceGovernanceManifestServiceTest,DccControlledFileSourceGovernanceExecutionServiceTest,DccControlledFileSourceGovernanceBatchServiceTest,DccControlledFileSourceGovernancePostflightServiceTest,DccControlledFileSourceGovernancePreparationServiceTest,DccControlledFileSourceGovernanceApiContractTest" "-Dsurefire.failIfNoSpecifiedTests=true" test
```

结果：PASS。`Tests run: 81, Failures: 0, Errors: 0, Skipped: 0`，Maven `BUILD SUCCESS`，总耗时约 53.4 秒。

覆盖的测试类及数量：

- `DccControlledFileMapperTest` 11
- `DccSourceOwnershipSchemaTest` 5
- `DccControlledFileSourceGlobalClaimServiceTest` 4
- `DccControlledFileSourceGovernanceBatchServiceTest` 10
- `DccControlledFileSourceGovernanceClassifierTest` 6
- `DccControlledFileSourceGovernanceExecutionServiceTest` 15
- `DccControlledFileSourceGovernanceManifestServiceTest` 10
- `DccControlledFileSourceGovernancePostflightServiceTest` 3
- `DccControlledFileSourceGovernancePreparationServiceTest` 3
- `DccControlledFileSourceMigrationCommitServiceTest` 3
- `DccControlledFileSourceMigrationServiceTest` 3
- `DccControlledFileSourceOwnershipServiceTest` 6
- `DccControlledFileSourceGovernanceApiContractTest` 2

合计：13 个测试类，81 tests，0 failures/errors。

### 2.2 Schema/API evidence validators

- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\database-schema-delivery\\scripts\\validate_database_schema.py --self-test` -> PASS。
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\database-schema-delivery\\scripts\\validate_database_schema.py --evidence doc\\tasks\\20260904-dcc-source-ownership-hash-governance\\database-schema-evidence.md` -> PASS。
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\backend-api-delivery\\scripts\\validate_backend_api.py --self-test` -> PASS。
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\backend-api-delivery\\scripts\\validate_backend_api.py --evidence doc\\tasks\\20260904-dcc-source-ownership-hash-governance\\backend-api-evidence.md` -> PASS。
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\backup-disaster-recovery-readiness\\scripts\\validate_backup_disaster_recovery.py --self-test` -> PASS。
- `python -X utf8 C:\\Users\\BJB110\\.codex\\skills\\backup-disaster-recovery-readiness\\scripts\\validate_backup_disaster_recovery.py --evidence doc\\tasks\\20260904-dcc-source-ownership-hash-governance\\runtime-backup-evidence.md` -> PASS。
- `git diff --check -- IntRuoyiBackend\\yudao-module-dcc doc\\tasks\\20260904-dcc-source-ownership-hash-governance` -> PASS（仅有 LF/CRLF 提示，无 whitespace error）。

Pass 7 复核再次执行 schema/API/backup evidence validator 和 `git diff --check -- IntRuoyiBackend\\yudao-module-dcc doc\\tasks\\20260904-dcc-source-ownership-hash-governance`，均 PASS。

### 2.3 数据库/运行态门禁

结果：PASS（已授权有界/全租户运行态）；全量迁移门禁仍 NO-GO。真实测试库 additive schema 已部署并核对三表为 InnoDB/唯一索引；独占 smoke 1/1、共享 smoke 3/3、postflight、同摘要幂等、manifest 篡改拒绝、全局 claim 唯一键冲突回滚、自动 prepare 游标和备份恢复均有证据。最新运行包健康为 UP，同一完成批次再次重试 `processedCount=0`、`completed=7`、postflight `7/7` 无发现。授权运行处理 tenant 1 的 17,607 条和 tenant 122 的 465 条，未完成项均进入 BLOCKED；smoke 数据及对象已清理，未创建 Revision/Iteration。

真实证据：`runtime-backup-evidence.md`、`verification-report.md`、`windchill-inventory-report.md`、`execution-log.md`。备份恢复到隔离库 11 张表 checksum 一致，访问日志历史行缺失/变更为 0；最新盘点时间为 `2026-09-06 00:02:45`，治理三表存在数为 3，并保留授权批次审计记录。

## 3. AC-01..AC-20 覆盖矩阵

| 验收 | 结论 | 独立证据/缺口 |
|---|---|---|
| AC-01 | PASS（只读盘点+合同） | 最新冻结盘点使用相同 `deleted=0`、ID 上限、全局引用和只读事务口径，最终 `ROLLBACK`；有界运行按同一冻结边界处理。 |
| AC-02 | PASS（小批真实+单测） | 真实 `CLAIM_SOURCE` smoke 完成，postflight 1/1，source/ownership/hash 一致；同摘要重试未重复处理。 |
| AC-03 | PASS（小批真实+单测） | 真实三条 shared-copy 均 COMPLETED，三条物理对象 key/infra_file ID 唯一，SHA-256 一致，postflight 3/3；代码还校验冻结全局引用集合、失败清理、批大小不得拆组以及单条 `COPY_SHARED_SOURCE` 不降级。全量历史共享组未执行。 |
| AC-04 | PASS（单测+全租户结果） | 分类器覆盖 `SOURCE_REFERENCE_MISSING`；未完成历史项保留治理 blocker，不生成默认 source/hash。 |
| AC-05 | PASS（盘点+单测） | 最新盘点识别 7 条软删除 source；分类器覆盖 `SOURCE_RECORD_DELETED`，未根据 ownership 或副本宣布完成。 |
| AC-06 | PASS（小批真实+单测） | 历史不可读源在真实 prepare/execute 中生成 `SOURCE_CONTENT_UNREADABLE` BLOCKED；分类器和无业务写入路径测试通过。 |
| AC-07 | PASS（单测/合同+全租户结果） | ownership 指针和 hash 不一致进入 blocker；全租户结果中未满足证据的记录未计为 COMPLETED。 |
| AC-08 | PASS（静态/单测） | 未使用文件名、路径或弱字段猜测；全局 source 引用 Mapper 合同通过。 |
| AC-09 | PASS（运行态+单测） | `requireVersioned` 校验 `CURRENT_RULE_VERSION`/`CURRENT_SCHEMA_VERSION`，执行先校验版本和 `CONFIRMED + manifest/request` 摘要；清单/范围篡改拒绝，旧 `/source-ownership-migration/run` 明确抛出 `CONTROLLED_FILE_SOURCE_GOVERNANCE_LEGACY_ENTRY_DISABLED`。 |
| AC-10 | PASS（单测+有界运行） | source ID、删除状态、定位、快照 hash 漂移测试通过；有界运行未发现绕过漂移门禁的成功记录。 |
| AC-11 | PASS（授权运行范围） | 真实唯一键冲突使事务退出码为 1 且批次插入回滚为 0；共享组和末端写入失败单测验证新副本清理并抛错；tenant 1/122 有界处理未产生 FAILED。此结论证明已执行范围，不代表未来全量维护窗口无需继续监控。 |
| AC-12 | PASS（小批真实+单测） | 同摘要终态重试返回 `processedCount=0`；task key/摘要冲突和 global claim 唯一约束测试通过，真实三表唯一索引已部署。 |
| AC-13 | PASS（授权运行+单测） | prepare 游标从 `2054545668044052029` 推进到 `2054545668044052031`；tenant 1/122 有界运行按游标续跑并完成 18,072 条状态对账，未完成项均 BLOCKED。 |
| AC-14 | PASS（授权运行+单测） | 全局跨租户引用越界分类和显式租户 Mapper 合同通过；tenant 1/122 运行中范围外引用保持 BLOCKED，未修改另一租户数据。 |
| AC-15 | PASS（备份恢复/授权运行边界）；BLOCKED（全量前后快照） | 11 张相关表恢复到隔离库并 checksum 一致，访问日志历史行缺失/变更为 0；代码有历史证据 hash 漂移测试。最新盘点明确治理前没有覆盖全部关联行的逐记录快照，不能用当前 after 盘点替代全量 before/after 对账。 |
| AC-16 | PASS（授权运行+单测） | 独占 smoke postflight 1/1、shared-copy postflight 3/3；代码对完成项重新查全局引用并在 `references.size() > 1` 时报告 `COMPLETED_SOURCE_STILL_SHARED`，授权完成项未发现共享 source。 |
| AC-17 | PASS（合同） | blocker VO、稳定 reason code、controlled file/tenant/source 字段和查询接口合同通过；真实导出脱敏结果未运行。 |
| AC-18 | PASS（完整只读重跑；业务结果 NO-GO） | 最新 2026-09-06 00:02:45 只读盘点使用同一查询版本，报告 `AUTO_MAP=0`、source gate 和其它 blocker；运行包健康 UP，未创建 Revision/Iteration。 |
| AC-19 | PASS（复现并解释） | 最新冻结报告复现 18,065 ownership blocker、7 软删除 source、42/290 全局共享（跨租户 2/26），并解释历史 43 组与当前 `deleted=0`/物理行口径差异；tenant 1/122 处理计数已记录。 |
| AC-20 | PASS（fail-closed）；BLOCKED（全量业务治理） | 18,072 条有效受控记录均已进入授权批次结果：7 条 COMPLETED，其余 18,065 条 BLOCKED；最新只读盘点仍明确 `NO-GO`、`AUTO_MAP=0`，未宣称全部历史文件已治理或具备 Revision/Iteration 条件。 |

## 4. 需要主 Agent 处理的 blocker

1. 18,065 条 ownership/hash blocker 仍需业务确认或补充正式证据，不能猜测回填；当前状态已正确保留为 BLOCKED。
2. 对审批、签名、分发、培训、打印和访问历史关联表建立全量治理前后快照及 hash 对账，不能只依赖备份恢复 checksum。
3. 继续使用同一查询版本重跑 Windchill 盘点；只有 AUTO_MAP、blocker 和历史证据均满足门禁时，才评估 Revision/Iteration 回填。

## 5. 独立放行结论

结论：NO-GO。Pass 7 确认 13 类、81 项定向回归，schema/backend/backup 三类 evidence validator，最新运行包健康 UP，完成批次幂等重试 `processedCount=0`、7 条完成项 postflight `7/7`，以及 2026-09-06 00:02:45 同口径只读盘点均有证据。AC-11 在授权运行范围通过；AC-15 的全量历史前后快照仍 BLOCKED；AC-18、AC-19 和 AC-20 已完成只读重跑、数量解释及 fail-closed 状态确认，但业务结果仍为 NO-GO。当前 18,065 条 ownership/hash blocker、17,864 条平台 ACTIVE 漂移、24 条路线快照孤儿和 685 条访问日志孤儿仍阻断 Revision/Iteration；不得宣称“全部历史文件已治理”。

## 6. 独立测试者签名

- 角色：T6 independent tester
- 测试时间：2026-09-06（Pass 7，最终独立复测）
- 写入范围：仅本文件
