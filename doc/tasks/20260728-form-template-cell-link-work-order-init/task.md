# 表单模板链接入口生产工单与模板布局修复

## Task Goal

修复表单模板页签点击“链接”后的两类阻塞：前端把 `PRODUCTION_WORK_ORDER` 误当作普通表单报表查询；后端读取 Form Center 模板单元格时只识别字符串型 `sheetLayoutJson`，导致根布局 JSON 模板或仅有 `recognized_schema_json` 的模板报“批记录表单布局 JSON 无效：FORMTPL:32”。

## Milestones

- [completed] 复现并定位表单模板链接入口默认来源初始化错误
- [completed] 补充回归测试，先证明当前初始化会误查 `form-cells`
- [completed] 修复工作台初始化 sourceType 与默认来源同步
- [completed] 复现并定位 `FORMTPL:32` 表单模板布局 JSON 解析错误
- [completed] 补充后端回归测试，证明根布局 JSON 模板可作为正式布局来源
- [completed] 复现并定位真实 `FORMTPL:32` 数据中 `jimu_schema_json=NULL`、`recognized_schema_json=字段数组`
- [completed] 补充后端回归测试，证明识别字段数组可生成链接目标单元格
- [completed] 修复 Form Center 模板布局解析，支持正式保存形态和识别字段形态并保留无效布局 fail-fast
- [completed] 运行目标前后端回归验证并记录结果
- [completed] 使用标准本地重启脚本将 `48081` 切换到包含修复的新后端 Jar

## Expected Verification

- `node tests/e2e/form-template-cell-link-work-order-init-static.spec.js`
- `node tests/e2e/mes/batch-record-cell-link-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRecognizedSchemaWhenJimuSchemaMissing,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromRootLayoutJimuSchema,MesProBatchRecordCellLinkServiceImplTest#getFormCells_resolvesFormTemplateCellsFromJimuSchema" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `mvn -pl yudao-server -am "-DskipTests" package`
- `powershell.exe -NoProfile -ExecutionPolicy Bypass -File E:\IntRuoyi\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend -WorktreeName int_main`

## Current Status

ready_for_closeout

## Remaining Blockers

- 当前工作区存在大量非本任务脏改动；按项目提交规则，提交/推送前需要先处理脏工作区基线，但这些改动不属于本任务，未进行提交或推送。
- `48081` 已切换到新后端 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar`，PID `56272`，SHA256 `073AFE1D63B0D1C8F99847F68AB7E2916FCB090CA1DF720C63B58952D0B68903`。
- 新后端启动后曾返回 health `UP`，但后续登录态目标接口复验被当前本地 DB/Redis/Docker 运行态超时阻塞：`/actuator/health` 连续 3 次 20 秒超时，登录 API 超时，Docker 只读检查超时；未终止或重启无关依赖进程。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是，修复初始化状态不一致，并将 Form Center 模板正式布局形态与识别字段形态纳入后端解析契约；无布局或非法布局仍 fail-fast
- `是否存在临时补丁或绕过`：否

## 经验门禁

- 表单模板与批记录表单没有直接关系，表单模板按钮必须使用模板上下文，不得伪造 `reportId` 或绑定状态。
- 单元格链接生产工单字段必须同时证明字段可见、可选、选中态和保存载荷语义，不能只证明文字存在。
- Form Center 模板布局读取必须按正式 `jimuSchemaJson.sheetLayoutJson/layout/rows` 或 `recognizedSchemaJson` 字段数组形态解析，不能误走批记录 Jimu 报表来源，也不能吞掉真正无效的布局。
- 静态合同遇到宽回归失败时，应新增或运行聚焦当前缺陷的最小合同，避免改动无关断言。
