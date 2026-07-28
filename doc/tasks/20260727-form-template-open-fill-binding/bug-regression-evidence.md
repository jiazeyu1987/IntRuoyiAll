# Bug Regression Evidence

## Bug Summary And Expected Behavior

表单中心/表单模板预览区域点击“打开”和“填写”时，页面提示“当前模板未绑定批记录表单，无法执行该操作”。期望行为是：通用 FormCenter 模板的“打开”进入本页模板查看弹窗，“填写”进入本页模拟填写弹窗，均不依赖批记录绑定。

## Reproduction Path

- 页面路径：`表单中心 > 表单模板`。
- 用户动作：选择未绑定批记录但已有模板布局的模板，分别点击“打开”和“填写”。
- 复现命令：`node tests/e2e/form-template-batch-record-button-alignment-static.spec.js`。

## Root Cause

`openSelectedTemplate` 调用了 `openSelectedTemplateDesigner('preview')`，`openSelectedTemplateFill` 调用了 `resolveSelectedTemplateBatchRecordBinding()` 并跳转批记录模板模拟页。两个流程都错误要求 `batchRecordBindingStatus === 'BOUND'` 且存在 `batchRecordReportId`，把通用 FormCenter 模板动作耦合到了批记录报表。

## Regression Test

更新 `IntRuoyiFronted/tests/e2e/form-template-batch-record-button-alignment-static.spec.js`，锁定：

- “打开”必须调用 `TemplateViewDialog`，不得进入批记录设计器。
- “填写”必须重置模拟值并打开本页填写弹窗，不得跳转批记录模拟填写页。
- “打开/编辑/填写”均不得调用批记录绑定校验。
- 表单模板页不得再包含“当前模板未绑定批记录表单”拦截文案。

## RED

RED: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> FAIL，失败断言：`表单模板“打开”必须进入本页模板查看弹窗，不得依赖批记录绑定`。

## GREEN

GREEN: `node tests/e2e/form-template-batch-record-button-alignment-static.spec.js` -> PASS。

## Verification

- `pnpm ts:check` -> PASS。
- `node tests/e2e/form-center-static.spec.js` -> FAIL，失败点为既有无关断言 `activeMenu: '/mdm/form-center/policy'`。

## Risk And Follow-up

风险较低：仅恢复页面已有的查看和模拟填写流程，未新增 fallback，未修改批记录表单列表自身的 `reportId` 前置条件。宽 FormCenter 静态合同的路由断言漂移应由独立任务处理。

## Blockers

无当前任务阻塞；存在无关宽回归失败，已按前端静态契约隔离门禁记录。
