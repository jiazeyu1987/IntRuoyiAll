# Start Local Frontend And Backend Runtime

## Task Goal

Start the `int_main` local backend and frontend from `E:\IntRuoyi`, using the fixed ports `48081` and `8081`, then verify both runtime entry points.

## Milestones

- [x] Read required local runtime, worktree, task, PowerShell, and encoding rules.
- [x] Identify task directory and applicable experience gates.
- [ ] Confirm required ports are safe to use.
- [ ] Dispatch backend and frontend startup.
- [ ] Verify backend health and frontend HTTP entry.
- [ ] Record closeout status and blockers.

## Expected Verification

- Port `48081` is either free or owned by a confirmed same-profile old backend before startup.
- Port `8081` is either free or owned by a confirmed same-profile old frontend before startup.
- `GET http://127.0.0.1:48081/actuator/health` returns `UP`.
- `GET http://127.0.0.1:8081/` returns HTTP `200`.

## Applicable Gate Summary

- Local runtime gate: use `int_main` fixed ports `48081/8081`; do not switch ports or stop unknown processes.
- Worktree gate: `E:\IntRuoyi` is the `int_main` baseline workspace; non-`int_main` profiles must not use `48081/8081`.
- Runtime script gate: standard restart uses `IntRuoyiFronted`, stable backend runtime jar copies, persistent Runner token, and `output/runtime/<profile>/logs`.
- PowerShell gate: no `&&`; record exit codes and do not print or commit secrets.
- Dirty workspace gate: the repository already has many unrelated dirty changes; this task may not stage, commit, revert, or clean unrelated task artifacts.

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅按既有标准运行态脚本启动，不改端口、不改配置、不替换依赖。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
