# Verification Report

## Summary

- 当前前后端正式代码已提交为四个快照提交：`61ba202942b4399fa274a0a4fe0b488fb4a030e1`、`e3b8691b03eee9be07297c8b54bf4363c2b01332`、`052c73596` 与 `4e97a301bd611ae19cae1d428ee34b55f42a901f`。
- 提交后前后端 tracked/untracked 代码残余为 0。
- 本任务未将其它任务的产品验证阻塞伪装为通过；只验证 Git 提交/推送所需的范围、凭据、大文件、空白和端口契约门禁。

## Passed Gates

- `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main` 使用前端 8081、后端 48081。
- `git diff --cached --check` -> PASS，提交前两轮暂存均通过。
- 暂存范围审计 -> PASS，正式代码提交只包含 `IntRuoyiBackend/` 与 `IntRuoyiFronted/`。
- 凭据/冲突标记审计 -> PASS，未命中私钥、常见 token、AWS key 或 Git 冲突标记。
- 大文件门禁 -> PASS，待推送历史最大 blob 为 1,448,982 字节，超过 100 MB 的 blob 为 0。
- 远端连通性 -> 初始 `git ls-remote origin HEAD` 因 TLS unexpected EOF 失败；使用已监听的 GitHub 代理 `127.0.0.1:7890` 一次性配置复验通过。
- task-closeout-cleanup preview/apply -> PASS，三份核心任务记录保留，delete/blocked/warnings 均为空。
- 首次推送 -> PASS，远端 `origin/int_main` 与本地 HEAD 均为 `4e97a301bd611ae19cae1d428ee34b55f42a901f`。

## Known Non-Product Verification

- 本任务是 Git 提交/推送任务，没有新增生产行为实现；不运行 Maven、pnpm 或 Playwright 全量产品回归。
- 工作区中其它任务文档仍记录各自的 `in_progress`、`ready_for_closeout` 或 `blocked` 状态；这些状态不作为本任务通过证据。
