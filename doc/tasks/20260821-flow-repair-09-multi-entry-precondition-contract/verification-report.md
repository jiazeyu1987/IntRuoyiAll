# 验证报告

## 结论

流程9自身代码符合性 `IMPLEMENTED / VERIFIED`，跨流程全链路符合性 `PARTIAL`。本轮新增受控独立凭证 issue/verify/revoke、持久化结构和迁移；数据库、密钥配置、流程6消费接线、服务启动和写入型 E2E 未执行。

## 验收逐条关闭

1. **流程修复 10 合同：已关闭（文档）**。已读取并引用现有 `20260821-flow-repair-10-final-release-state-and-trace` 合同：流程 10 消费已创建/复用的 `batchExecutionId`、流程 8 四材料 gate 和签名，并唯一写入最终 `RELEASED`；不再写“目录缺失”或将其列为 blocker。
2. **canonical 独立凭证：已关闭（文档）**。所有文档统一使用 `IndependentBatchPrerequisiteReceipt`，冻结 `receiptId`、`tenantId`、`entryType`（`MANUAL/SCHEDULED/PQC_INDEPENDENT`）、工单 ID/code、路线 ID/version、批号、正式 source relation/source IDs、`sourceSnapshotHash`、业务理由、`issuerSystem`、`issuerUserId`、`issuerUserRole`、`issuedAt`、`expiresAt`、撤销字段、`credentialVersion`、`payloadHash`、签名/审计事件和幂等键。有效期由后端按 entryType 生成。
3. **PQC_INDEPENDENT：已关闭（文档）**。有效 canonical receipt + 正式 source relation 是独立 PQC 调用流程 6 的必要条件；缺凭证阻断。活跃订单 PQC 只能消费流程 4 receipt，不能产生回填或先建批。
4. **状态边界：已关闭（文档）**。流程 4 唯一产生 completion receipt；流程 6 唯一拥有批次创建/复用；流程 8 拥有四材料上传/gate；流程 10 唯一写 RELEASED；流程 7 提供完整映射/放行后追溯；流程 11 负责 BDD/TDD/回归/迁移总门禁；流程 9 只做分流和前置合同。
5. **活跃 receipt 消费语义：已关闭（文档）**。排产、活跃 PQC、手工重试均明确为流程 4 receipt 的消费方，不得创建、修改或重新回填。
6. **场景条件化：已关闭（文档）**。活跃链路才要求 activeOrderId、领料绑定和 completion receipt；合法独立批次可无 activeOrderId，使用 canonical 独立 receipt 和正式来源关系；两类均统一走流程 8 gate 和流程 10 最终放行。
7. **建批/放行分离：已关闭（文档）**。流程 9 将建批请求交给流程 6；流程 10 只消费已创建/复用的 batchExecutionId，不把放行命令当建批命令。
8. **签发与 blocker：已关闭（文档）**。独立 receipt 明确后端受控签发、签发系统/用户/角色、有效期、撤销、签名和审计字段；删除泛化的业务确认 blocker，仅保留未实现代码、迁移和历史数据审查 blocker。
9. **BDD/迁移/回滚：已关闭（文档）**。新增独立三 entryType、无凭证、场景混用、生命周期失效、建批/放行分离、四材料、流程 10 最终放行、真实来源追溯和 `BLOCKED_LEGACY` 场景，迁移/回滚边界已同步。
10. **RED/GREEN/REGRESSION：已关闭**。RED 记录了合同测试先失败的夹具问题，GREEN 记录目标编译和 42/42 测试通过，跨流程回归仍明确 `NOT RUN`。
11. **独立凭证后端合同：已实现并验证**。`MesIndependentBatchPrerequisiteReceiptServiceImpl` 按固定 canonical 字段生成 SHA-256/HMAC-SHA256，重新读取持久化行验真并支持撤销；REST 入口只接受 receiptId 或事实字段，不接受完整可信凭证。新增服务测试 `4/4 PASS`，并覆盖跨租户稳定 `TENANT_MISMATCH`。

## 主线程证据

- Flow9 基础入口合同 commit：`477c97d41 feat: enforce flow9 multi-entry batch preconditions`。
- Flow9 独立 receipt 生命周期 commit：`2cf830d7b feat(flow9): add controlled independent receipt lifecycle`，父节点为并行流程11提交 `ef217fe2c`；当前 `int_main` 已包含该提交，不重复融合旧 worktree。
- Flow9 租户隔离修复 commit：`656e343df fix(flow9): preserve tenant mismatch for receipt lifecycle`；当前 `int_main` 已包含该提交。
- 编译：`mvn -o -pl yudao-module-mes -am -DskipTests compile` -> `BUILD SUCCESS`。
- 目标测试：`ScheduleApplierTest, MesBatchExecutionEntryContractTest, MesPqcReleaseBatchExecutionServiceTest, MesProductionReleaseBatchExecutionPortTest` -> `Tests run: 42, Failures: 0, Errors: 0`。
- `git diff --check` -> 通过；`branch-runtime-port-guard.ps1` -> 通过（int_main: 8081/48081）。
- 最新主线复核（2026-08-23）保持上述 compile、42 项定向测试、diff-check 和 runtime guard 结果；流程9专项完成，流程6/7/8/10/11 全链路仍不属于流程9交付范围。
- 最终主线程 HEAD=`40118d79e28d09aaba85cc88ea44a35c482be4ba`；`477c97d41`、`2cf830d7b`、`656e343df` 均可由 `git merge-base --is-ancestor <commit> HEAD` 证明已在祖先链。
- 本轮新增验证：`mvn -o -pl yudao-module-mes '-Dtest=MesIndependentBatchPrerequisiteReceiptServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test` -> `BUILD SUCCESS`，`Tests run: 4, Failures: 0, Errors: 0`；新增 SQL/API evidence 已静态核对，真实迁移 NOT RUN。
- 本轮以 `int_main` 最新 HEAD=`ef217fe2ca8887e5b4242d0823f203179d6b059e` 为父节点创建 `2cf830d7b`，随后创建 `656e343df` 修复跨租户错误码；未覆盖主线其它 dirty/untracked 文件。

## 已读取合同证据

- 流程修复 6 `task.md:60,123` 和 `development-plan.md:46-50`：canonical 独立 receipt、PQC 分流和统一建批。
- 流程修复 10 `development-plan.md`：流程 10 唯一最终化、消费流程 8 四材料 manifest 和既有批次身份。
- 本任务 `development-plan.md`：双凭证入口矩阵、批次/放行分离、跨线程 owner。

## 未解决 blocker

- 流程6正式消费 `receiptId`/验证结果的接线和跨入口复用仍由流程6负责；本轮已提供流程9后端签发、持久化、验真和撤销接口，但密钥配置和运行态迁移尚未执行。
- 流程8四材料 gate、流程10最终 RELEASED、流程11全链路/迁移/真实 Playwright 路径均未执行。
- 无正式 receipt/source relation 的历史批次只能保持 `BLOCKED_LEGACY`，不得猜测认领。

## 状态

流程9自身任务：`completed`（入口合同与受控 receipt 生命周期已提交，42项入口回归与4项 receipt 专项测试通过；最终主线程 HEAD=`40118d79e`）。跨流程生产闭环：`PARTIAL / BLOCKED`，不得据本文档批准上线。
