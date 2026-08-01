# 20260801 restart local frontend backend

## Task Goal

重启 `E:\IntRuoyi` 主工作区 `int_main` 本地前端与后端运行态，保持固定端口前端 `8081`、后端 `48081`，并验证服务可访问。

## Milestones

- [x] 读取本地运行、worktree、PowerShell、编码和任务收尾规则。
- [x] 建立端口归属证据，确认只停止 `E:\IntRuoyi` / `int_main` 旧进程。
- [x] 重启后端并验证 `http://127.0.0.1:48081/actuator/health`。
- [x] 重启前端并验证 `http://127.0.0.1:8081/`。
- [x] 写入验证报告与最终状态。

## Expected Verification

- 端口 `48081` 监听进程归属 `E:\IntRuoyi\IntRuoyiBackend` 或本任务启动的新后端进程。
- 端口 `8081` 监听进程归属 `E:\IntRuoyi\IntRuoyiFronted` 或本任务启动的新前端进程。
- 后端健康检查返回 `status=UP`。
- 前端首页请求返回 HTTP `200`。

## Applicable Gates

- 本地运行门禁：不得换端口、不得强杀未知进程；端口被旧 `int_main` 进程占用时，记录 PID、命令行和归属依据后停止再启动。
- worktree 门禁：`8081/48081` 只属于 `E:\IntRuoyi` 的 `int_main`，非 `int_main` 或未知 worktree 占用必须阻塞。
- PowerShell 门禁：命令不用 `&&`；关键命令记录退出码与必要摘要。
- 编码门禁：中文 Markdown 使用 UTF-8，优先通过 `apply_patch` 写入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务仅执行标准本地运行态重启与真实端口验证。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Verification Summary

- 后端：`48081` 新监听 PID `52620`，运行 Jar `output/runtime/int_main/backend-runtime-control-20260801-102211.jar`，health 返回 `UP`。
- 前端：`8081` 新监听 PID `23752`，命令归属 `E:\IntRuoyi\IntRuoyiFronted` 的 Vite，首页返回 HTTP `200`。
- 重启脚本：`IntRuoyiBackend/script/deploy/restart-int-ruoyi-local.ps1 -Component full` 退出码 `0`。
- Cleanup：`task-closeout-cleanup` preview/apply 均通过，未删除任何路径。
- 经验沉淀：已按 `project-experience-consolidation` 检索现有长期经验，现有 `docs/local-runtime.md` 已覆盖本次本地重启门禁，无需新增长期经验。
