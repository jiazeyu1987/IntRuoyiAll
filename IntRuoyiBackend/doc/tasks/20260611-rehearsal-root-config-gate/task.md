# 20260611 演练目录配置门禁修正

## 任务目标

将备份恢复演练配置中的 `servers.test.rehearsalRoot` 修正到安全门禁允许的 `/backup/int-ruoyi/rehearsal/*` 范围，避免 DCC 增量备份真实流程在 B3/B4/B5 备份完成后因演练目录配置不一致被 `INTBK-7001` 阻断。

## 里程碑

- [x] M1 记录真实流程失败原因和 BDD 场景。
- [x] M2 补充配置一致性的 RED 测试。
- [x] M3 修正备份恢复配置与示例配置。
- [x] M4 运行回归验证并提交本任务改动。

## 预期验证

- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `python -X utf8 -m pytest script\tests\test_backup_ops_linux_runtime_ports.py script\tests\test_backup_ops_linux_runtime_rehearsal_tooling.py -q`
- `git diff --check`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。配置默认值与既有安全门禁保持一致，不放宽门禁。
- `是否存在临时补丁或绕过`：否。

## 当前状态

- 状态：已完成。
- 阻塞：无。

## 完成记录

- 修正 `backup-ops.config.json`、`backup-ops.config.example.json`、`backup-ops.linux-local.example.json` 的测试演练目录默认值。
- 增加配置一致性测试，确保默认配置保持在 `/backup/int-ruoyi/rehearsal/*` 安全范围内。
- 验证结果：相关 pytest 回归通过。
