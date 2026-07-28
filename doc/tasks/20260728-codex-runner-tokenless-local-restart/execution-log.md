# Execution Log

## 2026-07-28

- User intent: 用户反馈仍提示 `Codex Runner token 无效或未配置`；此前已要求“裸调 Codex CLI”，本轮定位到本地后端重启脚本仍注入 `CODEX_TEST_RUNNER_TOKEN`。
- BDD: Tokenless local backend restart -> Given `int_main` backend is restarted through `restart-int-ruoyi-local.ps1`, When test management starts the on-demand Runner, Then backend runtime must not be configured with `CODEX_TEST_RUNNER_TOKEN`, and Runner requests must use tokenless local protocol unless an explicit token is configured elsewhere.
