# Frontend Feature Evidence

## Feature

同步测试管理页 `串行路线` 常驻下拉到当前运行工作区，保证用户在红框位置可见并可按节点串筛选。

## Acceptance

- 快速筛选右侧显示 `串行路线` 下拉。
- 下拉使用 `queryParams.nodeChainName`。
- 选择或清空下拉后回到第一页并刷新测试项列表。
- 下拉使用 `180px` 紧凑宽度和 `aria-label="串行路线"`，避免被操作按钮挤到下一行。

## BDD:

- Given 测试管理列表存在多个节点串，When 用户选择一条串行路线，Then 列表只显示该串行路线对应节点。

## RED:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> FAIL，缺少常驻下拉。

## GREEN:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> PASS。
- `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。

## Verification

- 聚焦静态合同覆盖红框下拉、查询参数、第一页重置和刷新。
- 聚焦静态合同覆盖紧凑宽度和无障碍标签。
- 相邻静态合同覆盖测试管理原有列表、按钮和权限区域未回归。
- TypeScript 检查通过。

## Blockers

- 本地并行工作区不适合提交；远端正式主线已包含同等代码。
