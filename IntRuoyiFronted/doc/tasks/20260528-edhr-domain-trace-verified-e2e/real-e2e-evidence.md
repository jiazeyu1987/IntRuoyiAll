# eDHR 主数据追溯真实路径 E2E Evidence

- Task ID: `20260528-edhr-domain-trace-verified-e2e`
- 生成时间：2026-05-27T21:40:12.347Z
- 前端 worktree：D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3
- 固定前端入口：`http://localhost:8081`
- 真实 E2E 复跑命令：`pnpm e2e:edhr:domain-trace`
- 静态语法检查命令：`pnpm e2e:edhr:domain-trace:check`
- 产物目录：`test-results/edhr-domain-trace/`（截图、trace、result.json、evidence.md 均不提交）
- 当前状态：PASS
- Expected final status: `VERIFIED`
- Actual final status: `VERIFIED`
- Expected final blocker count: `0`
- Actual final blocker count: `0`

## BDD

- BDD: 主数据追溯详情可见 -> Given 执行人登录测试租户, When 通过真实主数据追溯详情路由打开指定执行记录, Then 页面展示执行编号、status、domainTraceHash、blockers[] 和 items[] canonical 追溯明细。
- BDD: 主数据追溯校验由前端触发 -> Given 主数据追溯详情页已加载, When 用户点击页面上的校验动作, Then 前端发起 `/domain-trace/verify` 请求并展示后端返回的 canonical 校验状态。
- BDD: 主数据追溯 UI/API 证据一致 -> Given 校验动作完成, When 使用已登录页面上下文读取 `/domain-trace/detail`, Then API 中的 status/domainTraceHash/blockers/items 与页面关键证据一致。
- BDD: 主数据追溯 E2E 缺前置即阻塞 -> Given 缺少测试租户、真实账号、执行记录或前端入口, When 脚本启动, Then fail-fast 写入 evidence markdown 且不使用 mock、API 替代或 silent downgrade。

## GREEN

- GREEN: `pnpm e2e:edhr:domain-trace` -> PASS, 真实 UI 主数据追溯查看、校验和最终 API 交叉确认完成。
- 主数据追溯详情可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\test-results\edhr-domain-trace\01-domain-trace-detail.png`
- 主数据追溯校验状态可见 -> PASS, screenshot: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\test-results\edhr-domain-trace\02-domain-trace-verified.png`
- 已登录上下文 API 最终交叉确认 -> PASS
- Trace: `D:\ProjectPackage\Int\IntRuoyi\worktrees\20260527-edhr-prod-doc-code-subagent-review\yudao-ui-admin-vue3\test-results\edhr-domain-trace\trace.zip`
- Final status: `VERIFIED`
- Final hash: `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`
- Final blocker count: `0`
- Final item count: `8`
