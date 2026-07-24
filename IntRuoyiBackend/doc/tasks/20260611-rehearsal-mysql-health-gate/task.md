# 20260611 演练 MySQL 健康门禁修正

## 任务目标

修正恢复演练中 MySQL 就绪判断过早的问题：演练栈启动后必须等待 Docker health 为 `healthy` 且 MySQL 可连接，才能导入备份 dump，避免 MySQL entrypoint 临时初始化服务短暂可 ping 后又关闭导致导入失败。

## 里程碑

- [x] M1 记录真实流程失败原因和 BDD 场景。
- [x] M2 补充 RED 测试，要求 MySQL 等待逻辑检查 Docker health。
- [x] M3 实现健康门禁，不引入降级或静默重试成功。
- [x] M4 运行回归验证并提交本任务改动。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `git diff --check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。健康检查未满足时仍然 fail fast。
- `是否从根因和长期维护角度解决`：是。用容器健康状态消除 MySQL entrypoint 临时服务导致的 ready 误判。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 完成记录

- `Wait-BackupOpsMySqlReady` 先等待 Docker health 为 `healthy`，再执行 MySQL 连通性检查。
- 避免 MySQL entrypoint 临时初始化服务短暂可 ping 时误判为可导入。
- 验证结果：相关 pytest 回归通过。
