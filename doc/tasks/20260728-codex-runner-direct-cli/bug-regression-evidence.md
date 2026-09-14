# Bug Regression Evidence

## Feature

Codex Runner 按需启动脚本。

## Acceptance

BDD: Given Runner token、后端健康入口、Node 与 Codex CLI 可用，且前端入口暂时不可达；When 测试管理后端按需启动 Runner；Then 启动脚本不得因前端入口不可达提前失败，必须让 Runner 启动并由具体真实页面任务暴露页面不可达。

## Scope

`IntRuoyiFronted/scripts/start-codex-test-runner.ps1` 的启动前置校验，以及后端 `CodexTestRunnerBootstrapServiceImpl` 调用该脚本的边界。

## Contract

按需启动前必须校验 Node、Codex CLI 和后端健康；不得校验前端入口 HTTP 可达性；仍必须把 `CODEX_TEST_FRONTEND_BASE_URL` 传给 Runner。

## Validation

使用新增静态合同锁定脚本结构，并复跑现有测试管理静态合同和后端 Bootstrap JUnit。

## Bug Summary

测试管理点击执行时，后端按需启动 Codex Runner 调用 `IntRuoyiFronted/scripts/start-codex-test-runner.ps1`。脚本在 Runner 启动前强制探测 `http://127.0.0.1:8081`，当前端入口暂时不可达时直接退出，导致用户看到“Codex Runner 按需启动失败：Frontend entry is not reachable”。

## Expected Behavior

按需 Runner 启动只应在启动前校验 Runner 自身必需前置：Runner token、后端注册入口、Node、Codex CLI。前端 URL 应传给 Runner，由具体真实页面测试任务在执行阶段暴露前端不可达问题。

## Reproduction

`node tests/e2e/codex-runner-on-demand-startup-script-static.spec.js`

## Root Cause

`start-codex-test-runner.ps1` 在启动 Node Runner 之前调用 `Assert-HttpReachable -Url $FrontendBaseUrl -Name 'Frontend entry'`，把页面入口健康错误提前提升成 Runner 启动失败。

## Regression Test

新增 `IntRuoyiFronted/tests/e2e/codex-runner-on-demand-startup-script-static.spec.js`，锁定启动脚本不得再包含前端入口硬阻断，同时必须继续保留后端健康、Node、Codex CLI 校验和前端 URL 环境注入。

## RED

RED:

`node tests/e2e/codex-runner-on-demand-startup-script-static.spec.js` -> FAIL，原因是旧脚本仍包含 `Assert-HttpReachable -Url $FrontendBaseUrl -Name 'Frontend entry'`。

## GREEN

GREEN:

- `node tests/e2e/codex-runner-on-demand-startup-script-static.spec.js` -> PASS。
- `node tests/e2e/system-codex-test-management-static.spec.js` -> PASS。
- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerBootstrapServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS。

## Verification

前端脚本静态合同、测试管理静态合同和后端 Bootstrap JUnit 均已通过。

## Blockers

无。

## Risk And Regression Scope

风险集中在 Runner 启动前置顺序。修复未放宽后端、token、Node 或 Codex CLI 失败；仅移除前端 HTTP 探测这个非 Runner 启动必需前置。真实页面不可达仍会在 Codex CLI 执行测试方法时失败并回写结构化结果。
