# Feature

优化进入 eDHR 批记录表单填写页的首屏时间。

## Acceptance

- 首屏关键路径只等待真实执行详情接口和草稿预填接口。
- 归档状态、追踪时间线和签名摘要在首屏渲染后一帧异步加载。
- 辅助接口失败继续在页面暴露真实错误，不使用 mock、fallback、默认成功或吞异常。
- 旧路由/旧请求返回不得覆盖当前批记录表单页面状态。

## BDD

- `BDD: 批记录表单首屏优先显示 -> Given 用户从 eDHR 正式入口进入批记录表单 / When 首屏初始化开始 / Then 表单主体所需上下文优先加载并渲染，非首屏辅助数据不得阻塞首屏可见。`
- `BDD: 非首屏数据仍正确加载 -> Given 批记录表单首屏已可见 / When 用户继续查看表单辅助区域或后续交互 / Then 非首屏数据按原有接口契约加载，失败时保持真实错误暴露。`

## RED

- `RED: node tests/e2e/edhr-execution-first-screen-defer-static.spec.js -> FAIL, loadExecution still waited for loadLatestArchive before first screen completion.`

## GREEN

- `GREEN: node tests/e2e/edhr-execution-first-screen-defer-static.spec.js -> PASS`
- `GREEN: node tests/e2e/edhr-fill-workspace-worktask-permission-static.spec.js -> PASS`
- `GREEN: node tests/e2e/edhr-recordbook-global-setting-static.spec.js -> PASS`
- `GREEN: node tests/e2e/edhr-batch-fill-direct-navigation-static.spec.js -> PASS`

## Verification

- `node tests/e2e/edhr-pre-release-editable-submit-static.spec.js -> PASS`
- `node tests/e2e/edhr-execution-list-removal-static.spec.js -> PASS`
- `pnpm ts:check -> PASS` on rerun with extended timeout.
- `git diff --check -- <task-owned files> -> PASS` with CRLF warnings only.

## Blockers

- Real browser E2E was not run because this task did not start local frontend/backend runtime or touch tenant data.
- Repository closeout is blocked by unrelated dirty working tree changes and current branch being ahead of `origin`.
