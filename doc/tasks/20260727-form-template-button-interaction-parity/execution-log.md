# Execution Log

## User Intent

- 用户要求表单模板三个按钮按批记录管理的行为执行。
- 用户明确指出实际表单与批记录表单没有直接关系。
- 用户反馈上一轮只移除了错误提示，当前页面交互逻辑仍与批记录管理不同。

## BDD

BDD: 打开当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“打开”，Then 当前页面切换到只读模板工作区，且只携带表单模板上下文。

BDD: 编辑当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“编辑”，Then 当前页面切换到模板编辑工作区，且不要求批记录绑定。

BDD: 模拟填写当前表单模板 -> Given 用户在表单模板预览区选中模板，When 点击“填写”，Then 跳转到独立的表单模板模拟填写页面，页面从当前模板版本的 `jimuSchemaJson` 加载内容。

BDD: 保持领域独立 -> Given 任意普通表单模板未绑定批记录表单，When 用户执行三个按钮中的任意一个，Then 不读取或校验 `batchRecordReportId`、`batchRecordBindingStatus` 或批记录 `reportId`。

## Command Intent

- 只读检查批记录管理与表单模板三个按钮的源码流转。
- 新增聚焦静态合同锁定同页工作区、独立填写页和领域边界。
- 执行 RED/GREEN、类型检查和真实页面验证。

## Milestone Status

- Milestone 1: `in_progress`
- Milestone 2: `pending`
- Milestone 3: `pending`
- Milestone 4: `pending`
- Milestone 5: `pending`

## Evidence

- 当前批记录管理“打开/编辑”切换同页 `DesignerWrapper`，“填写”跳转独立 `template-simulate` 页面。
- 当前表单模板“打开/编辑/填写”分别使用三个弹窗，未完成交互行为对齐。
- RED: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> FAIL，缺少同页工作区路由模式。
- RED: `mvn -pl yudao-module-bpm -am "-Dtest=FormCenterTemplateVersionQueryTest,FormCenterRuntimeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，缺少 `getTemplateVersion` 精确查询方法。

## Blockers

- 当前工作区存在其他任务的未提交改动；本任务不得提交、覆盖或清理这些文件。
- 既有宽合同 `form-center-static.spec.js` 已知可能先失败于无关策略路由断言，本任务使用聚焦合同隔离验证。
