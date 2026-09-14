# Codex Runner Tokenless Local Restart

## Task Goal

修复测试管理仍提示 `Codex Runner token 无效或未配置` 的问题。当前根因是本地后端重启脚本仍会生成并注入 `CODEX_TEST_RUNNER_TOKEN`，导致后端运行态继续进入 token 校验模式；本任务将 `int_main` 本地后端启动链路改为默认 tokenless Runner 模式，保持受控 Runner、Codex CLI、心跳和结构化回写不变。

## Milestones

- [x] 复现本地重启脚本仍注入 Runner token 的现有合同失败。
- [x] 修改本地后端启动脚本，默认不生成、不读取、不注入 Runner token，并清理继承环境变量污染。
- [x] 更新运行态规则与任务证据，明确 tokenless 本地模式边界。
- [x] 运行目标回归验证。
- [x] 按 Git 门禁提交并推送当前修复。

## Expected Verification

- `python -X utf8 -m pytest IntRuoyiBackend/script/tests/test_runtime_control_scripts.py -q` 通过。
- `node IntRuoyiFronted/tests/e2e/codex-runner-on-demand-startup-script-static.spec.js` 通过。
- `node IntRuoyiFronted/tests/e2e/codex-test-runner-http-client-static.spec.js` 通过。
- `mvn.cmd -pl yudao-module-system -am "-Dtest=CodexTestRunnerServiceImplTest#registerRunner_allowsMissingTokenWhenLocalCliModeHasNoConfiguredToken,CodexTestRunnerBootstrapServiceImplTest#ensureRunnerAvailable_startsWrapperWhenRunnerTokenIsBlankForLocalCliMode" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过。

## Current Status

completed

## Applicable Gates

- Strict no fallback：不得 mock Runner 成功；Node、Codex CLI、后端健康、注册、领取、心跳、结构化回写失败仍必须真实暴露。
- BDD/TDD：先增加失败合同，再改脚本。
- 本地运行态：`int_main` 仍使用 `8081/48081`，不得随机换端口。
- PowerShell：不得使用 `&&`；中文文档使用 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。移除的是本地 Runner token 前置，不绕过后端 Runner 协议或真实 Codex CLI 执行。
- 是否从根因和长期维护角度解决：是。修复本地重启脚本把 tokenless 后端重新带回 token 模式的根因。
- 是否存在临时补丁或绕过：否。

## Cleanup Candidates

- doc/tasks/20260728-codex-runner-tokenless-local-restart/restart-tokenless-int-main-backend.ps1
