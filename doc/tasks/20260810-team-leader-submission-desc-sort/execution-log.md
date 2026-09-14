# Execution Log

## User Intent

- 用户要求“报工管理的列表倒序排列，就是最近提交的排在最前面”。

## Skill / Rule Inputs

- 使用 frontend-feature-delivery：本次为用户可见前端列表行为改动，需要 BDD、RED/GREEN 和证据文件。
- 已读取 docs/task-closeout-rules.md、docs/frontend-development.md、docs/backend-development.md、docs/powershell-encoding.md。
- 已读取 docs/experience-index.md，命中生产组长报工管理正式链路经验门禁。

## BDD

- BDD: 报工管理按最近提交优先展示 -> Given 生产组长进入“报工管理”页签并请求正式报工分页, When 后端返回多条不同提交时间的报工事件, Then 第一页按服务端提交时间倒序返回，时间相同时按事件 ID 倒序稳定排列。

## TDD Evidence

- RED: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> FAIL, expected reason: 现有 Mapper 仍为 ORDER BY pool_event.server_submit_time ASC, pool_event.id ASC。
- GREEN: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> PASS。
- GREEN: node IntRuoyiFronted/scripts/team-leader-submission-desc-sort-static.spec.cjs -> PASS。
- REGRESSION: git diff --check -> PASS；仅输出既有 LF/CRLF 工作区提示，无 whitespace error。

## Milestone Updates

- in_progress: 已定位前端 TeamLeaderWorkbenchPage.vue 报工管理页签和 teamLeader.ts#getTeamLeaderSubmissionPage，后端入口为 MesTeamLeaderWorkbenchServiceImpl#getSubmissionPage，正式分页 Mapper 为 MesProProcessPoolTimelineReadMapper.xml。
- in_progress: 已将分页 SQL 改为服务端提交时间倒序，并新增前端静态合同防止本地分页后排序。
- in_progress: 定向静态合同与 diff 检查已通过。

## Blockers

- 暂无。
