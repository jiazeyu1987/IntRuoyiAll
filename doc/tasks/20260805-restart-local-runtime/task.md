# Restart Local Runtime

## Task Goal

Restart the `int_main` local frontend and backend for `E:\IntRuoyi` using the fixed runtime ports.

## Milestones

- [x] Read required local runtime, worktree, PowerShell, and task closeout rules.
- [x] Record current frontend/backend port ownership.
- [x] Stop only confirmed old `int_main` frontend/backend processes.
- [x] Start backend on `48081` and frontend on `8081`.
- [x] Verify backend health and frontend HTTP entry.
- [x] Record final verification and closeout status.

## Expected Verification

- `8081` is served by the local frontend rooted at `E:\IntRuoyi\IntRuoyiFronted`.
- `48081` is served by the local backend rooted at `E:\IntRuoyi\IntRuoyiBackend`.
- `http://127.0.0.1:48081/actuator/health` returns `UP`.
- `http://127.0.0.1:8081/` returns HTTP `200`.

## Applicable Gates

- Local runtime ports remain fixed: frontend `8081`, backend `48081`.
- Port occupants may be stopped only when their command line confirms `E:\IntRuoyi` / `int_main` ownership.
- Unknown or non-IntRuoyi occupants must block the restart; no random port changes or silent skips.
- PowerShell commands avoid `&&`; Chinese task documentation is written with UTF-8-safe methods.

## Current Status

completed

Restart completed after explicit user authorization to stop the worktree-owned `48081` process. Cleanup preview/apply passed with no delete candidates.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按固定本地运行态脚本和端口契约重启。
- `是否存在临时补丁或绕过`：否。
