# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 去掉填写配置弹窗底部空白占位，让原表单、辅助表单预览和右侧映射控制栏填充全屏弹窗正文高度。
- Non-goal: 不改变填写配置保存、重新读取、导航候选、辅助表格映射或接口契约。

## Requirements And Acceptance

- AC1: 全屏填写配置弹窗底部不再保留旧页脚空白。
- AC2: 主工作区通过 flex 填充剩余高度，内部滚动仍由各面板承担。
- AC3: 红框隐藏后的必要操作能力仍由现有红框隐藏合同覆盖。

## UI Entry Points And Owned Files

- Entry: 批记录表单列表右侧“填写配置”动作打开 `BatchRecordCellRulesConfirmDialog`。
- Component: `IntRuoyiFronted/src/views/mes/pro/batchrecordformlist/BatchRecordCellRulesConfirmDialog.vue`。
- Tests: `IntRuoyiFronted/tests/e2e/batch-record-cell-rule-bottom-fill-static.spec.js`。

## API Contracts And States

- 未修改 API、路由、权限、保存 payload 或错误处理。
- 仅修改 CSS 布局高度分配。

## BDD Scenarios

- BDD: 去掉填写配置弹窗底部空白 -> Given 用户打开填写配置弹窗 / When 原表单、辅助表单预览和映射控制栏渲染 / Then 底部不存在固定空白占位，三栏主内容区域填充到弹窗可用底部空间。

## RED / GREEN

- RED: `node tests/e2e/batch-record-cell-rule-dialog-size-static.spec.js` -> FAIL，旧实现缺少正文高度占满断言。
- GREEN: `node tests/e2e/batch-record-cell-rule-bottom-fill-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。

## Responsive / Accessibility / State Checks

- Responsive: 工作区使用 `flex: 1` 与内部滚动容器，保留窄屏三栏响应式规则。
- Accessibility: 未修改按钮、选择器、表格单元格或 aria 属性。
- Loading / Empty / Error: 未修改 `v-loading`、`el-empty`、`el-alert` 或错误传播链。

## E2E Or Component Verification Path

- 聚焦静态合同验证样式契约。
- 未运行真实 E2E；本次为 CSS 高度分配修复，且真实运行态/登录态不是该静态布局修复的必要前置。

## Blockers And Follow-Up

- 并行红框隐藏改动让部分旧相邻静态合同仍失败；这些旧断言要求已删除的顶部操作组和说明标题，已在 execution-log 中单独记录。

