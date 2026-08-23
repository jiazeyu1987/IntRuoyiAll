# 流程修复 7：批次执行完整映射和放行后追溯

## Current Status

### Latest Authoritative Status (2026-08-23)

`partial / blocked` for the Flow7 implementation slice. The continuation added task-owned batch Origin/TraceLink/Manifest persistence, a formal Tx-C producer/outbox path, and service contracts, but it does not claim completion of the full cross-flow workflow. Main-workspace Maven 3.9.16 compile, testCompile and the focused 29 tests passed (validator 17 + service contract 12, zero failures/errors, `BUILD SUCCESS`). The linked-worktree ACL failure is historical context only. Real upstream receipt adapters/owners, database migration/runtime/permissions, Flow8 four-material gate, Flow10 final `RELEASED`, full regression, service startup and write-enabled E2E remain `NOT RUN`/blocked.

M5 (implementation slice and verification) is `partial / blocked`; its focused compile/tests pass, while full cross-flow delivery remains blocked. M1-M4 below are historical design/document milestones and their earlier `completed` labels do not mean production delivery.

Git 交付已完成 task-owned 选择性提交：`0767b1fa5`。提交包含 Flow7 DTO/API、Tx-C producer、Origin/TraceLink/Manifest/outbox 持久化、迁移 SQL、定向测试和任务文档；未使用 `git add -A`，未混入流程9或其它 dirty/untracked。提交后标准 Maven compile 与未跳过 testResources 的 29 项定向测试均通过。

### 主流程统一冻结合同（2026-08-22）

流程7只拥有批次执行完整映射、统一 trace graph 和放行后追溯；receipt 由流程4拥有，`BATCH_*` 由流程6拥有。独立批次无 activeOrderId 时追溯返回 `relationStatus=NOT_APPLICABLE` 及原因码；应存在关系缺失返回 `MISSING/BLOCKED` 并阻止放行。历史迁移先 dry-run，缺 receipt、绑定、hash、损耗决策或关系分类 `INCOMPLETE_OR_AMBIGUOUS`，已放行但来源不全分类 `ALREADY_RELEASED_REVIEW_REQUIRED`。

partial / blocked

流程9入口类型已与本任务统一为 `PQC_INDEPENDENT`、`MANUAL`、`SCHEDULED`；活跃订单建批仍以流程4 completionBackfillReceipt、流程1 pickListBindingId/sourceSnapshotHash 和流程6 batch provision 成功结果为核心，不要求尚未发生的 releaseApplicationId。

## 任务目标

为后续实现定义批次执行、活跃订单、生产工单、正式领料单及分录、一线生产、一线 PQC、损耗、回填资料和放行决定之间的不可篡改映射。目标流程为：生产组长把生产工单及对应领料单绑定到活跃订单；一线生产与一线 PQC 提交签名事实，组长只复核事实；双进度均为 100% 后由生产组长点击完成；同一完成事务回填批记录、过程检验单，且仅在实际有损耗时回填损耗单；三类回填成功后创建或复用批次执行；批次执行上传来料检报告、灭菌报告、成品检报告、成品检记录四份材料；材料齐全后才可放行；放行后可追溯所有来源。

原始任务约束为只审计和设计；本次用户明确要求的 continuation 仅验证并整理流程7 task-owned 实现，仍不启动服务、写入数据库或运行写入型 E2E。

## 里程碑

| 里程碑 | 状态 | 交付 |
| --- | --- | --- |
| M1 规则与现状审计 | completed | 正式来源规则、经验门禁、现有模型和接口事实 |
| M2 目标态设计 | completed | 关系、状态、接口、权限、幂等和迁移设计 |
| M3 BDD/TDD 与验收计划 | completed | 可执行 RED/GREEN/REGRESSION 计划 |
| M4 文档结构验证 | completed | 五份必需文档及必填章节 |
| M5 流程7实现切片与验证 | partial / blocked | Origin/TraceLink/Manifest/Tx-C producer/outbox/API/SQL/测试切片；29项定向证据；完整链路 blocker |

## 预期验证

- 文档覆盖目标态、当前代码事实、根因、修改边界、接口/数据/状态设计、BDD、RED/GREEN/REGRESSION、blocker、迁移/回滚以及流程修复 1、2、3、4、5、6、7、8、9、10、11 的接口契约。
- 实施前必须以 test-plan.md 的 RED 顺序证明现有契约不足，再以 GREEN 和回归证明所有来源关系、文件齐套和放行权限。

## 当前代码符合性结论

不符合目标态，不能直接作为完整追溯与放行依据：

1. 批次执行当前只持有生产工单、批号、路线、状态与聚合 hash；没有活跃订单、领料单/分录、生产/PQC/损耗事实、申请或完成交易的一等关系。
2. 申请记录虽有 activeOrderId 与 batchExecutionId，但批次到订单只能经申请反查；没有批次层强制关联，更没有领料分录及签名/损耗来源细目。
3. 批次复用身份是工单、批号、路线，不能区分同工单同批号下的不同活跃订单完成申请，也不能证明重复请求属于同一完成事务。
4. 资料回填规划不读取或验证正式领料单及分录，不能满足工单与领料单正式对应的来源规则。
5. 当前资料规划存在零损耗报告规则，与本目标的无实际损耗不得生成损耗单相冲突；这是实施 blocker。
6. 原设计错误地把 releaseApplicationId 当作活跃订单建批必填，并把流程4/5/6/9 owner 错位；正确边界是流程4 Tx-A completionBackfillReceipt、流程6 Tx-B batch provision、流程7在 batchExecutionId 返回后后继建图，releaseApplicationId 只可在流程10后续放行确实发生时追加。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。任何来源、hash、权限、状态或文件缺失均返回结构化 blocker。
- 是否从根因和长期维护角度解决：是。使用来源关系和冻结快照，不从当前人员、当前配置、名称、formBindings、默认 MAIN 或旧资料推断。
- 是否存在临时补丁或绕过：否。旧批次不自动认领、补写或删除；仅在有正式完成/回填或对应入口来源凭证、幂等键时按受控迁移计划关联。

## 适用经验门禁

- experience-index 的 218、220、223、224、232、264、265、266、368 条：批记录正式绑定、领料单正式来源、批次任务快照隔离、申请放行统一回填、放行负责人和真实 E2E 前置。
- backend-development 的活跃订单申请放行资料必须只使用正式来源章节：双 100%、完成节点统一回填、三类回填后建批次、四份材料齐套、管理者代表放行和历史数据门禁。

## 关联文档

- development-plan.md
- test-plan.md
- execution-log.md
- verification-report.md

## Cleanup Keep

- doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/development-plan.md
- doc/tasks/20260821-flow-repair-07-batch-traceability-mapping/test-plan.md
