# Execution Log

## 2026-07-28

- User report: 页面红框处没看到 `串行路线` 下拉框。
- Root cause: 当前运行工作区 `E:\IntRuoyi` HEAD 为 `08c3eae0`，`origin/int_main` 为 `fd05b0da`，本地落后远端 6 个提交；当前 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue` 缺少 `codex-test-node-chain-filter` 常驻控件。
- BDD: 本地可见串行路线筛选 -> Given 当前用户进入测试管理页；When 查看快速筛选右侧区域；Then 能看到 `串行路线` 下拉并可选择节点串筛选列表。
- RED: `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> FAIL，断言 `测试管理页必须在快速筛选右侧提供常驻的串行路线下拉。`
- Implemented: 在当前运行工作区 `IntRuoyiFronted/src/views/system/codex-test-management/index.vue` 的 `extra-filters` 内增加 `串行路线` 下拉，绑定 `queryParams.nodeChainName`，切换后清空选择、回到第一页并调用 `getCaseList()`。
- GREEN: `node .\tests\e2e\system-codex-test-node-chain-static.spec.js` -> PASS。
- REGRESSION: `node .\tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check` -> PASS，仅有已有工作区 CRLF 提示，无空白错误。
- Commit blocker: 当前 `E:\IntRuoyi` 本地 `int_main` 仍是 `08c3eae0`，`origin/int_main` 是 `fd05b0da`，并且有大量并行任务脏改动；为避免混入并行任务，本地同步改动暂不提交。正式远端主线已经包含同等代码。
- UI placement fix: 在最新正式 worktree 中发现外置 `label="串行路线"` 加 `!w-240px` 下拉总宽度过大，可能被工具栏操作区挤到下一行；改为 `el-form-item` 无外置标签、`el-select aria-label="串行路线"`、`class="!w-180px"`，确保能放入红框位置。
- GREEN(compact): `node .\tests\e2e\system-codex-test-node-chain-static.spec.js`、`node .\tests\e2e\system-codex-test-management-static.spec.js`、`pnpm ts:check`、frontend/bug evidence validators、`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- Integration: 紧凑版提交 `83f37166` 后融合 `origin/int_main` `35a1255c`，生成最新 HEAD `37f66c7e`。
- GREEN(after compact merge): `node .\tests\e2e\system-codex-test-node-chain-static.spec.js`、`node .\tests\e2e\system-codex-test-management-static.spec.js`、`pnpm ts:check`、`scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- Integration retry: 远端主线再次前进到 `7d59f3bf`，融合后 HEAD `4be33aa1`；节点串静态合同、测试管理静态回归、`pnpm ts:check` 和端口守卫均 PASS。
