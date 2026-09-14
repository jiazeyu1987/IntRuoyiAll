# Frontend Feature Evidence

## Feature Goal And Non-Goals

- Goal: 报工管理列表默认展示最近提交的报工记录。
- Non-goal: 不改变报工历史、分配、审核、筛选字段或数据来源。

## Requirements And Acceptance IDs

- A1: 生产组长报工管理页签的第一页必须来自正式分页接口，且后端按 server_submit_time DESC, id DESC 返回。
- A2: 前端继续调用 /mes/pro/process-pool/team-leader/submission/page，不做本地分页后排序。

## UI Entry Points, Routes, Components, Owned Files

- UI: IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue 的“报工管理”页签。
- API: IntRuoyiFronted/src/api/mes/pro/processpool/teamLeader.ts#getTeamLeaderSubmissionPage。
- Backend query: IntRuoyiBackend/yudao-module-mes/src/main/resources/mapper/pro/processpool/MesProProcessPoolTimelineReadMapper.xml。

## API Contracts And Data States

- 请求保持 leaderType=PRODUCTION 与 allocationView=WORKBENCH。
- 排序使用服务端提交时间 server_submit_time，时间相同用 pool_event.id 保持稳定倒序。

## BDD Scenarios

- BDD: 报工管理按最近提交优先展示 -> Given 生产组长进入“报工管理”页签并请求正式报工分页; When 后端返回多条不同提交时间的报工事件; Then 第一页按服务端提交时间倒序返回，时间相同时按事件 ID 倒序稳定排列。

## RED Command And Expected Failure

- RED: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> FAIL；现有 Mapper 仍为 ORDER BY pool_event.server_submit_time ASC, pool_event.id ASC。

## GREEN Command And Passing Result

- GREEN: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> PASS。
- GREEN: node IntRuoyiFronted/scripts/team-leader-submission-desc-sort-static.spec.cjs -> PASS。
- git diff --check -> PASS；仅输出既有 LF/CRLF 工作区提示，无 whitespace error。

## Responsive, Accessibility, Loading, Empty, Error, Permission Checks

- 本次不改 UI 结构；现有 loading、empty、error 和权限入口保持原样。

## E2E Or Component Verification Path

- 采用静态合同验证分页 SQL 和前端 API 调用契约。若需要真实页面 E2E，需先确认本地前后端运行态和登录账号。

## Blockers And Follow-Up Skills

- 暂无。
