# Codex Runner Tokenless CLI Mode

## Task Goal

修复测试管理点击执行仍提示 `Codex Runner token 无效或未配置` 的问题。用户明确要求“裸调 Codex CLI”，本任务按“本地按需 Runner 不再要求配置 Runner token，直接拉起 Codex CLI 并继续结构化回写执行结果”实施。

## Milestones

- [x] 复现 token 未配置时启动失败的现有行为。
- [x] 增加后端与前端脚本回归合同，锁定 token 未配置时不阻断本地 Codex CLI 执行链路。
- [x] 修改 Runner token 校验、启动脚本和 HTTP client 头部注入逻辑。
- [x] 运行目标回归验证。
- [ ] 更新任务证据并提交推送。

## Expected Verification

- 后端 JUnit 覆盖 token 未配置时 Runner 注册/启动不抛 `CODEX_TEST_RUNNER_TOKEN_INVALID`。
- 前端静态合同覆盖启动脚本不再要求 `CODEX_TEST_RUNNER_TOKEN or -TokenFile is required`。
- 现有 Runner HTTP client 与测试管理静态合同通过。

## Current Status

completed

## Applicable Gates

- 用户明确覆盖旧门禁：旧 E2E 门禁禁止“裸调用 codex CLI”，但本次用户明确要求去掉 Runner token 配置依赖。
- Strict no fallback：不得 mock Runner 成功；Codex CLI、Node、后端健康仍必须 fail-fast。
- BDD/TDD：先记录 RED，再改生产代码。
- 实现边界：测试管理仍通过后端受控 Runner 创建执行、注册、领取、心跳和结构化回写；去掉的是本机 Runner token 必填前置，Runner 内部执行 `codex exec`。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。token 未配置时进入显式 tokenless 本地执行链路；Codex CLI、Node 和后端失败仍真实暴露。
- 是否从根因和长期维护角度解决：是。根因是 Runner token 被当成必填运行前置，与用户要求的裸调模式冲突。
- 是否存在临时补丁或绕过：否。

## Cleanup Keep

- doc/tasks/20260728-codex-runner-tokenless-cli/bug-regression-evidence.md
