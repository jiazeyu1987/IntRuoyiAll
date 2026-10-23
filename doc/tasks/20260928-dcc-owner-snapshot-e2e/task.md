# DCC BDD-09/10 任务创建时负责人快照真实 E2E

## Current Status
in_progress

## 任务目标

在测试租户“芋道源码”中，通过真实 Playwright 页面验证 DCC 会签负责人快照：创建任务时生产部负责人为 Owner A、质量部负责人为 Owner B；任务创建后交换两部门负责人；再分别以 Owner A、Owner B 登录真实审批中心或详情页面，确认既有任务仍按创建时快照分配，不随当前负责人变更转移。最终通过管理员页面恢复生产部 -> Owner A、质量部 -> Owner B，并在页面确认。

## BDD

### BDD-09 任务创建时冻结负责人

Given 生产部负责人为 Owner A、质量部负责人为 Owner B，且会签矩阵选择生产部和质量部
When 管理员通过真实 DCC 上传页面创建一条 `needTraining=false` 的新任务并提交会签，随后通过真实部门管理页面交换负责人为生产部 -> Owner B、质量部 -> Owner A
Then 任务创建时保存的两条部门义务仍分别指向 Owner A/Owner B，既有任务不因负责人配置变更而改派；页面应展示部门、负责人快照和任务状态。

### BDD-10 负责人变更不转移既有任务

Given BDD-09 已创建任务且负责人已交换
When Owner A 和 Owner B 分别通过真实登录页面进入审批中心或任务详情
Then Owner A 仍能看到并办理创建时属于生产部的会签义务，Owner B 仍能看到并办理创建时属于质量部的会签义务；若页面权限或会签任务状态阻断，必须记录明确的真实页面阻断，不使用 API、数据库或强制点击绕过。

## 业务数据约束

- 租户固定为“芋道源码”。
- 前端 `http://127.0.0.1:8062`，后端运行态 `48062`；只使用当前任务运行态。
- 创建使用带 `DCC-E2E-OWNER-SNAPSHOT-` 前缀的任务自有文件编号，`needTraining=false`。
- 初始配置：生产部 -> Owner A，质量部 -> Owner B。
- 交换配置：生产部 -> Owner B，质量部 -> Owner A。
- 收尾恢复：生产部 -> Owner A，质量部 -> Owner B。
- 密码不写入本任务任何文件、截图说明或 JSON；仅使用父线程提供的凭据。

## 设计约束检查

- [x] 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/e2e-rules.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/worktree-restrictions.md`。
- [x] 用户当轮明确授权真实 Playwright E2E。
- [x] 业务动作必须由 Playwright 页面完成；禁止 `fetch`、API client、直接 HTTP 写请求、数据库写入和 Git。
- [x] 允许监听页面自然请求的 HTTP 状态，但不以响应 JSON 代替页面业务断言。
- [ ] 已保存任务自有文件 ID、负责人交换页面证据、A/B 页面证据和最终恢复证据。

## 预期验证

- 管理员页面完成一次 `needTraining=false` DCC 上传提交，并在真实页面确认创建结果。
- 部门管理页面完成交换并重新打开确认。
- Owner A、Owner B 分别真实登录并核对既有任务的会签义务归属；记录 PASS 或明确 BLOCKED。
- 管理员页面恢复并重新打开确认。
- 每个关键阶段保存截图、Playwright trace 和脱敏 JSON 结果；不落盘凭据。

## Cleanup Keep

- doc/tasks/20260928-dcc-owner-snapshot-e2e/task.md
- doc/tasks/20260928-dcc-owner-snapshot-e2e/execution-log.md
