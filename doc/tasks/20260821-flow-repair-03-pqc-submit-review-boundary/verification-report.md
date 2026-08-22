# Verification Report

## Scope

本报告验证流程修复 3 独立 worktree 中的实现变更、设计文档和可执行验证边界。Maven 已可用，Flow3 定向测试已通过；主分支指针已受保护快进到集成提交，但主线程等价复验被非 task-owned ERP 接口漂移阻断。

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

## Documentation Verification

| Check | Result | Evidence |
| --- | --- | --- |
| 独立任务目录存在 | PASS | task directory present |
| 五个要求文档存在 | PASS | five required files present；另含授权新增的 `prd.md`、`task-state.json`、`test-report.md` |
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

## Code Compliance Conclusion

- 当前结构化来源模型总体符合专项目标：PQC 任务、逐件明细、检验项目、设备快照、复核和汇集明细均有正式字段与事务校验。
- 当前组长批准会推进来源为 `CONFIRMED` 并生成 aggregate detail；该行为可保留为“来源事实确认”，但必须与正式过程检验单回填、批次执行创建、材料齐套和最终放行彻底区分。
- 未发现本次审计范围内由一线 PQC 提交或组长复核直接创建正式过程检验单的证据。
- 业务语义、字段身份、状态 owner、终态/受控修订、两个建批分支、四份材料与禁止替代规则已在五份文档冻结；具体 DTO/事件名称允许实现统一映射。
- 当前仍不具备生产放行结论：生产代码尚未完整实现/验证终态复核、唯一 aggregate、流程 4/6/7/8/9/10 合同、多入口统一门禁和迁移对账。
- 本轮已实现 P1 来源回执身份字段与重复复核阻断：`sourceRevision` 由现有 `submittedEventId` 承载，`payloadHash` 使用冻结 `submittedContentHash`；未引入数据库迁移。

## Explicitly Not Run

- Production code changes: `DONE IN TASK-OWNED COMMIT`；`int_main` 已指向 cherry-pick 等价集成提交，原主工作树未被覆盖
- Database/schema/data operations: `NOT DONE`
- Service start/restart: `NOT DONE`
- Main-code Maven compile: `PASS`；命令使用 `-Dmaven.test.skip=true`，不代表测试通过
- Targeted Flow3 test command: `PASS`，27 tests，0 failures、0 errors；全量 reactor 和真实 E2E 未运行
- Maven/Node production tests: Flow3 focused Maven tests `PASS`；全量 reactor、Node tests 和生产 E2E `NOT RUN`
- Playwright/read-write E2E: `NOT RUN`（计划项，未执行）
- Git commit/merge: task-owned commit `d809c9995`、集成提交 `aeb58c37d`、收尾文档提交 `8b8ed148c` 及其集成等价提交 `f1377d1b0` 已完成；普通 merge 被同名未跟踪任务文档拒绝，随后以旧值校验的原子 fast-forward ref 更新完成分支指针融合。本报告收尾证据写入前 `int_main=f1377d1b0`。

## Unresolved Blockers

1. 终态普通复核幂等/冲突和聚合异常传播的代码及合同测试已补齐，Flow3 定向测试已执行通过。
2. 流程 4 的 `aggregateVersionId -> formalProcessInspectionDocumentId`、流程 6 的 `completionBackfillReceipt`/`IndependentBatchPrerequisiteReceipt` 互斥分支、流程 7 pre-release/过程检验映射及流程 8/10 硬门禁尚未完成生产实现和验证；生产测试必须断言已冻结的 receipt/trace/release 稳定码。
3. 批次详情、PQC/生产申请、管理者代表批准、独立批次放行等入口尚未通过真实 E2E 证明只能适配统一 gate/finalization。
4. 历史确认/汇集/正式单据/批次执行不能完整对账的记录仍缺迁移证据，必须作为 migration blocker，不得自动猜测或复用。
5. 四份材料和状态 owner 已冻结；旧产品文档过时材料口径仅为待修订文档/历史兼容项。跨线程载体名可调整，但不得保留 canonical receipt 别名或弱化稳定错误码。
6. 主线程等价 Maven 复验被非 task-owned ERP/MES 接口漂移阻断；普通 merge 的工作树更新被同名未跟踪任务文档保护性拒绝，未覆盖用户文件。

## Final Result

blocked：流程 3 P1/P2 最小代码与测试改动已提交（`d809c9995`），集成提交（`aeb58c37d`）已受保护快进到 `int_main`，Flow3 定向测试 27/27 PASS；主线程等价编译/回归因非 task-owned ERP 接口漂移阻断，真实 E2E 和迁移仍未完成。
