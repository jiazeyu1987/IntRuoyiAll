# Verification Report

## Scope

- 验证本机 `48081` 后端已加载 `/admin-api/mes/pro/batch-record-report/production-batch-record/total-recognition-json`。
- 验证用户指定生产批记录 `.doc` 返回类似 `批记录总对应.json` 的批记录业务映射 JSON。
- 验证 admin 登录态可见 `表单解析` 菜单和 `form:parser:production-batch-record` 按钮权限。

## Results

- PASS: `48081` 当前监听 PID 63536，运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260908-220415.jar`，Jar SHA-256 `33CAC89B7B54512465A5AFC93769C17682B1FD554A40B7A0CDA51D4F01B38E20`，Jar 修改时间 `2026-09-08 22:04:13 +08:00` 早于 Java 进程启动时间 `2026-09-08 22:04:22 +08:00`。
- PASS: `GET http://127.0.0.1:48081/actuator/health` 返回 `status=UP`。
- PASS: 匿名 POST 目标接口返回业务码 `401 账号未登录`，证明当前运行态已加载 Controller mapping，不再返回 `请求地址不存在`。
- PASS: 管理员登录态上传 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc` 到目标接口，返回 HTTP 200 / `code=0`。
- PASS: 返回 `data` 可解析为业务映射 JSON，`schemaVersion=2`，产品为 `按压式球囊扩充压力泵` / `IDI-01`，工序数 15，首工序 `粗洗工序`，末工序 `大包装工序`。
- PASS: 返回 JSON 与 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json` 的产品和工序名序列一致，且不含 `jimuSchemaJson`、`sheetLayoutJson`、`cellRules`、`recognizedSchemaJson`。
- PASS: `GET /admin-api/system/auth/get-permission-info` 返回 `表单解析` 菜单，路径 `form-center/parser`，组件 `form-center/parser/index`，并包含 `form:parser:production-batch-record`。

## Command Evidence

- PASS: `mvn -pl yudao-module-signature -am "-Dtest=GxpAuditTrailServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，5 tests, failures 0, errors 0。
- PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，4 tests, failures 0, errors 0。
- PASS: `node tests\e2e\form-parser-json-download-static.spec.cjs` from `IntRuoyiFronted`，`form parser json download static contract passed`。
- PASS: `mvn -pl yudao-server -am -DskipTests package`，full reactor `BUILD SUCCESS`。
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260908-form-parser-48081-runtime-reload\bug-regression-evidence.md`，`Bug regression evidence is valid.`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-form-parser-48081-runtime-reload\backend-api-evidence.md`，`Backend API evidence is valid.`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-48081-runtime-reload --mode preview`，keep core task records; delete only task temporary artifact/evidence files; no blocked paths or warnings。
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-48081-runtime-reload --mode apply`，deleted only preview-approved task temporary files。

## Sensitive Data Handling

- 登录令牌、数据库密码、用户密码和连接串敏感值未写入本任务文档。

## Closeout Result

- PASS: 用户已在 2026-09-08 明确授权 Git 收尾；实现提交、授权基线提交、残留基线提交和任务收尾记录均按项目规则提交并推送到 `origin/int_main`。
