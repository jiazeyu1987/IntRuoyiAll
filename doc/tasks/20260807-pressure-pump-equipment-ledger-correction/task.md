# 压力泵设备台账编码修正

## Task Goal

在本地 `int_main` 系统的租户 `122` 设备台账中完成三项用户明确指定的主数据变更：新增 `C01017 / 撤压机`，将光固机 `A05059` 的设备编码改为 `A05075`，将箱型干燥机 `B09041` 的设备编码改为 `B04091`；同时保持设备 ID、正式设备工序关系和当前全局 MES 工序目录一致。

## Milestones

- [x] 创建任务记录并确认用户指定范围。
- [x] 只读核对目标设备、目标编码唯一性、关联表、设备类型、车间和条码数据。
- [x] 保存精确变更前快照并记录回滚条件。
- [ ] 在单个事务中执行三项主数据变更，任一前置断言失败则整体回滚。
- [ ] 只读验证设备台账、关联表、条码和租户边界。
- [ ] 完成任务记录、cleanup、提交并推送。

## Expected Verification

- 租户 `122` 的未删除设备台账中 `C01017`、`A05075`、`B04091` 各恰好存在一条。
- 旧编码 `A05059`、`B09041` 在租户 `122` 的未删除设备台账中均为零条。
- `A05059 -> A05075` 与 `B09041 -> B04091` 保持原设备 ID 和非编码业务字段不变。
- 新设备 `C01017` 使用已核对的正式设备类型、车间和生产状态；当前租户没有设备条码自动生成配置，因此不得伪造条码记录。
- 设备工序关系及全局 MES 工序目录中的活动设备编码同步更新，不出现旧编码残留。
- 其它租户和其它设备不被修改，事务影响行数与预期完全一致。

## Current Status

in_progress - 写入前置条件、快照和事务脚本已完成核对，尚未修改业务数据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；任一目标记录、目标编码唯一性、关联来源或事务影响行数不符合预期即整体阻塞并回滚。
- `是否从根因和长期维护角度解决`：是；按稳定设备 ID 保留既有设备身份，并同步核对设备台账、关系表和条码中的冗余编码。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/experience-index.md` 存在；未找到直接针对本次三项设备主数据修正的独立经验条目。
- 采用 `docs/database-rules.md` 的真实 schema 核对、精确租户边界、事务影响行数和失败回滚门禁；中文写入使用 UTF-8/ASCII 安全 SQL 路径，不在命令或日志中暴露凭据。
- 采用 `docs/task-closeout-rules.md` 和 `docs/powershell-memory.md` 的共享分支、并发基线、验证、cleanup、提交与推送门禁。

## Cleanup Keep

- doc/tasks/20260807-pressure-pump-equipment-ledger-correction/task.md
- doc/tasks/20260807-pressure-pump-equipment-ledger-correction/execution-log.md
- doc/tasks/20260807-pressure-pump-equipment-ledger-correction/verification-report.md
