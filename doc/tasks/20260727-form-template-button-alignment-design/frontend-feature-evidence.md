# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 表单模板红框 `打开 / 编辑 / 填写` 始终操作当前 FormCenter 模板，不要求批记录绑定。
- Non-goal: 不修改批记录表单页面，不新增跨域转换，不做无关视觉重构。

## Requirements And Acceptance

- `FT-INDEPENDENT-001`: `打开`调用 `TemplateViewDialog` 查看当前模板。
- `FT-INDEPENDENT-002`: `编辑`调用 `openSelectedTemplateAction('edit')` 打开当前模板规则工作区。
- `FT-INDEPENDENT-003`: `填写`重置当前模板模拟值并打开 `.form-template-fill-dialog`。
- `FT-INDEPENDENT-004`: 三个按钮不读取批记录绑定字段、不显示未绑定错误、不跳转 MES 路由。

## UI Entry Points And Owned Files

- Entry route: `/mdm/form-center/template`。
- Page: `IntRuoyiFronted/src/views/form-center/template/index.vue`。
- API type: `IntRuoyiFronted/src/api/form-center/template.ts`。
- Regression contract: `IntRuoyiFronted/tests/e2e/form-template-independent-button-actions-static.spec.js`。

## API Contracts And Data States

- `FormTemplateListItemVO` 只包含当前模板数据，不包含七个 `batchRecord*` 绑定字段。
- 三个按钮的唯一业务上下文是 `selectedTemplate`。
- 模板未选择时不执行；模板存在时不检查 `reportId`。
- 模板布局或接口失败时暴露当前模板真实错误，不切换到批记录链路。

## BDD Scenarios

- `BDD: 未绑定批记录的表单模板可以打开 -> Given 当前模板存在且没有批记录绑定 / When 点击“打开” / Then 显示“查看表单模板”，不显示绑定错误。`
- `BDD: 表单模板编辑使用自身规则工作区 -> Given 当前模板允许编辑 / When 点击“编辑” / Then 显示模板规则编辑弹窗且路由仍属于 FormCenter。`
- `BDD: 表单模板填写使用自身模拟工作区 -> Given 当前模板允许交互 / When 点击“填写” / Then 显示模板模拟填写弹窗且不创建批记录执行。`
- `BDD: 三按钮禁止跨域跳转 -> Given 用户位于表单模板页面 / When 依次点击三个按钮 / Then pathname 始终为 /mdm/form-center/template。`

## RED And GREEN

- `RED: node tests\e2e\form-template-independent-button-actions-static.spec.js -> FAIL, “打开”仍调用批记录设计器，三个按钮依赖 BOUND + reportId。`
- `GREEN: node tests\e2e\form-template-independent-button-actions-static.spec.js -> PASS, 三个按钮恢复当前模板查看、编辑和模拟填写工作区。`
- `GREEN: pnpm ts:check -> PASS, 前端 TypeScript 检查通过。`

## Responsive, Accessibility, Loading, Empty, Error, And Permission Checks

- Responsive: 未改变模板页布局，三个 Dialog 继续使用现有响应式宽度。
- Accessibility: 保留 `打开 / 编辑 / 填写` 文本按钮和 Dialog 键盘关闭能力。
- Loading/empty: 继续使用模板池加载态和未选择模板空态。
- Error: 未出现“当前模板未绑定批记录表单”，未吞掉其他真实错误。
- Permission: `编辑`继续使用既有 `form:template:create` 权限和模板状态控制。

## E2E Verification Path

- 本机入口：`http://127.0.0.1:8081/mdm/form-center/template`。
- 身份标签：`芋道源码/admin`。
- 浏览器：本机 Google Chrome，通过 Playwright `executablePath` 启动。
- 结果：
  - `打开`显示“查看表单模板”。
  - `编辑`显示 `.form-template-rules-dialog`。
  - `填写`显示 `.form-template-fill-dialog`。
  - 三次点击 pathname 均为 `/mdm/form-center/template`。
  - 页面没有批记录绑定错误。

## Blockers And Follow-Up Skills

- 当前前端行为无 blocker。
- 本地数据库冗余列的物理清理由后续独立 `database-schema-delivery` 任务处理。
