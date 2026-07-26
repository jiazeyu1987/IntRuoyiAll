# Execution Log

## User Intent

- 用户指出执行测试时反复出现 `没有在线 Codex Runner`，希望采用长期方案而不是补丁式修复。

## BDD / TDD

- BDD: Runner 在线执行 -> Given 本机存在已注册且心跳未过期的 Codex Runner / When 用户点击测试项执行 / Then 后端创建执行批次且不会提示没有在线 Runner。
- BDD: Runner 离线可诊断 -> Given 没有在线 Runner / When 用户点击测试项执行 / Then 页面展示 Runner 离线原因、最近心跳、启动指引或自动启动状态，不只给出笼统错误。
- BDD: Runner 前置条件缺失 -> Given Codex CLI、Runner token、Node、前端入口或后端入口缺失 / When 触发 Runner 启动或探测 / Then 系统 fail fast 并展示具体缺失项，不创建伪成功执行。
- BDD: Runner 心跳过期 -> Given Runner 记录存在但 heartbeat 超时 / When 用户执行测试 / Then 后端将其视为离线并返回可诊断状态。

## Command Log

- CREATED: task docs for `20260726-codex-runner-availability-hardening`.
- GREEN: experience-preflight -> PASS, applicable gates copied from `docs/e2e-rules.md#Codex Runner 自动测试门禁` and `docs/local-runtime.md`.

## Blockers / Limits

- Pending: inspect current Runner lifecycle and reproduce the recurring offline error.
