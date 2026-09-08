# Execution Log

## BDD Scenarios

BDD: 生产批记录 Word 可解析为 JSON -> Given 用户上传指定的生产批记录 Word 文件 / When 点击“表单解析 -> 生产批记录” / Then 后端使用与表单中心一致的 Word 解析链路生成包含 schema rows 的 JSON，并由前端下载 `.json` 文件。

BDD: schema 缺失明确失败 -> Given Word 解析结果没有正式 schema rows / When 执行 parse-only JSON / Then 后端返回明确错误，不生成默认 JSON 或空成功结果。

BDD: 生产批记录下载批记录总对应 JSON -> Given 用户上传指定的生产批记录 Word 文件 / When 点击“表单解析 -> 生产批记录” / Then 系统下载的 `.json` 内容是从该 Word 解析出的 `product/schemaVersion/processes` 批记录业务映射结构，格式对齐 `批记录总对应.json`，而不是 Jimu 表单版式 JSON。

BDD: 批记录总对应解析保持 parse-only -> Given 用户只在表单解析页上传 Word / When 后端解析生产批记录业务映射 JSON / Then 后端只复用 Word 表格解析和总识别提取，不创建 Jimu 报表、不写批记录版本、不写 DCC 项目编码。

## RED / GREEN Evidence

- RED: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest#recognizeLegacyDocProductionRecordBuildsVisualSchemaRows' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, legacy `.doc` recognition returned fields but `jimuSchemaJson` was `null`, matching the reported `Template schema rows are missing` service failure.
- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest#recognizeLegacyDocProductionRecordBuildsVisualSchemaRows' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 1 test, failures 0, errors 0.
- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterRuntimeServiceImplParseJsonTest,FormCenterRuntimeImportRecognitionFlowContractTest,FormCenterRuntimeContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 25 tests, failures 0, errors 0.
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, `form parser json download static contract passed`.
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260908-form-parser-template-schema-rows\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- GREEN: task-scoped `git diff --check` with `git add --intent-to-add -f` for new task files -> PASS, only CRLF normalization warnings.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, `parseProductionBatchRecordTotalRecognitionJson` does not exist on `MesProBatchRecordReportService` or `MesProBatchRecordReportController`.
- RED: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> FAIL, frontend lacks `parseProductionBatchRecordTotalRecognitionJson` and still targets the old Jimu JSON parser path.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, real `.doc` total-recognition output had leftover leading bracket in heat-sealing reference values, for example `(135±10`.
- RED: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> FAIL, service-level total-recognition JSON was `{}` because Fastjson did not serialize the Java record result.
- GREEN: `mvn -pl yudao-module-mes -am '-Dtest=MesProBatchRecordReportControllerTest#parseProductionBatchRecordTotalRecognitionJsonKeepsParseOnlyPermissionContract,MesProBatchRecordTotalRecognitionExtractorTest#extractRealIdiDocMatchesExpectedTotalRecognitionJson,MesProBatchRecordReportParseOnlyContractTest,MesProBatchRecordReportParseTotalRecognitionJsonTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 4 tests, failures 0, errors 0.
- GREEN: `node tests\e2e\form-parser-json-download-static.spec.cjs` -> PASS, `form parser json download static contract passed`.
- GREEN: `pnpm ts:check` -> PASS, process exit 0.
- GREEN: `mvn -pl yudao-module-bpm -am '-Dtest=DefaultWordFormTemplateRecognizerTest,FormCenterRuntimeServiceImplParseJsonTest,FormCenterRuntimeImportRecognitionFlowContractTest,FormCenterRuntimeContractTest' '-Dsurefire.failIfNoSpecifiedTests=false' test` -> PASS, 25 tests, failures 0, errors 0.
- GREEN: `python C:\Users\BJB110\.codex\skills\change-request-triage\scripts\validate_change_request.py --evidence docs\changes\20260908-form-parser-production-batch-record-mapping-json.md` -> PASS, `Change request evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260908-form-parser-template-schema-rows\backend-api-evidence.md` -> PASS, `Backend API evidence is valid.`
- GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260908-form-parser-template-schema-rows\bug-regression-evidence.md` -> PASS, `Bug regression evidence is valid.`
- GREEN: task-scoped `git diff --check` with `git add --intent-to-add -f` for new task files -> PASS, only CRLF normalization warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-template-schema-rows --mode preview` -> PASS, keep `task.md`, `execution-log.md`, `verification-report.md`; delete only `backend-api-evidence.md` and `bug-regression-evidence.md`; no blocked paths or warnings.
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id 20260908-form-parser-template-schema-rows --mode apply` -> PASS, deleted `backend-api-evidence.md` and `bug-regression-evidence.md`; no blocked paths or warnings.

## Work Notes

- 2026-09-08: 用户反馈解析时提示 `Template schema rows are missing`，并要求用指定 `.doc` 文件验证。
- 2026-09-08: 确认服务层一致性：表单中心正式导入 `importDoc` 与表单解析 `parseProductionBatchRecordJson` 都调用 `templateRecognizer.recognize(command)`，随后都通过 `requireRecognizedVisualSchema` 校验 schema rows。
- 2026-09-08: 根因定位：旧 `.doc` 分支只通过 `WordExtractor` 抽取字段 label 并返回 `FormTemplateRecognition.success(fields)`，未生成 `jimuSchemaJson`；因此与 `.docx` 分支的 visual schema 合同不一致。
- 2026-09-08: 修复范围：新增 HWPF `.doc` 表格 visual schema 构建器，并让 `.doc` 分支返回 `FormTemplateRecognition.success(fields, jimuSchemaJson)`；未新增 mock JSON、默认成功或降级分支。
- 2026-09-08: 真实文件验证使用 `E:\IntRuoyi\resource\按压式球囊扩充压力泵IDI-001\RE-PP-IDI-01（A 1） 按压式球囊扩充压力泵生产记录--2026.02.02生效.doc`，文件大小 950080 bytes。
- 2026-09-08: 服务层补充真实 `.doc` 回归：`parseProductionBatchRecordJsonParsesLegacyDocWithRealFormCenterRecognizer` 使用真实 recognizer 和真实文件，断言 parse-only 输出 `.json` 所需 `jimuSchemaJson`、`sheetLayoutJson` 与 `cellRules`，且不持久化模板版本、不提交审批。
- 2026-09-08: 按 project-experience-consolidation 技能，把 `.doc` 必须产出 visual schema 的复用经验合并到 `docs/system/shared-word-template-parser-design.md`。
- 2026-09-08: `task-closeout-cleanup` preview/apply 通过，只删除已归档到 `verification-report.md` 的 `bug-regression-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- 2026-09-08: 上一轮修复完成时未获 Git 提交/推送授权，且工作区存在无关脏改动；当时收尾记录停在 `ready_for_closeout`，未执行提交/推送，未标记 `completed`。
- 2026-09-08: 用户澄清生产批记录按钮当前下载的是 Jimu 表单 JSON，目标应是类似 `批记录总对应.json` 的 `product/schemaVersion/processes` 批记录总识别 JSON。
- 2026-09-08: 新增 MES parse-only 接口 `POST /mes/pro/batch-record-report/production-batch-record/total-recognition-json`，沿用 `form:parser:production-batch-record` 权限，只解析 Word 并返回 JSON 字符串，不创建 Jimu 报表、批记录版本、审批或 DCC 项目编码写入。
- 2026-09-08: 前端“表单解析 -> 生产批记录”按钮改调用 MES parse-only 接口，接收 JSON 字符串后用 `JSON.parse` 校验 `product/schemaVersion/processes`，再下载格式化后的 `.json` 文件；预览表格改为工序、关键/特殊、投入、产出和设备组统计。
- 2026-09-08: 真实 `.doc` 对齐 `批记录总对应.json` 时发现热合参数存在旧 Word 遗留前缀括号，已在参考值标准化处只剥离“括号后直接接数字”的噪声。
- 2026-09-08: 服务层总识别 JSON 原使用 Fastjson 序列化 Java record 会得到 `{}`，已改为 Jackson 序列化，并覆盖 `importPilotDoc`、parse-only 接口和 `recognizeUploadedRoute` 的总识别 JSON 生成点。
- 2026-09-08: 按 project-experience-consolidation 技能，把“批记录总对应 JSON 与 Jimu schema 是两种合同、Java record 总识别结果需 Jackson 序列化、parse-only 不写库”的复用经验合并到 `docs/system/shared-word-template-parser-design.md`。
- 2026-09-08: `task-closeout-cleanup` preview/apply 通过，只删除已归档到默认保留记录的 `backend-api-evidence.md` 和 `bug-regression-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`；因未获 Git 提交/推送授权，任务状态保持 `ready_for_closeout`。
