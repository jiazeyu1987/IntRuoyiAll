# 删除生产组长报工管理完成数量小于 10 的测试数据

## Task Goal

删除本机 IntRuoyi 环境中生产组长报工管理里“完成数量 < 10”的测试数据，确保删除范围来自正式报工管理读模型，不扩大到非测试数据或其它租户/角色数据。

## Milestones

- [x] 读取任务、编码、数据库和登录访问门禁。
- [ ] 只读确认生产组长报工管理数据源、表结构、租户范围和候选测试数据。
- [ ] 在事务中按精确条件删除候选测试数据，并记录影响行数。
- [ ] 删除后复核生产组长报工管理候选范围为 0，保留验证证据。
- [ ] 收尾清理并记录最终状态。

## Expected Verification

- 只读 SQL 证明候选数据来自生产组长报工管理正式读模型：`mes_pro_process_pool_event` 的 `PRODUCTION_SUBMIT` 事件及关联数量字段。
- 删除前记录候选事件数量、主键范围、租户范围和数量条件。
- 删除事务每条 DML 后立即保存 `ROW_COUNT()`，并核对影响行数等于预期候选范围。
- 删除后复核同一候选查询返回 0，且不触碰完成数量大于等于 10 的记录。

## Experience Gate Summary

- 已读取 `docs/experience-index.md`；匹配生产组长报工管理随机数据门禁，正式列表来自 `team-leader/submission/page`、`MesTeamLeaderWorkbenchService.getSubmissionPage`、`MesProProcessPoolTimelineReadMapper` 和 `mes_pro_process_pool_event` 的 `PRODUCTION_SUBMIT` 事件。
- 删除或造数都不能只看 `mes_pro_feedback`；必须核对工序池事件、数量片段、租户和 `actual_employee_id` 关联。
- 数据修复 DML 必须保存影响行数，数据修复前后需要复核并发写入风险。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；按正式数据源精确删除测试数据，不使用默认成功或扩大范围。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress - 已完成规则读取和任务建档，正在只读确认数据库连接、表结构与候选删除范围。
