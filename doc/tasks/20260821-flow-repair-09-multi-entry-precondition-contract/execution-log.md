# 执行日志

## 范围与已读合同

- 流程9自身修改了入口合同生产代码和 task-owned 合同测试；未修改数据库、配置或运行态数据。
- 未启动服务、未运行写入型 E2E；目标模块编译和目标测试已执行。
- 已读取项目规则和流程修复 6、7、8、10、11 任务文档。流程修复 10 目录存在，本文引用其“流程 10 消费已创建 batchExecutionId、流程 8 gate 和签名并唯一写入 RELEASED”的合同。
- 流程修复 6 的 canonical 独立凭证名称为 `IndependentBatchPrerequisiteReceipt`，本文已统一采用。

## 里程碑

- M1 PASS：读取规则与相邻流程合同。
- M2 PASS：核对活跃/排产/PQC/手工/独立入口及门禁分散事实。
- M3 PASS：冻结双场景 receipt、entryType 分流、source relation、幂等和批次/放行边界。
- M4 PASS：更新入口矩阵、放行矩阵、BDD、迁移/回滚和跨线程契约。
- M5 PASS：流程9入口合同代码、task-owned 测试和五份文档结构核验完成。
- M6 PASS：`477c97d41` 已 fast-forward 融合到 `int_main`；当前主线程 HEAD 为 `155c767d5`，目标编译 BUILD SUCCESS，目标测试 42/42 PASS。
- M7 PASS（2026-08-23）：最新 `int_main` HEAD=`b2f8e8356e1c6e27161147bb0d0d3802da3e848f`；确认 `477c97d41` 在祖先链，无重复融合。
- M8 IN PROGRESS（2026-08-23）：新增流程9受控 `IndependentBatchPrerequisiteReceipt` issue/verify/revoke 服务、REST 合同、持久化 DO/Mapper/SQL 迁移和 task-owned 合同测试；不修改流程6实现。
- M8 CLOSEOUT（2026-08-23）：保留主线 dirty/untracked，流程9 task-owned 文件单独收口。
- M9 PASS（2026-08-23）：在并行流程11提交 `ef217fe2c` 之后，以最新 `int_main` 为父节点提交流程9受控 receipt 生命周期，commit=`2cf830d7b`；commit hook 的 `branch-runtime-port-guard.ps1` 通过（8081/48081）。
- M10 PASS（2026-08-23）：复核跨租户验真/撤销时原先被租户过滤误报为缺凭证的问题；新增无租户范围 receipt 查询，仅用于返回稳定 `TENANT_MISMATCH`，不泄露凭证内容；commit=`656e343df`。

## BDD/TDD 记录

- `BDD: 活跃订单入口消费流程4 receipt -> Given/When/Then`：流程 9 不产生或修改 completion receipt，流程 6 拥有建批。
- `BDD: 独立 MANUAL/SCHEDULED/PQC_INDEPENDENT -> Given/When/Then`：有效 canonical receipt 和正式 source relation 才允许建批。
- `BDD: 场景混用/缺凭证/过期撤销阻断 -> Given/When/Then`：fail fast，无副作用。
- `BDD: 批次创建与最终放行分离 -> Given/When/Then`：流程 8 统一四材料 gate，流程 10 消费 batchExecutionId 并唯一写 RELEASED。
- `BDD: 独立来源追溯 -> Given/When/Then`：放行后保留真实 source IDs 和 receipt 链。
- `BDD: 独立凭证签发 -> Given/When/Then`：Given 受控后端用户、合法 entryType、租户和正式来源事实，When issue，Then 后端生成有效期、issuer、canonical payload/hash、HMAC 签名、审计事件并持久化；客户端字段不能覆盖这些值。
- `BDD: 独立凭证验真与篡改阻断 -> Given/When/Then`：Given receiptId，When verify，Then 服务端重新读取并校验 canonical/hash/signature/tenant/source snapshot/lifecycle；任一篡改或来源变化 fail fast。
- `BDD: 独立凭证过期/撤销/幂等 -> Given/When/Then`：Given 已过期、已撤销、重复相同幂等键或不同载荷重试，When verify/issue/revoke，Then 分别返回稳定失效、已撤销、原结果或冲突错误。

## 执行状态

- `RED: mvn -o -pl yudao-module-mes -Dtest=ScheduleApplierTest,MesBatchExecutionEntryContractTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseBatchExecutionPortTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test -> FAIL，先暴露独立/活跃凭证、损耗事实和入口映射夹具不一致。`
- `GREEN: mvn -o -pl yudao-module-mes -am -DskipTests compile -> PASS；目标四类测试 -> PASS，Tests run: 42, Failures: 0, Errors: 0。`
- `REGRESSION: NOT RUN ->` 流程11全链路、并发/迁移、四材料、最终放行和真实 E2E 仍未执行。
- `RED: mvn -o -pl yudao-module-mes '-Dtest=MesIndependentBatchPrerequisiteReceiptServiceTest' ... test -> FAIL（初次运行受主线流程7缺失类型阻断，非流程9错误）。`
- `GREEN: mvn -o -pl yudao-module-mes '-Dtest=MesIndependentBatchPrerequisiteReceiptServiceTest' '-Dsurefire.failIfNoSpecifiedTests=false' '-DforkCount=0' test -> PASS，Tests run: 4, Failures: 0, Errors: 0；覆盖签发、验真、篡改/来源变化、过期/撤销、跨租户和幂等边界。`
- `REGRESSION: NOT RUN ->` 数据库迁移、流程11全链路、流程8四材料、流程10最终放行和真实写入型 E2E 未执行。
- 最新主线程复核：MES compile `BUILD SUCCESS`；入口合同/PQC 联动/生产放行端口/排产 fail-fast 定向测试 `42/42 PASS`；receipt 专项测试 `4/4 PASS`；`git diff --check` 和 `branch-runtime-port-guard.ps1` 通过。

## 代码审计事实

- `ScheduleApplier.java:176-196`：排产缺项 warning/skip 与专用建批逻辑。
- `MesProAutoScheduleServiceImpl.java:419`：自动排产完成联动。
- `MesProEdhrBatchExecutionController.java:101-105`：页面 `open-or-create`。
- `MesPqcProductionReleaseServiceImpl.java:93-147`：PQC 批准路径先建批再写资料。
- `ErrorCodeConstants.java:1238-1243`：活跃订单双进度/正式来源/负责人错误合同。

## 状态边界结论

- 流程 4 唯一产生活跃 completionBackfillReceipt。
- 流程 9 只负责多入口分流、凭证前置、来源关系、幂等和适配。
- 流程 6 唯一拥有批次创建/复用状态。
- 流程 8 拥有四材料上传与硬门禁。
- 流程 10 唯一写最终 RELEASED。
- 流程 7 提供完整映射和放行后追溯；流程 11 负责总体验证和迁移门禁。

## 未实现项

- 流程9新增 receipt 服务代码已实现，但真实数据库迁移、配置密钥注入、流程6正式消费接线、流程8/10/11生产实现、历史盘点和真实 E2E 均未执行。
- 初次 receipt 测试曾被主线已有流程7未跟踪文件缺少 `MesProEdhrBatchTraceSourcePrecheckRespVO` 阻断，后续主线依赖状态恢复后已通过；该类型仍不属于流程9改动路径。
- 历史无正式凭证/source relation 只能保持 `BLOCKED_LEGACY`。

## 结论

流程9自身入口合同与独立 receipt issue/verify/revoke 实现已提交至 `int_main`（`2cf830d7b`、租户隔离修复 `656e343df`）并通过提交门禁；跨流程持久化运行、四材料 gate、最终放行和全链路迁移仍不能据此放行生产。
