# 开发计划

## Task Graph

### T1：冻结边界与全局源引用盘点合同

- task_id: T1
- title: 冻结边界与全局 source_file_id 引用盘点
- objective: 让治理清单以有效受控文件、冻结 controlled_file_id 上限和全局 source_file_id 引用为唯一输入，识别范围外租户引用、软删除源文件和正文可读性。
- dependency_ids: []
- affected_paths: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapper.java`、`IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/dal/mysql/file/DccControlledFileMapperTest.java`、`IntRuoyiBackend/sql/mysql/`
- write_scope: 只读盘点查询、Mapper 合同、schema 设计和对应单测；不得执行测试库写入。
- acceptance_ids: AC-01, AC-04, AC-05, AC-06, AC-08, AC-14, AC-19, AC-20
- validation_steps: 运行 DCC Mapper/SQL 合同单测；验证 `deleted = 0`、冻结 ID、全局 source_file_id 聚合、范围外引用阻塞、原因码稳定；执行只读数据库 postflight。
- done_definition: 查询不按业务租户误判全局源文件独占；每条记录可分类为 READY/BLOCKED，且无正文、凭据或跨租户业务数据泄露。

### T2：治理清单、状态和幂等数据合同

- task_id: T2
- title: 治理清单与 BLOCKED/幂等状态模型
- objective: 扩展或新增 additive schema，保存清单版本、冻结边界、逐条证据快照、稳定原因码、请求摘要和可续跑状态。
- dependency_ids: [T1]
- affected_paths: `IntRuoyiBackend/sql/mysql/`、`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileSourceMigrationDO.java`、`IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/dal/dataobject/file/DccControlledFileSourceOwnershipDO.java`、对应 Mapper 与 `DccSourceOwnershipSchemaTest.java`
- write_scope: 仅任务相关 SQL、DO、Mapper 和 schema 测试；不修改现有业务数据，不改 Revision/Iteration 表。
- acceptance_ids: AC-07, AC-09, AC-12, AC-13, AC-17, AC-19, AC-20
- validation_steps: schema validator；唯一约束/索引合同测试；同键同摘要幂等、同键不同摘要冲突、清单 hash 变更拒绝、BLOCKED 不入批测试。
- done_definition: 现有 `PENDING/COPY_VERIFIED/COMPLETED/FAILED` 不再承担业务 blocker 语义；清单和明细可审计且能安全续跑。

### T3：证据分类与治理执行服务

- task_id: T3
- title: READY/BLOCKED 分类、独占认领和共享复制
- objective: 将现有批次服务改为消费已确认清单，独占源直接认领，共享组按全局规则复制，所有提交具备 hash 和漂移保护。
- dependency_ids: [T1, T2]
- affected_paths: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccControlledFileSourceOwnershipService.java`、`DccControlledFileSourceMigrationService.java`、`DccControlledFileSourceMigrationCommitService.java` 及三者对应测试
- write_scope: 仅源文件 ownership/migration 服务、Mapper 调用和单元测试；不得触碰 original/published/stamped、审批、签名和平台生命周期。
- acceptance_ids: AC-02, AC-03, AC-07, AC-08, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15
- validation_steps: 严格 TDD 先 RED 后 GREEN；覆盖可读独占、共享全复制、软删除/不可读、hash mismatch、漂移、事务失败、幂等、续跑和租户隔离。
- done_definition: 所有完成记录 source/ownership/migration 证据原子一致；任何歧义或漂移都失败关闭，不猜测、不静默成功。

### T4：管理 API 与治理报告

- task_id: T4
- title: 清单确认、批次执行和 blocker 查询接口
- objective: 提供冻结清单生成/确认、受控批次执行、暂停续跑、明细汇总和脱敏 blocker 查询，拒绝未确认或 hash 不一致清单。
- dependency_ids: [T2, T3]
- affected_paths: `IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/controller/admin/file/DccControlledFileController.java`、对应 source migration VO、DCC API 合同测试；必要时 `IntRuoyiFronted/src/api/dcc/controlledFile.ts`，默认不新建页面
- write_scope: 仅 DCC source governance API 和合同测试；不执行真实写批次，不新增无批准页面。
- acceptance_ids: AC-09, AC-12, AC-13, AC-17
- validation_steps: API HTTP 合同测试；权限/租户范围负向测试；请求摘要和错误码测试；确认当前接口不会绕过清单门禁。
- done_definition: 管理员可查询和控制有界批次，业务租户看不到范围外租户明细，任何未确认清单请求 fail-fast。

### T5：Postflight 与 Windchill 盘点重跑

- task_id: T5
- title: 源证据 postflight 和完整迁移门禁重跑
- objective: 对已完成记录重算正文 hash、核对 ownership/指针独占和历史证据不变，并重新执行完整 Windchill AUTO_MAP 盘点。
- dependency_ids: [T3, T4]
- affected_paths: DCC source postflight 查询/服务、`doc/tasks/20260904-dcc-windchill-version-test-db-inventory/inventory-report.md` 的重跑证据和只读验证脚本
- write_scope: 只读 postflight、报告和验证脚本；不得自动创建 Revision/Iteration 或修改 remaining blocker。
- acceptance_ids: AC-15, AC-16, AC-18, AC-19, AC-20
- validation_steps: 只读数据库 postflight；哈希重算；历史指针对账；完整 Windchill 盘点；报告语义和证据验证。
- done_definition: 所有 COMPLETED 记录证据一致，剩余 BLOCKED 可逐条解释，输出最新 AUTO_MAP，且没有自动进入下一阶段的副作用。

### T6：独立回归与放行审查

- task_id: T6
- title: 独立测试、回归与阶段放行
- objective: 由不同于执行者的测试角色独立验证 T1-T5 的验收标准，形成 test-report 并由主 Agent 做最终 review。
- dependency_ids: [T5]
- affected_paths: `doc/tasks/20260904-dcc-source-ownership-hash-governance/test-report.md`、任务相关测试输出
- write_scope: 只允许写 `test-report.md`；不得修复产品代码或改写计划/状态。
- acceptance_ids: AC-01, AC-02, AC-03, AC-04, AC-05, AC-06, AC-07, AC-08, AC-09, AC-10, AC-11, AC-12, AC-13, AC-14, AC-15, AC-16, AC-17, AC-18, AC-19, AC-20
- validation_steps: 运行 test-plan 中全部定向测试；必要时做真实只读 postflight；记录 PASS/FAIL/BLOCKED 和证据。
- done_definition: 所有 AC 有独立证据；失败项返回对应执行任务，最多三轮修复/复测；无未解决 blocker 才能阶段放行。

## Wave Rules

- Wave 1：T1。
- Wave 2：T2。
- Wave 3：T3。
- Wave 4：T4。
- Wave 5：T5。
- Wave 6：T6。
- T1/T2/T3 不并行，原因是它们共享 DCC 源治理 schema、全局引用语义和同一批次状态边界。
- 任何执行任务发现数据库字段、对象存储、全局读取权限或维护窗口缺失，必须停止并标记 blocker。
