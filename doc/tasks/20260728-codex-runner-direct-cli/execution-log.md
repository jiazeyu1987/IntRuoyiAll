# Execution Log

## 2026-07-28

- User intent: 测试管理按需启动仍报错，启动脚本因 `Frontend entry is not reachable: http://127.0.0.1:8081` 退出；用户要求改为“裸调/裸掉 Codex CLI”。
- BDD: On-demand Runner startup should not require frontend entry preflight -> Given Runner token/backend/Codex CLI prerequisites are present and frontend entry is temporarily unreachable, When backend starts the on-demand Runner script, Then the script must not fail before Codex CLI/Runner registration because of frontend entry reachability.
- Status: in_progress。
