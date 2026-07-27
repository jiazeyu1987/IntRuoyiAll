# Bug Regression Evidence

## Symptom

在系统管理测试管理列表点击“执行”后，页面提示“Codex Runner token 无效或未配置”。

## Expected Behavior

执行入口应使用当前有效的 Runner token 完成后端身份校验并开始目标测试项执行；仅当 token 确实缺失、失效或注册状态不一致时才返回明确错误。

## Root Cause

当前 `48081` 后端进程从 `application-local.yaml` 读取的
`yudao.codex-test.runner.token` 为空，因为启动时没有注入 `CODEX_TEST_RUNNER_TOKEN`；
本机 Runner 使用现有受控 token 文件注册，后端因此拒绝该注册。

## Reproduction

- `48081` 当前进程属于 `E:\IntRuoyi\IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar`。
- `CODEX_TEST_RUNNER_TOKEN` 在当前 PowerShell、User 和 Machine 环境均未配置。
- Runner 注册探针使用现有受控 token 文件返回 `{"code":1002031011,"msg":"Codex Runner token 无效或未配置"}`。
- `.runtime\codex-test-runner\codex-runner.stderr.log` 持续记录 Runner 注册失败，未将进程存在误判为在线。

## Regression Test

注册探针作为运行态回归测试：后端与 Runner 使用同一 token 时，注册接口必须返回业务码 `0` 并产生在线会话；token 未对齐时必须返回 `1002031011`。

## Verification

待执行。
