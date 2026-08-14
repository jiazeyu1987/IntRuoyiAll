# Backend API Evidence

## Endpoint, Service, Job, Or Handler Scope

- Endpoint: `GET /admin-api/mes/pro/process-pool/team-leader/submission/page`。
- Service: `MesTeamLeaderWorkbenchService#getSubmissionPage` -> `ProcessPoolTimelineService#getTimelinePage`。
- Mapper: `MesProProcessPoolTimelineReadMapper.xml#selectTimelinePage`。

## API Contract And Data Contract

- API request/response shape unchanged.
- Data contract: submission page rows are ordered by `mes_pro_process_pool_event.server_submit_time DESC, pool_event.id DESC`.

## Auth, Permissions, Validation, And Error Behavior

- Auth and scope checks unchanged: PQC leader visibility remains constrained by `MesTeamLeaderWorkbenchServiceImpl` responsible employee scope.
- Validation unchanged: submitDate remains optional and is translated to a closed-open server submit time window only when provided.
- Error behavior unchanged: backend exceptions and frontend request failures are still surfaced through existing error paths.

## Required Config, Services, Fixtures, And Migrations

- No new config.
- No schema migration.
- No external fixture required; mapper/static and focused service tests cover the ordering contract.

## BDD Scenarios

- BDD: PQC管理列表最近提交优先 -> Given PQC组长打开“PQC管理”列表且存在多条不同提交时间的 PQC 提交记录 / When 列表通过正式分页接口加载 / Then 第一页按服务端提交时间倒序返回，提交时间相同按事件 ID 倒序稳定排列，最近提交记录排在最前面。
- BDD: 排序不在前端当前页伪造 -> Given PQC管理列表通过服务端分页加载 / When 用户切换页码或筛选条件 / Then 前端直接使用正式分页返回顺序，后端按提交时间倒序和事件 ID 倒序提供稳定跨页排序。

## RED Command And Expected Failure

- RED: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> FAIL，mapper 缺少 `ORDER BY pool_event.server_submit_time DESC, pool_event.id DESC`。
- RED: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL，新增测试引用倒序排序预期时现有测试支持/实现仍为升序；早期 RED 还确认缺少排序字段版本会在 testCompile 阶段失败，随后收敛为后端固定排序。

## GREEN Command And Passing Result

- GREEN: `node yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs` -> PASS。
- GREEN: `mvn -pl yudao-module-mes "-Dtest=ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 2, Failures: 0, Errors: 0, Skipped: 0。

## Contract Or Integration Verification

- Static contract verifies mapper ordering is `server_submit_time DESC, id DESC` and rejects `server_submit_time ASC, id ASC`.
- JUnit verifies `ProcessPoolTimelineServiceImpl` returns the latest submitted event first through the in-memory read mapper.

## Observability Touchpoints

- No logging or metrics changed.

## Blockers And Downstream Skill Needs

- Blockers: 无。
- Downstream skills: 无。
