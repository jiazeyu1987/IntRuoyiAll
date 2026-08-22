# Test Plan

## Purpose and Scope

本计划只定义后续实现的 BDD、严格 TDD、回归、结构/schema/真实页面门禁。本专项只验证流程修复 5 被流程修复 4 在活跃订单完成节点调用，且提交/复核不会提前触发回填；不验证流程 4 的完成编排、流程 6 建批、流程 8 四材料门禁、流程 10 最终放行或流程 11 总门禁的实现细节。本任务不运行生产代码、服务、数据库或写入型 E2E；所有命令均为实施阶段模板，未运行不得写成 PASS。

## Evidence Reviewed

- 流程5核心测试已覆盖正损耗来源、数量守恒、签名、映射、NO_LOSS、缺失事实 BLOCKED 和幂等；实现前的 ZERO_LOSS_CONFIRMATION_UNSUPPORTED 仅作为 RED 基线证据。
- 现有批次执行 port 单测覆盖申请上下文复用和遗留批次阻塞。
- 前端真实流程测试当前要求 lossReportEvidenceIds 非空，后续需改为按工序 decision 与唯一 `lossReportStatus` 分层断言。
- 流程修复 4 测试计划要求完成节点同事务统一回填；无损耗工序保存 NO_LOSS，工序及订单 receipt 使用唯一字段 `lossReportStatus=NOT_REQUIRED`。
- 流程修复 1/6 的正式绑定合同要求同时消费 `pickListBindingId`、`pickListId`、`sourceSnapshotHash`、`bindingVersion`、`batchPickListRelationId`；流程修复 5 只读校验，不创建或猜测领料关系。
- 流程修复 4/6 合同要求逐工序和订单级显式输出 `hasActualLoss`；不能从缺少 `lossRecordId` 推断无损耗。

## Feature Scenarios

BDD: 正数损耗按工序建单 -> Given 双进度均为 100%，某冻结工序正式反馈 unqualifiedQuantity > 0，五字段领料绑定快照完整，原因、明细、填写签名和组长复核签名完整且映射已发布；When 流程 4 完成节点调用流程 5；Then 该工序生成一次损耗单，决策为 REQUIRED，`hasActualLoss=true`、`lossQuantity>0`，工序及订单 receipt 的 `lossReportStatus=SUCCESS`，并记录绑定快照、来源 ID、签名快照和来源哈希。

BDD: 无损耗不建单 -> Given 某冻结工序正式反馈已确认零损耗且规范化损耗数量等于 0，五字段领料绑定快照完整；When 流程 4 的完成节点调用流程 5；Then 流程 5 返回 `NO_LOSS`，该工序 `hasActualLoss=false`、`lossQuantity=0`，流程 4 在工序及订单完成 receipt 中保存 `lossReportStatus=NOT_REQUIRED`、事实和来源快照，lossReportId/lossEvidenceIds 为空，不创建损耗单或任何无损耗报告。

BDD: 缺零损耗事实阻塞 -> Given 数量为 0 但没有正式零损耗确认字段或事件，或五字段领料绑定快照缺失；When 流程 4 调用流程 5；Then 返回 `NO_LOSS_FACT_REQUIRED` 或绑定 blocker，不输出 `hasActualLoss=false`，流程 4 不提交三类回填 receipt，流程 6 不得建批。

BDD: 部分工序损耗 -> Given 工序 A 损耗大于 0、工序 B 明确无损耗且两者均有五字段领料绑定快照；When 流程 4 在同一完成节点调用流程 5；Then 只创建 A 的损耗单，A 为 `REQUIRED/hasActualLoss=true/lossQuantity>0/lossReportStatus=SUCCESS`，B 返回 `NO_LOSS/hasActualLoss=false/lossQuantity=0/lossReportStatus=NOT_REQUIRED`，流程 4 receipt 同时保存逐工序 decisions 且订单级 `hasActualLoss=true/lossReportStatus=SUCCESS`。

BDD: 任一工序来源缺失 -> Given 工序 A 正损耗、工序 B 缺正式反馈、签名或五字段绑定快照；When 流程 4 完成节点调用流程 5；Then 整体 BLOCKED，不输出订单级 `hasActualLoss` 成功 receipt，A 的损耗单也不留下，不能部分成功。

BDD: 原因和明细守恒 -> Given 损耗分类合计、明细合计或原因快照不一致；When 规划损耗；Then 返回稳定 blocker，不创建损耗单。

BDD: 重复完成幂等 -> Given 同一完成版本、五字段领料绑定快照和来源哈希已成功；When 以同一幂等键重试；Then 返回相同完成回执、订单/工序 `hasActualLoss` 和损耗单 ID，不重复创建。

BDD: 同键载荷冲突 -> Given 幂等键已绑定来源哈希和五字段领料绑定快照；When 使用不同来源哈希、绑定版本、绑定关系、工序集合或零损耗标记重试；Then 返回 LOSS_IDEMPOTENCY_PAYLOAD_CONFLICT 或绑定快照 blocker。

BDD: 来源快照变化 -> Given 规划后正式反馈、签名、路线绑定版本或五字段领料绑定快照变化；When 写入损耗；Then 返回 LOSS_SOURCE_SNAPSHOT_CHANGED 或 LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED，事务回滚。

BDD: 放行后追溯区分无损耗 -> Given 一工序有损耗单且 `hasActualLoss=true/lossReportStatus=SUCCESS`、另一工序在完成 receipt 中为 `NO_LOSS/hasActualLoss=false/lossQuantity=0/lossReportStatus=NOT_REQUIRED`，两者均有五字段绑定快照且下游流程已放行；When 通过流程 7/10 的正式追溯入口查询；Then 前者显示损耗单和明细，后者显示“已确认无损耗”及绑定/来源快照，不显示损耗单或任何无损耗报告。流程 5 只提供被消费的事实，不拥有放行状态。

## Failure and Boundary Scenarios

- 负数、空值、超精度、数量守恒失败：LOSS_QUANTITY_INVALID 或 LOSS_QUANTITY_CONSERVATION_FAILED，零写入。
- 原因 ID/编码/名称缺失或被当前主数据覆盖：LOSS_REASON_SNAPSHOT_REQUIRED，不得使用最新名称补齐。
- 明细数量为 0、明细合计不等于正数损耗：LOSS_DETAIL_REQUIRED。
- 一线或组长签名缺失：LOSS_SIGNATURE_REQUIRED。
- 五字段领料绑定快照缺失、字段不一致或 hash/version 变化：LOSS_SOURCE_PICK_LIST_BINDING_REQUIRED 或 LOSS_SOURCE_PICK_LIST_BINDING_SNAPSHOT_CHANGED；不得使用 materialPickListId 单字段替代。
- `hasActualLoss` 缺失、与 `lossQuantity` 或正式无损耗快照矛盾：LOSS_HAS_ACTUAL_LOSS_REQUIRED 或 LOSS_HAS_ACTUAL_LOSS_CONFLICT；不得从缺少 lossRecordId 推断 false。
- 有损耗但正式损耗报表绑定/版本缺失：LOSS_REPORT_MAPPING_REQUIRED。
- 完成前生产/PQC 提交或组长复核：只形成来源事实，不调用流程 5、不回填损耗单。
- 历史损耗单没有有效完成回执关联：LEGACY_LOSS_DECISION_MIGRATION_REQUIRED，不认领、不删除。

## Strict TDD Sequence

1. RED：新增流程 1 五字段绑定快照、`NO_LOSS`、`hasActualLoss`、`lossReportStatus` 完成 receipt 事实、正数判定和部分工序测试，预期现有实现因 `ZERO_LOSS_CONFIRMATION_UNSUPPORTED`、绑定字段缺失或无条件 evidence 断言失败；GREEN：实现流程 5 条件 decision 和显式 true/false 输出。
2. RED：同事务中任一工序失败后检查无部分损耗单/回执/批次；GREEN：完成编排器统一事务和异常传播。
3. RED：同键同载荷、同键不同载荷、来源快照变化和并发完成测试；GREEN：唯一键、哈希和乐观版本门禁。
4. RED：流程修复 6/7 合同测试，验证五字段绑定快照、订单/工序 `hasActualLoss` 和 `lossReportStatus=NOT_REQUIRED`（工序决策 `NO_LOSS`）不被批次建批或来源映射要求损耗 evidence，且 `BLOCKED` 不得建批；GREEN：仅由下游消费成功 receipt 和正式映射，不从缺少损耗单 ID 推断 false。
5. RED：流程修复 8/10/11 的跨线程合同测试，验证四材料、最终放行和迁移门禁不由流程 5 旁路；GREEN：保持各自 owner 和 blocker。

## RED / GREEN / REGRESSION Evidence Template

BDD: <scenario> -> Given/When/Then
RED: <exact command> -> FAIL, <expected reason>
GREEN: <exact command> -> PASS
REGRESSION: <exact command> -> PASS

实施阶段禁止使用 mock、直接 SQL、API-only 或默认成功替代真实正式来源；未满足正式租户、账号、模板、服务和可清理任务数据时记录 BLOCKED。

## Regression Matrix

| 层级 | 必测内容 | 证据 |
| --- | --- | --- |
| 后端单测 | 正/零/部分工序、原因、明细、签名、幂等、版本、回滚 | 测试输出和持久化断言 |
| schema/合同 | 状态枚举、唯一键、Long ID 字符串、五字段领料绑定快照、订单/工序 hasActualLoss 与 lossQuantity、可空损耗 evidence、来源快照 | 合同测试/迁移预检 |
| 前端静态 | 完成按钮、工序 REQUIRED/NO_LOSS/BLOCKED、receipt `lossReportStatus=SUCCESS/NOT_REQUIRED`、重复保护、blocker、追溯文案 | 静态测试 |
| Playwright（跨流程） | 真实组长完成、有损耗订单、无损耗订单、部分工序、四份材料、管理者代表放行、追溯 | 流程 5 只核对完成 receipt 和条件损耗；四份材料由流程 8、最终放行由流程 10、总门禁由流程 11 提供证据 |

## Test Blockers

流程5核心21项 JUnit、MES compile、git diff-check 和 runtime guard 已通过。主线组合中 `MesFrontlineRuntimeConfigProcessScopeTest` 失败，属于前线运行时参数校验静态契约，不属于流程5 owner。数据库迁移、服务和写入型 E2E 未运行；流程4/6/7/8/10/11 的跨线程合同和全链路门禁仍由各自 owner 负责。
