# 删除批记录表单重复填写人规则

## Task Goal

针对批记录表单列表中“粗洗工序生产记录”显示“填写人加载失败”的问题，在确认目标租户、报表 ID、批记录版本和异常规则内容后，保留一条记录并规范为当前版本应继承的正式表单级填写人规则，删除其余 86 条 E2E 辅助规则，并验证接口恢复正常。

## Milestones

- [x] 核对目标表结构、租户、报表 ID、批记录版本和异常规则明细。
- [x] 固化待删除规则快照范围和保留规则选择依据。
- [x] 在事务内删除 86 条重复规则并核对影响行数。
- [x] 复验最终数据库范围稳定为 1 条正式规则。
- [x] 复验规则查询接口和页面行状态。
- [x] 完成任务记录和收尾验证。

## Expected Verification

- 目标唯一范围内删除行数为 86，保留行数为 1。
- 保留行规范为 `scope_key=ALL`、`candidate_source_type=ROLE`、`candidate_source_ids=910405`、`batch_record_version_id=130`，解析成员为“王歆、任丹”。
- `/mes/pro/edhr-process-form-permission-rule/get-by-report` 不再返回 `TooManyResultsException`。
- 不影响其他报表、其他版本、其他租户或其他规则类型。
- 记录原始快照、执行 SQL、影响行数、回滚路径和最终验证结果。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：否；本次按用户要求修复现有异常数据。多单元格填写分配与列表摘要查询的长期模型兼容问题不在本次数据修复范围内，后续再次运行相关写入型 E2E 仍可能重建多条规则。
- `是否存在临时补丁或绕过`：否。

## Current Blocker

- 无。数据修复、登录态接口和真实页面只读复验均已通过。

## Cleanup Keep

- doc/tasks/20260727-delete-duplicate-fill-rules/database-schema-evidence.md
- doc/tasks/20260727-delete-duplicate-fill-rules/before-87-rules.sql
- doc/tasks/20260727-delete-duplicate-fill-rules/repair-86-rules.sql
- doc/tasks/20260727-delete-duplicate-fill-rules/verify-page-readonly.mjs

## 经验门禁

### 业务数据修复

- Trigger: 删除或修复批记录填写人规则等真实业务数据。
- Preflight check: 先核对真实 schema、精确租户/报表/版本范围、重复行明细和保留行选择依据。
- Blocker: 目标范围不唯一、保留规则无法确认、备份/快照失败、影响行数不等于 86 或事务校验失败。
- Verification: 记录原始快照、修复行数、保留行和接口/页面复验结果。
- Forbidden action: 不得猜测保留行、扩大 WHERE 范围、用取最新一条掩盖重复数据或跳过回滚路径。
- Evidence: `docs/database-rules.md`、`docs/release-build-preflight-lessons.md`。
