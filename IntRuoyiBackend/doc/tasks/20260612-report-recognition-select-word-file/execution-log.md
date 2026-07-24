# Execution Log: 六路识别上传 Word 路线解析

BDD: 上传 Word 文件按路线解析 -> Given 前端提交 `file` 和 `routeKey` When 后端收到 `POST /mes/pro/batch-record-report/recognize-uploaded` Then 服务应校验 Word 文件、定位路线识别器、按上传内容解析并保存生成报表。

BDD: 无效路线或文件必须失败 -> Given 上传文件为空、扩展名非法或 `routeKey` 不属于 A-F When 调用上传路线解析接口 Then 后端应返回现有业务错误，不写入报表元数据。

BDD: 解析结果异常不得写入半成品 -> Given 路线识别器抛错或结果不是目标模板数量 When 服务执行上传路线解析 Then 事务应回滚，不返回默认成功。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, Maven `testCompile` 被非本任务 `MesProEdhrBatchExecutionServiceTest`、`MesProRouteUseConfigServiceImplTest` 依赖缺失提前阻塞，目标测试未执行。

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/MesProEdhrBatchExecutionServiceTest.java,**/MesProRouteUseConfigServiceImplTest.java" test` -> FAIL, 当前 POM 未应用该排除参数，仍在同两个非本任务测试编译错误处停止。

GREEN: `mvn -pl yudao-module-mes -DskipTests compile` -> PASS, 后端主源码包含 `recognize-uploaded` controller/service 实现并通过编译。

BLOCKED: 最新 `mvn -pl yudao-module-mes -DskipTests compile` -> FAIL, 非本任务既有脏改动 `MesProEdhrBatchExecutionServiceImpl.java` / `EdhrBatchExecutionReviewTimelineRespVO.java` 编译不一致：缺 `TaskEvent.setBatchRecordSort(...)` 与 `resolvedGate` 变量；当前后端完整编译状态被该外部改动阻塞。

BLOCKED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-Dmaven.compiler.testExcludes=**/batchrecord/MesProEdhrBatchExecutionServiceTest.java;**/route/MesProRouteUseConfigServiceImplTest.java" test` -> FAIL, `testCompile` 仍被非本任务 `MesProEdhrBatchExecutionServiceTest`、`MesProRouteUseConfigServiceImplTest` 字段/方法不匹配阻塞，目标 JUnit 未能执行。

GREEN: `mvn -pl yudao-module-mes,yudao-module-erp,yudao-module-infra -DskipTests compile` -> PASS, 当前前后端提交前复核中，后端受影响模块主源码编译通过。

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, 33 tests passed，旧外部 `testCompile` 阻塞已解除。

GREEN: `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --check` -> PASS, 仅 LF/CRLF 提示。

GREEN: backend API evidence validation -> PASS, `validate_backend_api.py` 确认证据文件结构有效。

GREEN: task-closeout-cleanup preview -> PASS, `task.md`、`execution-log.md`、`backend-api-evidence.md` 全部保留，无待删项、无阻塞。
