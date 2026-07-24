# Execution Log: eDHR 批记录附加表单槽位

BDD: 未上传显示红边上传 -> Given 已存在批记录名称，When 损耗单、过程检验单、参数记录表任一槽位未上传，Then 左侧显示红色边框上传按钮，点击可选择 Word 文件。

BDD: 上传后显示名称并预览 -> Given 用户在槽位上传合法 `.doc` 或 `.docx` Word 模板，When 后端解析并生成表单报表，Then 槽位显示解析出的表单名称，点击槽位在右侧显示对应表单。

BDD: 重复上传必须先删除 -> Given 某批记录名称的某槽位已经存在表单，When 用户再次上传同槽位模板，Then 后端拒绝并提示先删除，不覆盖旧表单。

BDD: 删除受绑定保护 -> Given 附加表单已经绑定到工艺路线工序或执行任务，When 用户点击删除，Then 后端 fail fast 返回绑定保护错误，不删除表单或元数据。

BDD: 工序按需绑定附加表单 -> Given 批记录名称下已上传 0-N 个附加表单，When 配置工艺路线用途，Then 每道工序可选择使用哪些已上传槽位，未上传槽位禁用但可见。

## Evidence

- BDD: 未上传显示红边上传 -> Given 已存在批记录名称，When 损耗单、过程检验单、参数记录表任一槽位未上传，Then 左侧显示红色边框上传按钮，点击可选择 Word 文件。
- BDD: 上传后显示名称并预览 -> Given 用户在槽位上传合法 `.doc` 或 `.docx` Word 模板，When 后端解析并生成表单报表，Then 槽位显示解析出的表单名称，点击槽位在右侧显示对应表单。
- BDD: 重复上传必须先删除 -> Given 某批记录名称的某槽位已经存在表单，When 用户再次上传同槽位模板，Then 后端拒绝并提示先删除，不覆盖旧表单。
- BDD: 删除受绑定保护 -> Given 附加表单已经绑定到工艺路线工序或执行任务，When 用户点击删除，Then 后端 fail fast 返回绑定保护错误，不删除表单或元数据。
- BDD: 工序按需绑定附加表单 -> Given 批记录名称下已上传 0-N 个附加表单，When 配置工艺路线用途，Then 每道工序可选择使用哪些已上传槽位，未上传槽位禁用但可见。
- RED: `mvn.cmd -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordReportControllerTest" test` -> FAIL, runtime/test schema missing `mes_pro_batch_record_execution.form_slot_type` / `archive_visibility` coverage during schema contract alignment.
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesBatchRecordBaseSchemaTest,MesProBatchRecordReportControllerTest" test` -> PASS, 7 tests / 0 failures / 0 errors.
- GREEN: `mvn.cmd -pl yudao-module-mes "-Dtest=MesProBatchRecordDocParserTest,MesProBatchRecordReportServiceImplDbTest,MesBatchRecordBaseSchemaTest,MesProBatchRecordReportControllerTest" test` -> PASS, 59 tests / 0 failures / 0 errors.
- GREEN: `NODE_OPTIONS=--max-old-space-size=8192 pnpm.cmd ts:check` -> PASS.
- GREEN: `node tests/e2e/edhr-extra-form-slots-static.spec.js; node tests/e2e/edhr-form-slot-frontend-static.spec.js` -> PASS, frontend slot static contracts passed.
- BLOCKER: real-browser-upload-e2e -> NOT RUN, 当前工作区存在大量历史脏改且本次验证未启动或重启真实前后端服务；未使用 mock 或接口绕过替代真实 E2E。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260707-batch-record-extra-form-slots --mode preview` -> PASS, keep `task.md` and `execution-log.md`, delete `<none>`, blocked `<none>`, warnings `<none>`.
- BLOCKER: git-commit -> NOT RUN, 后端与前端仓库在本任务开始前已存在多处同文件重叠脏改，无法安全只暂存本任务 hunk；为避免混入无关用户/历史改动，本轮不创建提交。
RED: git commit backend -> FAIL, commit hook required script/tests coverage for sql/mysql changes.
BDD: SQL release contract -> Given eDHR adds MAIN/PROCESS_INSPECTION/LOSS_REPORT/PARAMETER_RECORD form slots, When schema migrations are reviewed, Then report uniqueness includes form_slot_type, execution snapshots carry slot metadata, and migrations fail fast through explicit release contracts.
GREEN: node script/tests/test_edhr_extra_form_slots_sql.test.mjs -> PASS, SQL release contract covers form_slot_type uniqueness, slot metadata columns, legacy archive_visibility normalization, and report-backed slot backfill.
