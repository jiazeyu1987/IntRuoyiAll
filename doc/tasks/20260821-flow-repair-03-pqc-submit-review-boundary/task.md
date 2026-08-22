# 流程修复 3：一线 PQC 提交与 PQC 组长复核边界

## Task Goal

只通过代码审计、需求澄清和开发文档设计，冻结一线 PQC 提交、PQC 组长复核、结构化过程检验来源和后续正式过程检验单回填之间的职责边界。

目标流程是：一线 PQC 按正式任务提交签名后的逐件检验事实；PQC 组长只对同一份来源事实确认或退回；确认产生可被后续完成节点消费的结构化来源，不提前写正式过程检验单、不创建批次执行、不放行。

## Scope

- 审计现有 PQC 任务、逐件明细、检验项目、设备快照、复核记录、结构化汇集和后续来源映射。
- 设计唯一状态所有者、不可变来源身份、版本冻结、幂等、并发、修订和追溯合同。
- 定义与流程修复 4、6、7、8、9、10、11 的字段级输入输出契约。
- 设计后续实施的 BDD、RED、GREEN、REGRESSION 和真实 E2E 验证计划。
- 原设计阶段不修改生产代码、数据库或环境，不启动服务，不运行写入型 E2E；本轮获授权后仅在独立 worktree 实现流程 3 来源事实边界。流程修复 3 只负责 PQC 来源事实，不负责完成回填、批次执行、材料门禁或最终放行。

## Out Of Scope

- 不实现接口、服务、页面、迁移或测试代码。
- 不改变现有数据，不修复历史 PQC 记录，不创建或删除批次执行。
- 不决定流程修复 4、6、7、8、9、10、11 内部类名或 DTO/事件命名，只冻结其业务语义和所有权合同。流程 4 负责活跃订单双 100% 完成及三类适用回填；流程 6 按合法前置 receipt 创建/复用批次；流程 7 负责放行前 Origin/TraceLink、适用的 PQC 过程检验映射及放行后追溯读模型；流程 8 负责四份材料门禁；流程 9 负责非活跃订单入口前置凭证；流程 10 只负责最终放行状态、签名、CAS 与放行审计；流程 11 负责总体验证。
- 不以 `formBindings`、工序开始配置、旧 IPQC 数据、当前 QA 配置或当前设备配置替代正式 PQC 来源。

## Milestones

1. `completed`：读取项目规则、正式来源规则、产品角色流程、前端与 E2E 规则。
2. `completed`：只读审计 PQC 提交、复核、汇集和来源映射现状。
3. `completed`：按验收结论修订目标态、根因、接口/数据/状态、跨线程契约和测试计划。
4. `completed`：执行只读文档结构与内容验证，记录未运行项和 blocker。
5. `completed`：已统一 canonical receipt、跨线程稳定错误码、M4 口径和五份 Cleanup Keep，并完成重新扫描与收尾。

## Expected Verification

- 五个要求文档存在且为 UTF-8。
- 文档包含目标态、当前代码事实、根因、修改边界、接口/数据/状态设计、BDD、RED/GREEN/REGRESSION 计划、失败 blocker、迁移/回滚边界和流程 4/6/7/8/9/10/11 契约；明确活跃订单与独立场景两个合法建批分支、`formalProcessInspectionDocumentId` 由流程 4 产生、`batchExecutionProcessInspectionRecordId` 由流程 7 产生，且流程 7 放行前映射是流程 8/10 的硬前置。
- 所有未执行的生产测试和 E2E 明确标记为 `planned / NOT RUN`，不得宣称功能已经实现；四份材料统一为来料检报告、灭菌报告、成品检报告、成品检记录。
- 结构检查确认没有修改生产代码、数据库或运行环境。

## Current Status

in_progress

独立 worktree 已完成 P1 来源回执身份与 P2 终态复核边界的最小实现及合同测试增补。旧测试接口漂移已修正，Flow3 定向测试 27/27 通过；主代码编译已通过。分支运行时门禁、task-owned commit、受保护融合、主工作树复验和 P3 只读来源验证仍待完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。任何正式来源、版本、状态或身份缺失都设计为明确阻断。
- `是否从根因和长期维护角度解决`：是。以唯一状态所有者、不可变结构化来源、版本冻结和跨节点幂等为根因方案。
- `是否存在临时补丁或绕过`：否。文档禁止 raw payload、旧 IPQC、当前配置重查、默认值或页面缓存补齐正式来源。

## Applicable Experience Gates

- `docs/backend-development.md` 的“活跃订单申请放行资料必须只使用正式来源”。
- `docs/backend-development.md` 的“PQC 过程检验汇集必须形成最终确认明细”。
- `docs/product/production-role-system-operations.md` 中一线 PQC、PQC 组长、活跃订单完成与放行职责。
- `docs/frontend-development.md` 中一线 PQC 正式任务、逐件明细、设备可选性、连续提交和 Long ID 边界。
- `docs/e2e-rules.md` 中真实页面、任务自有写入数据、正式 PQC 上下文与禁止 API-only 替代。
- `AGENTS.md` 中严格无 fallback、三类配置链路独立、任务文档和 BDD/TDD 要求。

## Initial Compliance Conclusion

- 符合：现有代码已把 PQC 任务、逐件检验值、检验项目、路线/规程版本、设备身份快照和复核后的结构化汇集持久化为正式来源事实。
- 符合：未发现一线 PQC 提交或 PQC 组长复核直接创建正式过程检验单的证据；现有汇集明细只是后续映射来源。
- 部分符合：组长批准后在同一事务将任务推进为 `CONFIRMED` 并写汇集明细，可作为“来源确认”；`CONFIRMED` 绝不表示正式过程检验单已回填、批次已创建、材料已齐套或已放行。
- 不符合/未闭合（代码层）：文档已冻结终态后普通复核冲突、同命令幂等、受控修订另立命令及唯一有效版本规则，但现有生产代码尚未完整实现/验证；流程 4 精确消费确认版本和流程 7 独立批次映射也尚未完成生产验证。

## Unresolved Blockers

- 业务语义、字段身份、状态 owner、终态复核/受控修订规则、四份材料及禁止替代规则已冻结；独立入口 canonical 凭证固定为 `IndependentBatchPrerequisiteReceipt`，跨线程 blocker 固定使用 `BACKFILL_RECEIPT_REQUIRED`、`SOURCE_SNAPSHOT_MISMATCH`、`ENTRY_PREREQUISITE_MISSING`、`ENTRY_SOURCE_INVALID`、`TRACE_MAPPING_BLOCKED`、`TRACE_SOURCE_CONFLICT`、`RELEASE_GATE_BLOCKED`、`RELEASE_SNAPSHOT_MISMATCH`。具体 DTO/事件载体可统一映射，但不得保留别名或改变语义。
- 生产代码尚未实现并验证全部状态机、跨流程 receipt、两类建批入口、流程 7 放行前映射、流程 8/10 门禁和真实 E2E，因此不能据本文档宣称生产链路已通过。
- 历史已 `CONFIRMED`/汇集/正式单据/批次数据仍缺迁移对账证据；无法证明来源版本、签名、逐件明细、设备快照和 hash 一致的数据必须列入迁移阻断清单。
- 当前合同冻结四份材料为来料检报告、灭菌报告、成品检报告、成品检记录，且成品检报告与成品检记录不可互代；旧产品文档过时材料口径仅为待修订文档/历史兼容项，不构成本专项设计 blocker。

## Deliverables

- `task.md`
- `development-plan.md`
- `test-plan.md`
- `execution-log.md`
- `verification-report.md`

## Cleanup Keep

- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/task.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/development-plan.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/test-plan.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/execution-log.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/verification-report.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/prd.md
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/task-state.json
- doc/tasks/20260821-flow-repair-03-pqc-submit-review-boundary/test-report.md

## Implementation Evidence

- P1 提交回执暴露 `sourceRevision`（现有不可变 `submittedEventId`）和 `payloadHash`（冻结 `submittedContentHash`）；同内容重试复用原结果，内容冲突不重复写入。
- P2 同一终态、同组长、同决定和规范化备注的复核重试返回原 `reviewId`，不会重复签名、复核或汇集；相反决定仍返回终态冲突。汇集异常向外传播，事务可回滚。
- `mvn.cmd -f IntRuoyiBackend/pom.xml -pl yudao-module-mes -am -Dmaven.test.skip=true package`：PASS（主代码编译，跳过测试）。
- 定向测试：PASS，4 个 Flow3 相关测试类共 27 tests，0 failures、0 errors；测试夹具补齐冻结工序快照，未放宽生产校验。
- 分支运行时资料已同步到 1..50 槽位合同；commit hook、task-owned commit、受保护融合和主工作树复验仍待执行。
