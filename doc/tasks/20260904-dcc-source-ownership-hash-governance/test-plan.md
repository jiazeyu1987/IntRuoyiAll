# 测试计划

## Test Cases

### TP-01：冻结边界与全局引用

- test_case_id: TP-01
- mapped_task_ids: [T1]
- mapped_acceptance_ids: [AC-01, AC-08, AC-14, AC-19]
- environment or setup: 测试库只读连接；受控数据库读取权限；固定测试租户和冻结 controlled_file_id 上限
- steps: 生成清单；按全局 source_file_id 聚合；检查 `deleted = 0`、范围外引用和清单 hash
- expected_result: 汇总和明细口径一致；范围外有效引用整组 BLOCKED；无法完成全局核验时使用 CHECK_UNAVAILABLE；不写库
- evidence: 只读 SQL 输出、冻结边界、清单 SHA-256、报告路径

### TP-02：缺失/软删除/不可读/无效 ownership

- test_case_id: TP-02
- mapped_task_ids: [T1, T2, T3]
- mapped_acceptance_ids: [AC-04, AC-05, AC-06, AC-07, AC-17, AC-20]
- environment or setup: 单测 fixture 覆盖空 source_file_id、软删除 infra_file、缺定位、正文读取异常、ownership 指针/hash 不一致
- steps: 执行只读分类和已确认批次入口
- expected_result: 稳定 BLOCKED 原因；不创建副本、不改 source 指针、不计 COMPLETED；查询能解释 blocker
- evidence: JUnit 结果、mapper capture、状态和写入次数断言
- classifier regression: `DccControlledFileSourceGovernanceClassifierTest`。

### TP-03：独占源认领

- test_case_id: TP-03
- mapped_task_ids: [T3]
- mapped_acceptance_ids: [AC-02, AC-16]
- environment or setup: 有效、可读、全局未共享源文件 fixture
- steps: 执行已确认治理；重新读取正文和 ownership
- expected_result: source_file_id 不变；ownership 指针、租户和 SHA-256 一致；postflight PASS
- evidence: 服务单测、事务调用验证和 postflight 输出

### TP-04：共享源全部隔离

- test_case_id: TP-04
- mapped_task_ids: [T1, T3]
- mapped_acceptance_ids: [AC-03, AC-08, AC-14]
- environment or setup: 同一全局 source_file_id 被多个有效受控记录引用，另含范围外租户引用和无范围外引用两组 fixture
- steps: 分别生成治理计划并执行可执行组
- expected_result: 无范围外引用时每条记录获得不同副本且 hash 相等；有范围外引用时整组 BLOCKED，不复制、不按租户认领
- evidence: 副本 ID、origin_source_file_id、哈希、全局引用分类和未写入断言

### TP-05：清单确认、hash 和漂移

- test_case_id: TP-05
- mapped_task_ids: [T2, T3, T4]
- mapped_acceptance_ids: [AC-09, AC-10, AC-11]
- environment or setup: 已生成 READY 清单；分别篡改清单 hash、source_file_id、文件状态、定位或正文
- steps: 提交批次；模拟副本创建后事务失败
- expected_result: 请求 fail-fast 或记录 BLOCKED/FAILED；无部分业务成功；副本有待清理证据；不吞异常
- evidence: API/JUnit 输出、事务回滚断言、清理结果
- implementation coverage: `executeSharedGroup` freezes the global reference set, rejects a batch size that splits a shared group, and cleans newly created copies when commit or final evidence persistence fails.

### TP-06：幂等与续跑

- test_case_id: TP-06
- mapped_task_ids: [T2, T3, T4]
- mapped_acceptance_ids: [AC-12, AC-13]
- environment or setup: 一个 COMPLETED 记录和一个中断批次；稳定任务键和请求摘要
- steps: 同摘要重试、不同摘要重试、中断后续跑
- expected_result: 同摘要返回原结果不复制；不同摘要冲突；已完成条目不变；未完成条目继续且计数可对账
- evidence: 数据库只读对账、mock interaction、批次结果
- implementation coverage: `DccControlledFileSourceGovernanceBatchServiceTest` covers confirmed-batch continuation, READY-only consumption, shared-group execution and scope rejection.

### TP-07：历史证据隔离

- test_case_id: TP-07
- mapped_task_ids: [T3, T5]
- mapped_acceptance_ids: [AC-15, AC-16]
- environment or setup: 带 original/published/stamped、审批、签名、分发、培训、打印和访问历史的受控记录
- steps: 执行治理后对比治理前快照
- expected_result: 非 source 证据主键、指针、版本标签和哈希不变；source postflight 一致
- evidence: 前后只读快照和差异为零的对账结果

### TP-08：Windchill 完整盘点重跑

- test_case_id: TP-08
- mapped_task_ids: [T5, T6]
- mapped_acceptance_ids: [AC-18, AC-19, AC-20]
- environment or setup: source postflight PASS；最新测试库冻结窗口
- steps: 重跑全量身份、版本、指针、检出、平台和源证据盘点
- expected_result: 输出最新 AUTO_MAP 和全部 blocker；不创建 Revision/Iteration；数字变化有冻结边界或数据变更解释
- evidence: 盘点报告、查询版本、快照 hash、schema validator
- implementation coverage: `windchill-readonly-inventory.sql` and `windchill-inventory-report.md` record the latest read-only freeze; execution remains blocked until governance schema deployment and source postflight.

### TP-09：系统级回归

- test_case_id: TP-09
- mapped_task_ids: [T1, T2, T3, T4, T5, T6]
- mapped_acceptance_ids: [AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16, AC-17, AC-18, AC-19, AC-20]
- environment or setup: DCC 模块定向 Maven 单测、schema 合同、只读测试库；E2E 仅在用户另行授权时执行
- steps: 按任务顺序执行 schema、service、API、postflight 和只读盘点验证
- expected_result: 所有计划测试真实执行并记录 PASS/FAIL/BLOCKED；未授权 E2E 不被冒充为通过
- evidence: `test-report.md`、命令输出、独立测试者签名和主 Agent review 记录

## Task-Level Validation

- T1/T2：schema validator、SQL/Mapper 合同和只读数据分类。
- T3：DCC source ownership/migration 服务定向 JUnit，严格 RED -> GREEN。
- T4：DCC HTTP API 合同和权限/租户负向测试。
- T5：只读 postflight、哈希重算、历史证据对账和 Windchill 报告验证。
- T6：独立复测全部 AC，不能依赖执行者口头结论。

## System-Level Validation

- 数据库：所有查询和 postflight 必须只读；写批次只能在后续单独授权和维护窗口执行。
- 对象存储：副本正文字节和 SHA-256 相等；失败副本有清理证据。
- 生命周期：不得改写 original/published/stamped、审批、签名、分发、培训、打印和访问历史。
- 迁移门禁：source postflight 通过后才能重跑 AUTO_MAP，不能自动创建 Revision/Iteration。

## Regression Checks

- `DccControlledFileSourceOwnershipServiceTest`
- `DccControlledFileSourceMigrationServiceTest`
- `DccControlledFileSourceMigrationCommitServiceTest`
- `DccSourceOwnershipSchemaTest`
- DCC 受控文件查询/发布/签名相关定向回归测试
- 只读数据库全量盘点与文档证据校验
