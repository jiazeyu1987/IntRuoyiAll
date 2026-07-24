# Execution Log

## BDD

BDD: eDHR 页面标签返回复用缓存 -> Given 用户已打开目标 eDHR 页面且数据加载完成，When 用户切换到其他标签后再返回，Then 页面复用原实例且不因返回标签重复加载相同数据。

BDD: eDHR 页面参数变化加载新数据 -> Given 目标页面实例仍在缓存中，When 路由业务标识变为另一条有效记录，Then 页面加载新的业务数据。

BDD: 非目标路由不触发缓存页面加载 -> Given 缓存页面已停用，When 全局 route query 因其他页面发生变化，Then 已停用页面的 watcher 不发起加载。

## TDD Evidence

- RED: `node tests/e2e/edhr-tab-cache-consistency-static.spec.cjs` -> FAIL，执行表单路由仍为 `noCache: true`，符合预期失败原因。
- GREEN: `node tests/e2e/edhr-tab-cache-consistency-static.spec.cjs` -> PASS，8 个目标路由、组件名和 3 个参数 watcher 缓存契约通过。
- REGRESSION: 目标 eDHR 静态测试组、ESLint 与 `pnpm ts:check` -> PASS。
- REGRESSION: `node tests/e2e/edhr-batch-detail-review-fusion-static.spec.js` -> FAIL，失败点为批次详情页既有“工序复盘”文案断言，与本任务路由缓存修改无关；本任务新增的复盘壳组件/路由断言已越过。

## Experience Preflight

- GREEN: experience-preflight -> PASS；已读取 `docs/experience-index.md` 命中经验、`docs/powershell-memory.md`、`docs/login-access.md` 与 Playwright 执行规则。
- 本次真实 E2E 仅在本机 `http://localhost:8081` 执行只读页面切换，不访问测试服/正式服，不产生 MES 写请求。

## Blockers

- 无。

## Real E2E

- GREEN: 官方登录预检（测试租户）-> PASS；测试租户批次列表无可用记录，因此未创建模拟数据。
- GREEN: 官方登录预检（芋道源码/admin）-> PASS；按项目规则仅执行最终只读复验。
- GREEN: `node doc/tasks/20260710-edhr-tab-cache-consistency-fix/verify-edhr-tab-cache-real.e2e.cjs` -> PASS。
- 批次复盘：`batchExecutionId=900000000480`、`batchCode=34126020001`；切换返回前后 `batchGet=2`、`batchWorkbench=2`、`batchReviewTimeline=1`，计数不变。
- 执行表单：`executionId=782`；切换返回前后 `executionGet=1`，计数不变。
- 只读门禁：`mesWrites=[]`，浏览器错误：`pageErrors=[]`。

## Milestone Status

- M1：完成。
- M2：完成。
- M3：完成。
- M4：完成。
- M5：完成。

## Closeout Evidence

- GREEN: implementation commit -> PASS，`ded02227a`。
- GREEN: task-closeout preview -> PASS，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- GREEN: task-closeout apply -> PASS，删除本任务一次性 E2E 产物、脚本和临时 evidence。
- GREEN: main worktree closeout -> PASS，无额外 worktree 合并或删除。
