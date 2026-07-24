# 20260611 演练后端前端启动依赖门禁

## 任务目标

修正恢复演练启动 backend/frontend 时隐式拉起 OnlyOffice 依赖导致端口冲突的问题。演练流程应只启动本次恢复验证需要的 backend/frontend，不自动启动 compose 中声明的非目标依赖服务。

## 里程碑

- [x] M1 记录真实流程失败原因和 BDD 场景。
- [x] M2 补充 RED 测试，要求演练启动命令使用 `--no-deps`。
- [x] M3 实现演练启动依赖隔离，不修改测试服已有服务端口或状态。
- [x] M4 运行回归验证并提交本任务改动。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `git diff --check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。启动范围由显式服务和 `--no-deps` 控制，避免依赖服务副作用。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 完成记录

- `Start-BackupAppServices` 支持显式 `NoDeps` 参数。
- `Start-BackupOpsFrontendBackend` 启动 backend/frontend 时使用 `docker compose up -d --no-deps backend frontend`。
- 验证结果：相关 pytest 回归通过。
