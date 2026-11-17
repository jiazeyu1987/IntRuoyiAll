> 历史变更记录：该独立配置现已停用；当前主工作区默认使用 `int_main` 的 `8081/48081`，以 `docs/branch-runtime-ports.md` 为准。

# D Main Runtime Port Split

## Decision

- User requested unique local frontend/backend ports for all five base directories.
- Accepted: D-Main becomes `int_main_d` with frontend `8101` and backend `48101`.
- E-Main remains `int_main` on `8081/48081`; Batch, Shedule, and QMS keep their existing pairs.
- Product, database, API, and server-release behavior are unchanged.
- E-Main is currently owned by a separate dirty/conflicted task and is not modified by this change.
