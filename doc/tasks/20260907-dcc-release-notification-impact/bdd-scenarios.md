# BDD 场景

## Purpose and Scope

定义 DCC 新大版本发布后的通知和关联文件影响评估行为，只覆盖新发布，不补历史数据。

## Evidence Reviewed

- `prd.md`、`user-flows.md`、`acceptance-criteria.md`。
- 当前发布、关联文件、站内消息和 major-revision 行为。

## Feature Scenarios

### 发布和后续账本

Given B/1 已批准且 A/1 为当前正式版，When 文控发布 B/1，Then B/1 ACTIVE、A/1 SUPERSEDED，并在同一事务创建唯一后续批次和冻结快照。

### 关联影响评估

Given X 正向关联 Y 且 Z 当前正式版本反向引用 X，When X 发布，Then Y、Z 各生成一个按 Master 去重的影响任务并冻结关系方向和版本。

### 无需升版

Given 当前用户是 Y 的影响负责人，When 选择无需升版并填写原因，Then 任务完成且 Y 的版本、状态和正式指针不变。

### 需要升版

Given 负责人决定 Y 需要升版，When 点击开始升版，Then 只进入现有大版本创建或关联流程，不自动创建、审批或发布版本。

### 通知发送

Given 同一用户命中文件责任人和正式分发对象，When 系统发送发布通知，Then 该用户只收到一条站内消息且可查看全部收件原因。

## Failure Scenarios

Given 后续账本任一必需记录保存失败，When 发布事务提交，Then 整体回滚且旧正式版继续有效。

Given 一个用户消息发送失败，When 其他消息已经发送，Then 文件保持 ACTIVE、成功消息保持 SENT、失败行可单独重试。

Given 负责人缺失或停用，When 生成影响任务，Then 任务 UNASSIGNED 并进入文控异常列表，不静默跳过。

Given 非负责人尝试提交决定，When 请求到达，Then 明确拒绝且任务和文件均不变化。

## Boundary Scenarios

Given 同一发布回调重复执行，When 再次物化批次、通知和任务，Then 数量不增加。

Given 正反向关系同时命中同一 Master，When 生成任务，Then 只创建一项并保存全部方向。

Given A/1 检入 A/2，When 小版本生成，Then 不创建发布通知或影响任务。

Given 收件人在发布后失去 VIEW，When 从消息打开文件，Then 当前权限拒绝正文访问。

## Open Questions

- 截止时间、催办和外部通知渠道不在第一版，待后续单独决策。

## Test Blockers

- 真实 E2E 需要任务自有新文件、正反向关系、文控和影响负责人账号。
- 未获当轮 E2E 和数据库写入授权时，场景只能停留在测试计划和非写入验证。
