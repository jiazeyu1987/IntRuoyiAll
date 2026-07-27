# Execution Log

## Intent

用户要求按“表单模板必须按批记录表单的 3 个按钮行为对齐”进行文档设计。

## Evidence Reviewed

- `IntRuoyiFronted/src/views/form-center/template/index.vue`：表单模板预览区按钮当前绑定 `openSelectedTemplate`、`editSelectedTemplate`、`openSelectedTemplateFill`。
- `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/index.vue`：批记录表单按钮当前绑定 `openDesigner(reportId, 'preview')`、`openDesigner(reportId, 'edit')`、`openSimulate(row)`。
- `IntRuoyiFronted/src/api/mes/pro/batchrecordreport/index.ts`：批记录设计器路径、编辑路径、单元格规则均以 `reportId` 为核心参数。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterTemplateRespVO.java`：表单模板响应当前没有 `reportId` 字段。
- `IntRuoyiBackend/yudao-module-bpm/.../FormCenterRuntimeServiceImpl.java`：模板池从 `bpm_form_template_version` 转换为响应对象。
- `IntRuoyiBackend/yudao-module-mes/.../MesProBatchRecordReportServiceImpl.java`：批记录分页已支持 `reportId` 精确过滤。

## BDD Scenarios

- `BDD: 表单模板打开按钮对齐批记录打开 -> Given 表单模板行已绑定批记录 reportId / When 用户点击表单模板预览区“打开” / Then 前端进入批记录表单同源预览路径并请求 designer-path，不再打开 TemplateViewDialog。`
- `BDD: 表单模板编辑按钮对齐批记录编辑 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“编辑” / Then 前端进入批记录设计器编辑路径并请求 edit-path，不再打开本页规则编辑弹窗或保存 form-center jimu-schema。`
- `BDD: 表单模板填写按钮对齐批记录填写 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“填写” / Then 前端跳转批记录模板模拟填写页，并携带 reportId/reportName/batchRecordName/returnTo。`
- `BDD: 缺少 reportId 必须 fail fast -> Given 表单模板行没有稳定批记录 reportId / When 用户点击打开、编辑或填写 / Then 页面显示明确阻塞原因，不调用旧弹窗、不猜测报表、不返回默认成功。`

## RED / GREEN Notes

- `RED: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> FAIL, FormTemplateListItemVO 缺少 batchRecordReportId 等显式绑定字段，三按钮仍使用旧弹窗/本页编辑/本页模拟填写。`
- `RED: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> FAIL, 缺少 IntRuoyiBackend/sql/mysql/20260727_bpm_form_template_batch_record_binding.sql。`
- `RED: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, FormCenterTemplateRespVO/FormTemplateVersionDO 缺少批记录绑定字段，runtime 未映射。`
- `GREEN: documentation-structure -> PASS, 任务级设计文档已覆盖前端、后端 API、数据模型、配置安全部署四个维度。`
- `GREEN: python -X utf8 section-check -> PASS, 任务级设计文档 UTF-8 可读，四份设计文档均包含系统设计必备章节。`
- `GREEN: git diff --check -- doc\tasks\20260727-form-template-button-alignment-design -> PASS, 任务文档无 diff 空白错误。`
- `GREEN: python -m pytest script\tests\test_form_template_batch_record_binding_sql.py -> PASS, SQL 迁移契约 3 项通过。`
- `GREEN: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> PASS, 表单模板三按钮静态合同通过。`
- `GREEN: mvn.cmd -pl yudao-module-bpm "-Dtest=FormCenterTemplateBatchRecordBindingContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, BPM 合同测试 3 项通过。`
- `GREEN: pnpm ts:check -> PASS, 前端 relaxed TypeScript 检查通过。`
- `GREEN: frontend/backend/database evidence validators -> PASS, 三份技能证据文档校验通过。`
- `GREEN: python -X utf8 docs read -> PASS, 任务目录 Markdown 均可 UTF-8 读取。`
- `GREEN: task-owned trailing whitespace scan -> PASS, 本任务新增/修改文件均无尾随空白。`

## Milestone Updates

- 现状核对完成：确认三按钮当前不一致。
- 根因定位完成：表单模板响应缺少稳定 `reportId`，不能安全直接复用批记录按钮链路。
- 设计完成：要求后端先暴露正式映射，前端再复用批记录三按钮路由与接口。
- 实现完成：`bpm_form_template_version` 新增显式绑定摘要字段，模板池响应映射这些字段；表单模板 `打开 / 编辑 / 填写` 改为使用 `batchRecordReportId` 进入批记录表单设计器预览、编辑和模板模拟填写页。

## Blockers

- 当前工作区已有本地提交领先 `origin/int_main`，且存在非本任务 MES 脏改动；验证后分支显示领先 `origin/int_main` 9 个提交。本任务暂不执行提交/推送，避免混入无关任务状态。
- 真实页面 E2E 尚未执行；需要运行态存在已绑定 `batchRecordReportId` 的表单模板数据后再验收三按钮真实点击路径。
