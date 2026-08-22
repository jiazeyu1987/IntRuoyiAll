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

## BDD/TDD 记录

- `BDD: 活跃订单入口消费流程4 receipt -> Given/When/Then`：流程 9 不产生或修改 completion receipt，流程 6 拥有建批。
- `BDD: 独立 MANUAL/SCHEDULED/PQC_INDEPENDENT -> Given/When/Then`：有效 canonical receipt 和正式 source relation 才允许建批。
- `BDD: 场景混用/缺凭证/过期撤销阻断 -> Given/When/Then`：fail fast，无副作用。
- `BDD: 批次创建与最终放行分离 -> Given/When/Then`：流程 8 统一四材料 gate，流程 10 消费 batchExecutionId 并唯一写 RELEASED。
- `BDD: 独立来源追溯 -> Given/When/Then`：放行后保留真实 source IDs 和 receipt 链。

## 执行状态

- `RED: mvn -o -pl yudao-module-mes -Dtest=ScheduleApplierTest,MesBatchExecutionEntryContractTest,MesPqcReleaseBatchExecutionServiceTest,MesProductionReleaseBatchExecutionPortTest -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=0 test -> FAIL，先暴露独立/活跃凭证、损耗事实和入口映射夹具不一致。`
- `GREEN: mvn -o -pl yudao-module-mes -am -DskipTests compile -> PASS；目标四类测试 -> PASS，Tests run: 42, Failures: 0, Errors: 0。`
- `REGRESSION: NOT RUN ->` 流程11全链路、并发/迁移、四材料、最终放行和真实 E2E 仍未执行。

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

- 双 receipt 后端受控签发/有效期/撤销、流程 6/8/10/11 生产实现、数据库迁移、历史盘点和真实 E2E 均未执行。
- 历史无正式凭证/source relation 只能保持 `BLOCKED_LEGACY`。

## 结论

流程9自身入口合同实现完成并已在 `int_main` 验证；跨流程持久化、四材料 gate、最终放行和全链路迁移仍不能据此放行生产。
