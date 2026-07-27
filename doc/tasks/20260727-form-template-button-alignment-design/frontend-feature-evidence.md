# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 表单模板红框 `打开 / 编辑 / 填写` 三个按钮按批记录表单行为对齐，全部使用后端返回的稳定 `batchRecordReportId`。
- Non-goal: 不重构表单模板本页旧预览/规则/模拟填写组件，不新增 mock 数据，不做视觉 redesign。

## Requirements And Acceptance

- `FT-BATCH-BUTTON-001`: `打开` 跳转 `/mes/pro/batch-record-form-list?mode=designer&reportMode=preview&reportId=<id>`。
- `FT-BATCH-BUTTON-002`: `编辑` 跳转 `/mes/pro/batch-record-form-list?mode=designer&reportMode=edit&reportId=<id>`。
- `FT-BATCH-BUTTON-003`: `填写` 跳转 `/mes/pro/feedback/edhr-batch-execution/template-simulate` 并携带 `reportId`。
- `FT-BATCH-BUTTON-004`: 缺少绑定或绑定状态异常时提示 `当前模板未绑定批记录表单`，不回退旧弹窗。

## UI Entry Points And Owned Files

- Entry: `IntRuoyiFronted/src/views/form-center/template/index.vue` 红框按钮。
- API type: `IntRuoyiFronted/src/api/form-center/template.ts` 的 `FormTemplateListItemVO`。
- Static contract: `IntRuoyiFronted/tests/e2e/form-template-batch-record-button-alignment-static.spec.js`。

## API Contracts And Data States

- `FormTemplateListItemVO` 新增 `batchRecordReportId/reportName/batchRecordName/versionNo/formSlotType/bindingStatus/bindingError`。
- `batchRecordReportId` 为空或 `batchRecordBindingStatus` 非 `BOUND` 时 fail fast。
- 不按模板名、源文件名或版本号推断批记录报表。

## BDD Scenarios

- `BDD: 表单模板打开按钮对齐批记录打开 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“打开” / Then 进入批记录表单设计器 preview 路由。`
- `BDD: 表单模板编辑按钮对齐批记录编辑 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“编辑” / Then 进入批记录表单设计器 edit 路由。`
- `BDD: 表单模板填写按钮对齐批记录填写 -> Given 表单模板行已绑定批记录 reportId / When 用户点击“填写” / Then 进入模板模拟填写页。`
- `BDD: 缺少 reportId 必须 fail fast -> Given 模板未绑定批记录表单 / When 用户点击任一红框按钮 / Then 显示阻塞提示且不打开旧弹窗。`

## RED And GREEN

- `RED: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> FAIL, 类型与三按钮行为未对齐。`
- `GREEN: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> PASS, 已覆盖三按钮使用批记录路由、稳定 reportId、返回标签，以及 BOUND + reportId 双条件。`
- `GREEN: pnpm ts:check -> PASS, 前端 relaxed TypeScript 检查通过。`
- `RED after conflict: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> FAIL, 当前工作区被并行任务改回本页流程。`
- `GREEN after user confirmation: node tests\e2e\form-template-batch-record-button-alignment-static.spec.js -> PASS, 用户确认“三个按钮按批记录表单执行”后恢复批记录路径。`

## UX And Permission Checks

- Error state: 缺少绑定时直接提示，不吞异常、不默认成功。
- Loading/empty/responsive/accessibility: 本次仅改按钮路由处理，不改变现有列表、预览和布局结构。
- Permission: 继续依赖批记录设计器和模板模拟填写页既有路由权限。

## Verification

- `node tests\e2e\form-template-batch-record-button-alignment-static.spec.js`：PASS。
- `pnpm ts:check`：PASS。
- `real E2E form template 3 buttons`：PASS，使用本地临时绑定夹具从 `/mdm/form-center/template` 真实点击 `打开 / 编辑 / 填写`，分别验证 preview designer、edit designer、template-simulate URL。
- `real E2E after user confirmation`：PASS，复验本机 8081/48081，临时绑定 `id=29` 到 reportId `2ef53e1302bd47bdba9ccbb87cd92032`，三按钮真实点击路由均通过，夹具恢复为 NULL。
- `git diff --check -- <task-owned files>`：PASS。

## Blockers And Follow-Up

- 当前无产品行为 blocker；实现提交 `3f79f736251dab6be9d0413eea602a4ee1990fa6` 已通过选择性暂存隔离其他并行任务改动，等待收尾文档提交与推送。
