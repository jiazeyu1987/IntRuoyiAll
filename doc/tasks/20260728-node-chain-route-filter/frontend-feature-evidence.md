# Frontend Feature Evidence

## Feature Goal

在 `系统管理 > 测试管理` 的快速筛选右侧增加常驻 `串行路线` 下拉框。用户选择某条串行路线后，列表只显示该串行路线对应的测试节点；清空后恢复显示全部匹配测试项。

## Non-goals

- 不新增后端接口或数据库字段。
- 不修改节点串创建、执行、阻断规则。
- 不修改测试租户、项目、测试项名称等既有筛选含义。

## Requirements

- R1: 快速筛选区域右侧必须可见 `串行路线` 下拉。
- R2: 下拉选项复用当前节点串选项接口，显示路线名称、项目和节点数量。
- R3: 选择路线时分页回到第一页并刷新列表。
- R4: 清空路线时恢复按其他筛选条件查询。

## Acceptance

- AC1: 测试管理页快速筛选右侧出现 `串行路线` 下拉。
- AC2: 下拉值写入 `queryParams.nodeChainName` 并触发测试项分页刷新。
- AC3: 清空下拉后保留其他筛选条件并刷新列表。

## UI Entry Points

- Route: `系统管理 > 测试管理`
- Component: `IntRuoyiFronted/src/views/system/codex-test-management/index.vue`
- Test: `IntRuoyiFronted/tests/e2e/system-codex-test-node-chain-static.spec.js`

## API Contracts

- Page query keeps using `CodexTestCasePageReqVO.nodeChainName`.
- Options keep using `GET /system/codex-test-case/node-chain-options`.
- Page data keeps using `GET /system/codex-test-case/page`.

## BDD Scenarios

- BDD: 串行路线筛选 -> Given 测试管理列表存在多个节点串；When 用户在 `串行路线` 下拉中选择一条路线；Then 列表只显示该路线对应节点。
- BDD: 清空串行路线 -> Given 用户已经选择某条串行路线；When 清空下拉；Then 列表恢复使用其他筛选条件展示全部匹配测试项。
- BDD: 不影响现有筛选 -> Given 用户同时设置项目、测试项名称或测试租户；When 选择或清空串行路线；Then 其他筛选条件保持不变。

## RED:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> FAIL，缺少常驻 `串行路线` 下拉。

## GREEN:

- `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> PASS。
- `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- `pnpm ts:check` -> PASS。
- After merging `origin/int_main` `1cab989a` into HEAD `17853328`, the same node-chain static contract, management static regression, `pnpm ts:check`, and branch runtime port guard all passed.
- After remote `int_main` advanced again, merging `origin/int_main` `bdeeef70` into HEAD `2d07ea77` also kept the node-chain static contract, management static regression, `pnpm ts:check`, and branch runtime port guard passing.

## Verification

- 聚焦静态契约确认常驻下拉、绑定字段、清空刷新和第一页重置行为。
- 相邻静态契约确认测试管理页既有列表、权限和操作区未回归。
- TypeScript relaxed check confirms the Vue template and script changes compile.

## UX Checks

- Loading: 复用现有列表加载状态 `caseLoading`。
- Empty: 筛选为空时沿用列表空态。
- Error: 查询失败沿用 `showRequestError(error, '测试项加载失败')`。
- Accessibility: 使用 Element Plus `el-form-item` label 和 `el-select`，保留键盘可筛选和可清空行为。
- Responsive: 下拉宽度与测试租户筛选一致，使用现有 `!w-240px`。

## Blockers

- 无。
