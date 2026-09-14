# 20260810 Team Leader Submission Desc Sort

## Task Goal

- 报工管理列表按提交时间倒序排列，最近提交的报工记录排在最前面。
- 保持生产组长报工管理正式链路：前端报工管理页签调用 /mes/pro/process-pool/team-leader/submission/page，后端分页查询基于 mes_pro_process_pool_event.server_submit_time 排序。

## Milestones

- [ ] 定位报工管理前端页签、API 包装和后端分页 Mapper。
- [ ] 先补 RED 静态合同，锁定 server_submit_time DESC, id DESC。
- [ ] 修改后端分页排序并补充前端请求契约，确保分页第一页就是最新提交。
- [ ] 运行定向验证并记录证据。
- [ ] 收尾前更新验证报告与任务状态。

## Expected Verification

- node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs
- node IntRuoyiFronted/scripts/team-leader-submission-desc-sort-static.spec.cjs
- git diff --check

## Current Status

in_progress

## Experience Gate Summary

- 命中 docs/experience-index.md：生产组长报工管理必须走 team-leader/submission/page、MesTeamLeaderWorkbenchService.getSubmissionPage、MesProProcessPoolTimelineReadMapper 正式链路。
- 命中 docs/backend-development.md#第三方报工直报正式链路门禁：报工管理列表不得用前端随机数据或员工编号兜底；分页结果应来自 mes_pro_process_pool_event 的正式事件数据。本任务只调整正式分页排序，不引入替代数据源。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；排序放在后端分页 SQL，保证分页顺序正确。
- 是否存在临时补丁或绕过：否。
