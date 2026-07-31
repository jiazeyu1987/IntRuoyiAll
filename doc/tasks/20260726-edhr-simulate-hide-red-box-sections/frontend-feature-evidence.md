# Frontend Feature Evidence

## Feature Goal

按用户截图隐藏 eDHR 模板模拟填写页红框区域，保留页面核心填写与预览能力。

## Non-goals

- 不修改后端 API、模板数据或路由参数。
- 不隐藏返回按钮、右侧“表单显示”标题或模板主体。
- 不在共享填写组件的其他使用页面隐藏规则图例。
- 不新增 fallback、mock 或兼容分支。

## Requirements

- `AC-1`: 页面不渲染当前工序标题和模板名称。
- `AC-2`: 页面不渲染模板 ID、顺序、填写字段、签名位、必填和附件规则摘要。
- `AC-3`: 左侧不渲染“模板内填写”标题和说明。
- `AC-4`: 左侧模拟填写实例不渲染规则图例。
- `AC-5`: 返回按钮、右侧“表单显示”、可编辑模板和只读模板保持可用。

## Acceptance

- `AC-1` through `AC-5` above define the visible behavior and preserved interactions.

## UI Entry

- Route: `/mes/pro/feedback/edhr-batch-execution/template-simulate`
- Page: `IntRuoyiFronted/src/views/mes/pro/edhr-batch/BatchExecutionTemplateSimulatePage.vue`
- Shared component: `IntRuoyiFronted/src/views/mes/pro/edhr/components/EdhrExecutionTemplateEditableForm.vue`

## API And State Contract

- 保留 `reportId` 直达和 `id + taskId` 两种加载路径。
- 保留规则、签名位和模板布局加载及错误暴露行为。
- 只调整渲染结构和共享组件显式展示参数。

## BDD

BDD: 红框区域隐藏 -> Given 用户进入 eDHR 模板模拟填写页 / When 页面成功加载模板 / Then 红框区域不可见，返回入口、右侧预览和左右模板保持可见

- Given 用户进入 eDHR 模板模拟填写页
- When 页面成功加载模板
- Then 红框区域不可见，返回入口、右侧预览和左右模板保持可见

## Verification

- RED: `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js` 首先失败于工序标题仍渲染。
- GREEN: `node tests/e2e/edhr-batch-template-simulate-red-box-hidden-static.spec.js`、`node tests/e2e/edhr-batch-template-simulate-return-static.spec.js`、`pnpm ts:check` 均通过。
- Blocked: `node tests/e2e/edhr-batch-template-simulate-static.spec.js` 首个既有断言仍要求旧 `Number(route.query.id)` 写法；`pnpm build:local` 因现有 `node_modules` 缺少实体依赖而失败。
- Responsive/accessibility: 删除非必要信息后沿用现有双列/单列响应式布局；规则图例通过显式属性控制。
- Loading/empty/error/permission: 本次不改变对应状态。
- Regression: 聚焦静态合同、既有模拟页合同、返回合同、类型检查与本地构建。

## Blockers

- 全量模拟页静态合同存在与本任务无关的历史断言失败。
- 本地构建环境的 `@babel/helper-validator-identifier` 依赖目录为空。
