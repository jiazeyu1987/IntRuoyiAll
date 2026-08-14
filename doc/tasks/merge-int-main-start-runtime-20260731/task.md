# 融合 int_main 并启动 int_shedule 前后端

## Task Goal

- 将当前 `int_shedule` 工作区融合 `int_main` 的最新代码，并按分支端口矩阵启动后端 `48021` 与前端 `8021`。

## Milestones

- [x] 读取任务、worktree、本地运行、端口矩阵、PowerShell/Git 与编码规则。
- [x] 检查当前 Git 分支、remote 与工作区脏状态。
- [x] 融合 `int_main` 并运行分支端口保护检查。
- [x] 检查端口占用，启动后端和前端。
- [x] 验证后端健康检查与前端入口。
- [x] 更新任务记录和验证报告。

## Expected Verification

- `git status --short --branch` 确认融合前状态。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- `http://127.0.0.1:48021/actuator/health` 可访问。
- `http://127.0.0.1:8021/` 可访问。

## 经验门禁

- Trigger: `int_shedule` 分支本地启动后端/前端。
- Preflight check: 使用分支端口矩阵 `8021/48021`，启动前检查端口占用；分支启动脚本不得改写共享 `.env` 或 `application-local.yaml`。
- Blocker: 端口被未知进程、其他 profile 或无关程序占用；Docker MySQL/Redis 依赖端口未监听、认证失败或 schema 不匹配。
- Verification: 记录 Docker 依赖端口、后端分支端口监听、`/actuator/health` 状态和前端入口 HTTP 状态。
- Forbidden action: 禁止换用 `8081/48081`、随机端口、mock 启动成功、强杀未知进程或打印 secret。

## Current Status

completed

## Closeout Reopen

- 最终状态复核发现失败的 Windows tar 解包在 `IntRuoyiBackend/.image/` 留下 56 个未跟踪乱码重复图片。
- 这些路径不属于目标提交，且初始工作区无未跟踪文件；已仅删除 `git ls-files --others --exclude-standard -z` 返回的上述精确路径，并重新通过 cleanup preview/apply 与运行态验证。

## Resolved Blocker

- `git merge --no-edit origin/int_main` 被后台 `git merge-tree/read-tree` 卡住并留下半写索引/工作区状态；标准 `git merge --ff-only origin/int_main` 与 `git reset --merge origin/int_main` 均被失败合并遗留文件阻塞。
- 用户回复“继续”后授权执行破坏性收敛。由于 `git reset --hard` 和整树 checkout 持续卡住，最终通过独立目标 index、同提交干净源文件复制、目标 blob 校验和原子更新分支引用完成精确收敛。
- 最终 `HEAD=e9eca0b386a7a01b28421084a937792245609d8f`，与 `origin/int_main` 一致；`git -c core.safecrlf=false diff --name-only` 为空。

## Cleanup Keep

- `doc/tasks/merge-int-main-start-runtime-20260731/backend-runtime.stdout.log`
- `doc/tasks/merge-int-main-start-runtime-20260731/backend-runtime.stderr.log`
- `doc/tasks/merge-int-main-start-runtime-20260731/frontend-runtime.stdout.log`
- `doc/tasks/merge-int-main-start-runtime-20260731/frontend-runtime.stderr.log`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，最终工作区、index 与分支引用均精确匹配目标提交，并沉淀 Windows 大规模检出半写恢复门禁。
- `是否存在临时补丁或绕过`：否。
