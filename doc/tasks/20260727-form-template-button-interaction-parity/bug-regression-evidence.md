# Bug Regression Evidence

## Bug Summary

表单模板三个按钮虽然已移除批记录绑定依赖，但页面级交互结构仍与批记录管理不同：批记录管理使用同页独立 `DesignerWrapper` 处理“打开/编辑”，使用独立路由页面处理“填写”；表单模板仍由列表页 `index.vue` 内部条件分支直接渲染三个工作区。

## Expected Behavior

- `打开/编辑`：当前 `/mdm/form-center/template` 路由进入 `mode=designer`，由 FormCenter 专属 `DesignerWrapper` 渲染。
- `填写`：进入 `/mdm/form-center/template/simulate` 的独立页面组件。
- 三个动作仅使用 `templateId + versionNo + jimuSchemaJson`，不得读取批记录 `reportId` 或绑定状态。

## Reproduction

`node tests\e2e\form-template-button-interaction-parity-static.spec.js`

## Root Cause

上一轮实现只完成了“取消弹窗”和“保持领域独立”，但将三个工作区继续内嵌在列表页组件中，并自定义为 `mode=workspace`。这与批记录管理的页面级组件切换契约不同。

独立模拟填写路由最初直接复用 `index.vue` 且用路由名称判断模拟模式，路由切换期间旧列表实例和新页面实例会同时加载一次模板版本，造成重复请求。

## Regression Test

`IntRuoyiFronted/tests/e2e/form-template-button-interaction-parity-static.spec.js`

## RED

RED: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> FAIL，缺少 `FormTemplateDesignerWrapper`、`mode=designer` 和独立 `FormTemplateSimulatePage.vue`。

RED: 同一合同扩展后 -> FAIL，DesignerWrapper/模拟填写未强制精确读取当前模板版本。

RED: 真实 Playwright 请求审计 -> FAIL，填写页面精确模板版本接口请求 2 次。

## GREEN

GREEN: `node tests\e2e\form-template-button-interaction-parity-static.spec.js` -> PASS。

GREEN: `node tests\e2e\form-template-independent-button-actions-static.spec.js` -> PASS。

GREEN: `pnpm ts:check` -> PASS。

GREEN: 真实 Playwright -> PASS，打开、编辑、填写各只请求一次 `GET /form-center/templates/28/versions/V3.0`，无写请求、无弹窗、无绑定错误、无控制台错误。

## Verification

- Vue SFC 编译检查、ESLint、聚焦合同和真实页面路径均通过。
- 宽合同只剩无关策略菜单 `activeMenu` 历史断言失败。

## Risk And Scope

变更仅调整表单模板页面的交互壳层和路由组件组织，不修改模板数据模型、不接入 MES 批记录接口、不创建批记录执行数据。

## Blockers

- 宽合同仍有无关策略菜单 `activeMenu` 历史断言失败，不影响本缺陷聚焦合同和真实 E2E。
