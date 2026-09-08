# Verification Report

## Scope

验证“表单解析 -> 生产批记录”上传真实旧版 `.doc` 后：

- 不再因缺少 visual schema rows 报 `Template schema rows are missing`。
- 下载的 `.json` 内容改为生产批记录业务映射 JSON，顶层为 `product/schemaVersion/processes`，语义对齐 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\批记录总对应.json`。
- 新增 parse-only 接口不写 Jimu 报表、批记录版本、DCC 项目编码或设备同步状态。

## Result

PASS

## Evidence

- BPM `.doc` 分支已生成 `FormTemplateRecognition` visual schema，包含 `sheetLayoutJson.rows` 与 `cellRules`，原 `Template schema rows are missing` 回归已覆盖。
- MES 新增 `POST /mes/pro/batch-record-report/production-batch-record/total-recognition-json`，权限为 `form:parser:production-batch-record`，返回 `CommonResult<String>`；`data` 是批记录总识别 JSON 字符串。
- 前端“生产批记录”按钮改调用 `BatchRecordReportApi.parseProductionBatchRecordTotalRecognitionJson(file)`，用 `JSON.parse` 校验 `product/schemaVersion/processes` 后下载格式化 `.json`。
- 真实 `.doc` 文件已通过 service 级测试，输出 JSON 与 `批记录总对应.json` 语义等价。
- 旧 Word 热合参数前缀括号已标准化，`135±10`、`1.8±0.5` 等值能识别为 numeric UI。
- 总识别 JSON 生成点已从 Fastjson record 序列化改为 Jackson 序列化，避免返回 `{}`。
- `backend-api-evidence.md` 和 `bug-regression-evidence.md` 均通过对应 validator；核心结论已归档到本报告。

## Commands

- `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterRuntimeServiceImplParseJsonTest,FormCenterRuntimeImportRecognitionFlowContractTest,FormCenterRuntimeContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 25 tests, failures 0, errors 0.
- `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 4 tests, failures 0, errors 0.
- `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, `form parser json download static contract passed`.
- `pnpm ts:check` -> PASS, process exit 0.
- `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260908-form-parser-production-batch-record-mapping-json.md` -> PASS, `Change request evidence is valid.`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-form-parser-template-schema-rows\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260908-form-parser-template-schema-rows\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- task-scoped `git diff --check` with `git add --intent-to-add -f` for new task files -> PASS, only CRLF normalization warnings.

## Not Run

未执行真实浏览器 E2E，也未重启 `int_main` 后端服务；项目规则要求 E2E 与服务重启需当轮明确授权，本轮未获得该授权。

未执行 Git 提交/推送；项目规则要求当轮明确授权后才能执行 Git 提交/推送，本轮未获得该授权。
