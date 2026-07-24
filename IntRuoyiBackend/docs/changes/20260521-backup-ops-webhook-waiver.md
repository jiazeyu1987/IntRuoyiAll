# Change Request: backup-ops 无 webhook 仍允许最终放行

## 请求

- 来源：当前用户
- 日期：`2026-05-21`
- 请求内容：
  - 允许 `backup-ops` 在未接入真实 `notify.webhook.url` 的情况下试运行
  - 将该状态视为最终放行，而不是继续把 webhook 作为 release blocker

## 当前基线

- 当前 `backup-ops` 已完成：
  - 正式到测试服务器的真实备份
  - 应用回滚
  - 数据恢复
  - 独立 rehearsal 槽位恢复演练
  - 恢复点治理
  - 计划任务注册入口
  - 通知模块真实 webhook 能力与 disabled/pending/fail 可见性
- 当前唯一剩余放行阻塞是：
  - runtime 配置未提供真实 `notify.webhook.url`
  - 默认配置仍为 `notify.enabled=false`、`channel=pending`

## 变更分类

- 分类：release decision / operations waiver

## 影响分析

### 范围影响

- 不再把“真实 webhook 已接通”作为 `backup-ops` 最终放行前置
- 备份、恢复、回滚、演练主链路不受影响

### 运维影响

- IT 不能依赖自动消息接收结果
- IT 在 webhook 补齐前需要人工查看：
  - 任务计划执行结果
  - 本地日志目录
  - 演练报告

### 风险

- 备份失败、恢复失败、演练失败时，不会自动推送到外部通知平台
- 需要人工值守流程补位，否则容易延迟发现问题

### 测试影响

- 不新增代码阻塞
- 现有验证保持有效：
  - `pytest` 全量通过
  - 真实 `backup-now / backup-scheduled / rollback-app / restore-data / rehearsal` 已验证

## 决策

- 结论：`accept`

## 放行口径

- 本次接受“无 webhook 最终放行”作为用户批准的 release waiver
- webhook 从“最终放行阻塞”降级为“后续运维增强项”

## 约束

- 必须保留当前显式行为：
  - `notify.enabled=false`
  - `notify.channel=pending`
  - 操作结果中明确显示“通知未发送”，不得假装已通知

## 后续动作

- 后续如拿到真实通知目标，再补：
  - `notify.webhook.url`
  - 真实通知联调验证
  - 运维 SOP 从“人工看日志”切回“自动通知 + 人工抽查”
