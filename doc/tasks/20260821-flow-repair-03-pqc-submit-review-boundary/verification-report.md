# Verification Report

## Scope

本报告验证流程修复 3 的设计合同、task-owned 代码实现及其当前 `int_main` 主线程证据；不把下游流程或全链路放行当作本专项通过。

## Evidence Reviewed

- `AGENTS.md`
- `docs/task-closeout-rules.md`
- `docs/experience-index.md`
- `docs/product/production-role-system-operations.md`
- `docs/backend-development.md` 中“活跃订单申请放行资料必须只使用正式来源”与“PQC 过程检验汇集必须形成最终确认明细”等相关门禁
- `docs/frontend-development.md`
- `docs/e2e-rules.md`
- 后端 PQC task、piece detail、leader review、aggregation、aggregate detail 与批记录来源映射代码
- 既有相邻任务文档结构作为格式参考
- `int_main` 提交历史：`aeb58c37d`、`8759b45f9`、`1197ce3e0`
- 主线程定向 Maven、MES reactor compile、runtime guard 和 Python 回归输出

## Documentation Verification

| Check | Result | Evidence |
| --- | --- | --- |
| 独立任务目录存在 | PASS | task directory present |
| 五个要求文档存在 | PASS | five required files present |
| 目标态/当前事实/根因/边界 | PASS | `development-plan.md` 1-4 |
| 接口/数据/状态/幂等 | PASS | `development-plan.md` 5-9 |
| 流程 4/6/7/8/9/10/11 契约 | PASS | `development-plan.md` 10 |
| 两个合法建批分支与互斥 receipt | PASS | `development-plan.md` 10, `test-plan.md` Scenario I |
| canonical receipt 与跨线程稳定错误码 | PASS | `IndependentBatchPrerequisiteReceipt`; receipt/trace/release 八个稳定码已在设计、BDD 和 blocker 中传播 |
| 流程 7/10 状态 owner 与放行前映射门禁 | PASS | `development-plan.md` 4/6/10, `test-plan.md` Scenario J/K |
| 迁移/回滚边界 | PASS | `development-plan.md` 11-12 |
| BDD 与 RED/GREEN/REGRESSION 计划 | PASS | `test-plan.md`, `execution-log.md` |
| 失败 blocker | PASS | 五份文档均有明确阻断范围 |
| 禁止提前正式回填 | PASS | 目标态、状态机、接口和测试均锁定 |
| 禁止 fallback/来源推断 | PASS | task 约束、blocker contract 和负向测试锁定 |
| task-owned 提交回执身份 | PASS | `sourceRevision=submittedEventId`、`payloadHash=submittedContentHash`，并由 27 个定向测试覆盖回放/冲突 |
| 主线程当前 HEAD 包含流程 3 | PASS | `int_main` HEAD `1197ce3e0ee0b63c8fdcfb51bcf2bc80e9e9bfed`，包含 `aeb58c37d` |
| 主线程流程 3 定向测试 | PASS | 4 个测试类共 27/27，Failures 0，Errors 0 |
| 主线程 MES 相关编译 | PASS | `-pl yudao-module-mes -am -Dmaven.test.skip=true compile` BUILD SUCCESS |
| runtime guard | PASS | `int_main/int_main: frontend 8081, backend 48081` |

## Code Compliance Conclusion

- 当前结构化来源模型总体符合专项目标：PQC 任务、逐件明细、检验项目、设备快照、复核和汇集明细均有正式字段与事务校验。
- task-owned 提交回执已在主线程暴露 `sourceRevision` 与 `payloadHash`；相同内容重试复用同一来源身份，相同幂等键不同内容触发冲突，定向测试已验证。
- 当前组长批准会推进来源为 `CONFIRMED` 并生成 aggregate detail；该行为可保留为“来源事实确认”，但必须与正式过程检验单回填、批次执行创建、材料齐套和最终放行彻底区分。
- 未发现本次审计范围内由一线 PQC 提交或组长复核直接创建正式过程检验单的证据。
- 业务语义、字段身份、状态 owner、终态/受控修订、两个建批分支、四份材料与禁止替代规则已在五份文档冻结；具体 DTO/事件名称允许实现统一映射。
- 当前仍不具备生产放行结论：流程 4/6/7/8/9/10/11 的跨流程合同、统一门禁、多入口追溯、历史迁移对账和真实 E2E 尚未完整实现/验证。流程 3 的来源事实边界不能替代这些下游证据。

## Explicitly Not Run

- Production code changes: `FLOW 3 TASK-OWNED IMPLEMENTATION DONE`; downstream flow implementation remains outside this evidence
- Database/schema/data operations: `NOT DONE`
- Service start/restart: `NOT DONE`
- RED/GREEN/REGRESSION implementation evidence: `FLOW 3 GREEN/REGRESSION PASS`; full downstream matrix `NOT RUN`
- Maven production tests: `FLOW 3 FOCUSED PASS`（27/27）；完整服务测试和写入型 E2E `NOT RUN`
- Playwright/read-write E2E: `NOT RUN`（计划项，未执行）
- Git commit/push: `FLOW 3 COMMIT AND FAST-FORWARD MERGE DONE`; no push performed

## Unresolved Blockers

1. 终态普通复核幂等/冲突与独立受控修订合同已冻结，但生产代码和自动化测试尚未证明其执行。
2. 流程 4 的 `aggregateVersionId -> formalProcessInspectionDocumentId`、流程 6 的 `completionBackfillReceipt`/`IndependentBatchPrerequisiteReceipt` 互斥分支、流程 7 pre-release/过程检验映射及流程 8/10 硬门禁尚未完成生产实现和验证；生产测试必须断言已冻结的 receipt/trace/release 稳定码。
3. 批次详情、PQC/生产申请、管理者代表批准、独立批次放行等入口尚未通过真实 E2E 证明只能适配统一 gate/finalization。
4. 历史确认/汇集/正式单据/批次执行不能完整对账的记录仍缺迁移证据，必须作为 migration blocker，不得自动猜测或复用。
5. 四份材料和状态 owner 已冻结；旧产品文档过时材料口径仅为待修订文档/历史兼容项。跨线程载体名可调整，但不得保留 canonical receipt 别名或弱化稳定错误码。

## Final Result

completed for the Flow 3 task-owned scope：代码已融合并在当前 `int_main` 完成 27/27 定向测试、MES 相关编译、runtime guard 和运行时脚本回归；全链路仍受流程 4/6/7/8/9/10/11 的跨流程实现、历史迁移对账和真实读写 E2E 阻断，这些不属于流程 3 可单方面关闭的范围。
