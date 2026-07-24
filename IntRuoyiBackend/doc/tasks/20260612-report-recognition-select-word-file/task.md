# 20260612 六路识别上传 Word 路线解析

## 任务目标

为 `MES 电子批记录生成报表` 提供正式的上传路线解析接口，让前端 A-F 路线按钮提交用户选择的 `.doc` Word 文件和 `routeKey`，后端按对应识别器解析并保存生成报表。

## 里程碑

1. M1 审计：确认现有 `/recognize-fixed` 固定样本接口、`/import` 旧上传接口、路线识别器和上一个后端任务状态。
2. M2 RED：新增 controller/service 契约测试，要求暴露 `file + routeKey` 上传解析接口。
3. M3 GREEN：实现接口、服务方法、Word 文件校验、路线识别器调用和保存语义。
4. M4 REGRESSION：运行目标 Maven 测试。
5. M5 收尾：记录验证证据，运行收尾清理预览。

## 预期验证

- `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；上传路线解析不回退到固定样本，缺路线识别器、文件为空、扩展名非法或解析数量异常均 fail fast。
- `是否从根因和长期维护角度解决`：是；新增正式 API 契约，复用现有路线识别器和报表保存链路。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：COMPLETED。
- 已完成：确认后端上一个任务 `20260612-process-use-route-tabs` 已完成；新增 `POST /mes/pro/batch-record-report/recognize-uploaded`、service 契约、`.doc` 文件校验、路线识别器调用、上传文件哈希 sampleKey 和目标 controller/service 测试。
- 已验证：`mvn -pl yudao-module-mes,yudao-module-erp,yudao-module-infra -DskipTests compile` -> PASS；`mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordReportControllerTest,MesProBatchRecordReportServiceImplDbTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，33 tests passed；`git diff --check` -> PASS，仅 LF/CRLF 提示。
- 阻塞：无。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260612-report-recognition-select-word-file/backend-api-evidence.md
