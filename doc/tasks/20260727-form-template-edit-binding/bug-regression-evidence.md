# Bug Regression Evidence

## Bug Summary And Expected Behavior

表单中心/表单模板预览区域点击“编辑”时，页面弹出“当前模板未绑定批记录表单，无法执行该操作”。期望行为是：模板编辑进入本页已有的规则编辑流程；只有批记录预览/填写这类确实依赖批记录绑定的操作才执行绑定关系 fail-fast 校验。

## Reproduction Path

- 页面路径：`表单中心 > 表单模板`。
- 用户动作：选择截图中的“过程检验记录”模板，点击预览区顶部“编辑”。
- 复现命令：`node tests/e2e/form-template-batch-record-button-alignment-static.spec.js`。

## Root Cause

`editSelectedTemplate` 误调用 `openSelectedTemplateDesigner('edit')`。该函数无论预览还是编辑都会先调用 `resolveSelectedTemplateBatchRecordBinding()`，因此普通 FormCenter 模板缺少 `batchRecordReportId` 或 `batchRecordBindingStatus !== 'BOUND'` 时会被批记录绑定校验拦截。

## Regression Test

更新 `IntRuoyiFronted/tests/e2e/form-template-batch-record-button-alignment-static.spec.js`，锁定：

- “编辑”必须调用 `openSelectedTemplateAction('edit')`。
- “编辑”不得调用 `openSelectedTemplateDesigner('edit')`。
- 批记录预览/填写仍保留绑定缺失 fail-fast 文案和 `BOUND + reportId` 校验。

## RED

RED: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> FAIL，失败断言：`编辑按钮不得进入批记录设计器编辑路径`。

## GREEN

GREEN: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> PASS。

## Verification

- `pnpm ts:check` -> PASS。
- `node tests/e2e/form-center-static.spec.js` -> FAIL，失败点为无关既有断言 `activeMenu: '/mdm/form-center/policy'`，未由本任务改动引入。

## Risk And Follow-up

风险较低：实现只改动编辑按钮目标函数，未改变打开/填写/下载/发布等其它动作。后续可另立任务处理宽 FormCenter 静态合同中的 `activeMenu` 断言漂移。

## Blockers

无当前任务阻塞；存在无关宽回归失败，已按前端静态契约隔离门禁记录。
