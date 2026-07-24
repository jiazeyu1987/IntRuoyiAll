# Execution Log

- BDD: 标准化执行快照版本必须与设计文档一致 -> Given eDHR V1 运行态从 `batchRecordReport` 生成 `executionSnapshotJson` / When 后端创建新的上下文执行实例 / Then `snapshotVersion` 必须等于文档冻结值 `EDHR_EXECUTION_V1`，不能再返回数字版本号。
- BDD: 旧模板创建入口必须从公开 HTTP 面移除 -> Given eDHR V1 运行态只允许从真实生产上下文打开执行实例 / When 调用方检视 `MesProBatchRecordExecutionController` 的公开映射 / Then 不应再暴露 `/legacy-create-from-template`。
- RED: `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL, `snapshotVersion` 仍返回 `1`，且 controller 仍暴露 `/legacy-create-from-template`。
- GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- GREEN: `mvn --% -pl yudao-module-mes -am -Dtest=MesProBatchRecordExecutionServiceImplTest,MesProBatchRecordExecutionControllerTest,MesProBatchRecordExecutionSignatureServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- GREEN: `mvn --% -pl yudao-server -am -Dmaven.test.skip=true package` -> PASS
- GREEN: 真实 API 验证 -> PASS，`entry-context / open-or-create-by-context / get` 均可用，`snapshotVersion = EDHR_EXECUTION_V1`，且 `legacy-create-from-template` 已不再暴露。
