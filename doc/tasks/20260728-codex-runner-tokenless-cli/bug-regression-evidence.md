# Bug Regression Evidence

## Bug Summary

测试管理点击执行仍提示 `Codex Runner token 无效或未配置`。用户已明确要求本机测试管理走 tokenless Codex CLI 模式，不再把 Runner token 作为本地启动前置。

## Expected Behavior

当后端未配置 `yudao.codex-test.runner.token` 且 Node、Codex CLI、后端健康可用时，按需 Runner 启动和 Runner 协议注册不得因 token 缺失失败；Runner 内部继续直接执行 `codex exec`，并通过后端正式执行批次、Runner 会话、heartbeat 和结构化回写完成测试。

## Reproduction

- RED: `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> FAIL，启动脚本仍要求 `CODEX_TEST_RUNNER_TOKEN or -TokenFile`。
- RED: `node tests\e2e\codex-test-runner-http-client-static.spec.js` -> FAIL，Runner 进程仍要求 `CODEX_TEST_RUNNER_TOKEN`。
- RED: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest#registerRunner_allowsMissingTokenWhenLocalCliModeHasNoConfiguredToken,CodexTestRunnerBootstrapServiceImplTest#ensureRunnerAvailable_startsWrapperWhenRunnerTokenIsBlankForLocalCliMode" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，后端启动受控 Runner 前抛 `Codex Runner token 无效或未配置`。

## Root Cause

前一次“裸调 Codex CLI”只移除了启动脚本对前端入口 HTTP 可达性的硬阻断，但 Runner 启动脚本、Node Runner HTTP client 和后端 Runner token 校验仍把 `CODEX_TEST_RUNNER_TOKEN` 当成必填前置，导致本地 tokenless 模式尚未真正生效。

## Fix

- `IntRuoyiFronted/scripts/start-codex-test-runner.ps1` 不再要求 `CODEX_TEST_RUNNER_TOKEN` 或 `-TokenFile`。
- `IntRuoyiFronted/scripts/codex-test-runner.mjs` 将 token 改为可选环境变量，仅在 token 存在时注入 `X-Codex-Runner-Token`。
- `CodexTestRunnerBootstrapServiceImpl` 允许空 token 启动受控 Runner，并避免空 token 脱敏替换污染启动输出。
- `CodexTestRunnerServiceImpl` 在服务端未配置 runner token 时允许 tokenless 本地 Runner 协议；服务端配置 token 时仍严格匹配。

## GREEN Evidence

- GREEN: `node tests\e2e\codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\codex-test-runner-http-client-static.spec.js` -> PASS。
- GREEN: `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest#registerRunner_allowsMissingTokenWhenLocalCliModeHasNoConfiguredToken,CodexTestRunnerBootstrapServiceImplTest#ensureRunnerAvailable_startsWrapperWhenRunnerTokenIsBlankForLocalCliMode" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，2 tests。

## Verification

- `node --check scripts\codex-test-runner.mjs` -> PASS。
- `node tests\e2e\system-codex-test-management-static.spec.js` -> PASS。
- `node tests\e2e\codex-test-runner-child-settlement-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest,CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，15 tests。

## Risk And Scope

本次只改变本机 Runner token 前置条件。测试管理执行批次、Runner 会话、heartbeat、任务领取、进度回写、检查点回写、artifact 上传和完成回写仍保留正式后端协议；未引入 mock、默认成功或 API-only 替代 E2E。

长期门禁已同步到 `docs/e2e-rules.md` 和 `docs/experience-index.md`：tokenless 本地 Runner 允许在后端未配置 token 且用户明确批准时使用；禁止的是绕过后端 Runner 会话和结构化回写直接裸调用 `codex` CLI。

## Blockers

推送前仍需处理当前 `int_main` 分支 `ahead 11, behind 6` 以及工作区存在多项并行脏改动的 Git 边界。
