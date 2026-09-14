# 20260728 本地后端重启

## Task Goal

重启当前 `E:\IntRuoyi` 主工作区 `int_main` 本地后端，使 `48081` 重新由确认归属的本项目 Java 后端监听，并通过 `/actuator/health` 验证运行状态。

## Milestones

- [x] 启动门禁：读取本地运行态、worktree、任务收尾、PowerShell 编码和 PowerShell 编排规则。
- [x] 经验门禁：读取 `docs/experience-index.md` 并命中本地重启、Runner token、稳定运行 Jar、标准输出阻塞和脏工作区规则。
- [x] 端口归属：确认 `48081` 由 `E:\IntRuoyi\output\runtime\int_main` 下当前运行 Jar 的 Java 进程监听。
- [x] 重启执行：已停止确认归属的旧后端进程，恢复 Docker Engine 和本地 MySQL/Redis 依赖后，使用稳定运行 Jar 启动新后端。
- [x] 验证：`48081` 由新后端 PID `39004` 监听，`http://127.0.0.1:48081/actuator/health` 返回 `UP`。
- [x] CLOSEOUT：执行 task-closeout-cleanup preview/apply，保留核心任务记录。

## Expected Verification

- `Get-NetTCPConnection -LocalPort 48081 -State Listen`
- `Invoke-RestMethod http://127.0.0.1:48081/actuator/health`
- 新进程命令行归属 `E:\IntRuoyi\output\runtime\int_main`，运行 Jar 修改时间不晚于进程启动时间。

## Applicable Gates

- 本地运行态端口门禁：`48081` 只属于 `E:\IntRuoyi` 的 `int_main` 后端；未知进程不得停止，端口不得随机切换。
- 稳定运行 Jar 门禁：长期运行后端从 `output\runtime\int_main` 的稳定 Jar 启动，不直接锁定 Maven `target` Jar。
- 标准输出阻塞门禁：新后端 stdout/stderr 重定向到稳定运行目录，不写入任务目录，避免 cleanup 与长运行进程冲突。
- Runner token 门禁：复用 `.runtime/codex-test-runner/runner-token.txt`，不在日志中记录 token 明文。
- 脏工作区门禁：当前主工作区有并行脏改；本次只做运行态重启，不提交、不回滚、不从脏源码重新打包。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按本地运行态规则确认归属后重启，不换端口、不改配置。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

旧后端 PID `56272` 已停止。Docker Engine 已通过用户态 `com.docker.backend.exe` 恢复，既有 `int-ruoyi-mysql` / `int-ruoyi-redis` 容器已启动并开放 `23306` / `26379`。新后端 PID `39004` 使用稳定运行 Jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260728-142124.jar` 启动，`48081` 已监听，健康检查返回 `UP`。
