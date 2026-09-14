# DCC P4 迁移、运行态重启与提交

## Task Goal

在用户授权范围内，将 DCC 新文件生命周期 P4 迁移应用到测试库，重启本地 48081 后端并完成验证；确认当前 DCC 生命周期代码已安全提交并推送，保留并发任务的无关改动。

## Milestones

- M1 迁移前核对与 P4 首次/重复执行
- M2 48081 标准重启与健康检查
- M3 DCC 回归验证与 Git 提交推送核对

## Expected Verification

- 迁移前后 schema、策略和历史业务数据只读核对
- P4 SQL 静态测试和完整依赖闭包门禁通过
- 48081 新 PID、运行 Jar、health 与 DCC 目标页面可访问
- DCC 后端/前端定向测试通过
- `git diff --check`、提交内容和远端同步状态可核对

## Current Status

completed

Current status: P4 migration, standard 48081 restart, final DCC verification and Git synchronization are complete. Closeout preview/apply passed on the primary `int_main` worktree.

## Constraints

- 仅操作测试库和 `int_main` 本地运行态，不访问远端服务器。
- 不处理历史业务文件，不修改并发任务的 MES 或其他无关改动。
- 日志和文档不得记录密码、令牌、密钥或完整 HMAC。
- 迁移失败必须停止并保留真实错误，不使用降级或手工状态回填。

## 设计约束检查

- P4 只转换 DCC `CONTROLLED_FILE / PUBLISH / READY_TO_PUBLISH` 的已发布策略为 `DIRECT`。
- 不更新 DCC 文件、Master、签名或历史版本业务数据。
- 迁移首次执行和重复执行必须得到同一有效策略结果。
- 48081 仅通过项目标准本地重启脚本重启，不使用旧 Jar 或手工降级启动。
