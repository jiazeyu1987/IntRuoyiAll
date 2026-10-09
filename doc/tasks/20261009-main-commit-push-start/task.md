# 主干提交推送与本机前后端启动

## Goal
按用户要求提交并推送 int_main 当前代码，然后启动当前主干前后端。

## Milestones
1. 已完成：确认主干 int_main、39 个既有改动文件、远端和空闲端口 8081/48081；启动依赖已存在。
2. 已完成：定向测试与完整后端打包通过，39 文件提交为 29df5ede2；已推送 origin/int_main，ahead/behind 为 0。
3. 已完成：并行任务已在本次构建期间启动同一主干前后端；运行 Jar 全部 851 个归档条目与本次构建逐字节一致，前端 HTTP 200、后端 HTTP 200 / UP；未打断共享服务。
4. 已完成：经验核对及 cleanup preview/apply 通过，7 个本任务临时清单已移除，3 份核心记录保留；收尾记录独立提交推送。

## Expected Verification
- 提交前后检查 Git 状态、暂存文件范围、差异格式和分支端口合同。
- 构建/启动脚本成功；前端 HTTP 200，后端 /actuator/health 为 HTTP 200 / UP。
- 前后端进程属于当前主干及规定端口 8081/48081；运行 Jar 位于稳定运行目录。
- cleanup preview/apply 仅处理本任务，最终主干与 origin/int_main 同步。

## 设计约束检查
- 已读取 AGENTS.md、task-closeout-rules.md、local-runtime.md、branch-runtime-ports.md、worktree-restrictions.md 和 PowerShell 编码规则。
- 当轮用户明确授权 Git 提交/推送和本机前后端启动；不执行远端发布或 E2E。
- 保留已有业务实现；不添加 fallback、不清理其他任务文件、不停止未知归属进程。
- 标准后端启动采用 BackendSchemaReadOnly，缺少 schema 时准确阻塞，不自动写数据库。
- 本任务文档与既有改动分开提交，日志不记录凭据。

## Current Status
completed

最终验证：代码基线已推送；前端 HTTP 200、后端 HTTP 200 / UP；归档内容与本次构建一致；cleanup PASS。

## Cleanup Candidates
- .git/main-commit-push-start-paths.txt
- .git/main-commit-push-start-manifest.json
- .git/main-start-schema-probes.json
- .git/main-start-schema-results.json
- .git/main-start-build.json
- .git/main-start-running.json
- .git/main-start-jar-diffs.json

## Cleanup Keep
- doc/tasks/20261009-main-commit-push-start/task.md
- doc/tasks/20261009-main-commit-push-start/execution-log.md
- doc/tasks/20261009-main-commit-push-start/verification-report.md
