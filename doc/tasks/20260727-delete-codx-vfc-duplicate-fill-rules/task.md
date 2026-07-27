# 删除 CODX-VFC 粗洗表单多余填写人规则

## Task Goal

针对租户 `1`、报表 `249d8d8d9b3f4041a3e71951bf603a19`、批记录版本 `134`
的“粗洗工序生产记录”，在确认正式保留规则后备份当前 87 条启用 `FILL`
规则，删除其余 86 条，并验证填写人查询接口和真实页面恢复正常。

## Milestones

- [x] 复现页面“填写人加载失败”并定位后端异常。
- [x] 核对目标表结构、版本沿袭、相邻表单规则和保留规则依据。
- [x] 固化删除前 87 条数据快照与回滚路径。
- [x] 在受控事务内保留 1 条、删除 86 条并校验精确影响行数。
- [x] 复验数据库、登录态接口和真实页面。
- [ ] 完成任务收尾、提交和推送。

## Expected Verification

- 目标范围删除前恰好 87 条启用 `FILL` 规则，删除后恰好 1 条。
- 删除范围固定为 `tenant_id=1`、`route_process_id=0`、
  `batch_record_report_id=249d8d8d9b3f4041a3e71951bf603a19`、
  `batch_record_version_id=134`、`rule_type=FILL`、`enabled=1`、`deleted=0`。
- 保留规则具有可追溯业务依据，不以随机、最新 ID 或静默兜底决定。
- `get-by-report` 不再出现 `TooManyResultsException`，返回业务码 `0`。
- “粗洗工序生产记录”填写人列不再显示“加载失败”。

## Current Status

ready_for_closeout

## Current Blocker

- 无。用户已明确要求最终只保留一条王歆规则。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否；本次按用户要求修复目标测试数据，长期的多辅助行规则与列表摘要查询模型兼容问题不在本次删除范围内。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260727-delete-codx-vfc-duplicate-fill-rules/before-87-rules.sql

## 经验门禁

### 业务数据精确删除

- Trigger: 删除批记录填写人规则、重复规则或 E2E 遗留业务数据。
- Preflight check: 核对真实 schema、精确租户/报表/版本范围、87 条明细、保留规则依据、并发写入进程和删除前快照。
- Blocker: 目标行数不是 87、保留规则无法确认、快照失败、存在同范围写入型 E2E、事务影响行数不是 86。
- Verification: 记录快照校验值、事务内删除数、最终保留数、登录态接口结果和真实页面结果。
- Forbidden action: 不得随机保留、扩大 `WHERE` 范围、强停并发任务、跳过备份或用前端隐藏错误。
- Evidence: `docs/database-rules.md`、`doc/tasks/20260727-delete-duplicate-fill-rules/verification-report.md`。
