# 执行日志

## 用户意图

- 重启本地前端和后端。

## 场景

- `BDD: int_main 本地前后端重启 -> Given 8081/48081 当前运行态归属 E:\IntRuoyi；When 停止旧进程并通过标准脚本启动；Then 前端返回 HTTP 200、后端 health 返回 UP，且新 PID 归属 int_main。`

## 执行记录

- 已读取 `docs/local-runtime.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-memory.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`，命中本地重启、tokenless Runner、稳定运行 Jar、前端 pnpm 链接及端口归属门禁。
- 启动前端口：`8081` 由 PID `9040` 监听；`48081` 由 PID `52824` 监听。
- Git 状态：分支 `int_main`，工作区无文件改动，落后 `origin/int_main` 3 个提交。

## 当前状态

- 正在核对标准重启脚本和旧进程命令行归属。
