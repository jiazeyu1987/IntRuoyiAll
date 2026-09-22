# 提交前后端代码并重启本机服务

## Goal
按用户要求，先提交当前前后端代码，再重启 E:\IntRuoyi 的 int_main 前后端。

## Milestones
1. completed：核对前后端改动范围、提交前检查，独立提交当前代码基线。
2. completed：完整后端打包预检，使用标准入口重启前后端。
3. blocked：后端 UP，前端初次 HTTP200 后复查20秒与60秒均超时；保留日志，前端可用性未通过。

## Expected Verification
端口门禁；Git staged 清单与提交 hash；Maven package（保留测试编译）；前端 HTTP 200、后端 health UP；仅运行态检查，不执行 E2E。

## Current Status
blocked：代码已提交、后端已重启且UP；前端已重启但最终HTTP检查连续超时，不能宣称前端恢复。未授权推送。

## 设计约束检查
不改业务代码，不替代其他任务的 BDD/TDD 证据；用户已明确授权提交当前前后端代码并重启。只提交前后端源码、测试及必需配置/SQL，排除临时产物。没有推送或数据库写入授权；标准启动涉及迁移时必须先完成只读探针，缺失即阻塞。保持 8081/48081，确认进程归属后方可停止。


## Cleanup Keep

- doc/tasks/20260922-commit-restart-local/restart-retry.log
