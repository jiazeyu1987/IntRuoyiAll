# Bug Regression Evidence

## Bug

用户在测试管理页红框位置看不到 `串行路线` 下拉框。

## Expected

测试管理页快速筛选右侧必须显示 `串行路线` 下拉，选择后列表使用 `queryParams.nodeChainName` 只查询该串行路线对应节点。

## Reproduction

- `rg -n 'codex-test-node-chain-filter|串行路线|handleNodeChainFilterChange' IntRuoyiFronted\src\views\system\codex-test-management\index.vue` -> 当前运行工作区最初未找到红框常驻控件。
- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> FAIL，缺少红框位置的常驻 `串行路线` 下拉。

## Root Cause

当前运行工作区 `E:\IntRuoyi` 的 `int_main` 落后已集成远端主线，页面源码仍停留在只有节点串列和快速过滤配置的版本，未包含红框位置的常驻下拉控件。

## RED:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> FAIL。

## GREEN:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> PASS。
- `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Verification

- 页面源码已包含 `codex-test-node-chain-filter`、`label="串行路线"`、`v-model="queryParams.nodeChainName"` 和 `handleNodeChainFilterChange()`。
- 静态合同确认切换串行路线会回到第一页并刷新列表。

## Blockers

- 本地 `E:\IntRuoyi` 同时落后远端且存在大量并行任务脏改动；为避免污染并行任务，本地同步不提交。正式远端主线已经包含同等代码。
