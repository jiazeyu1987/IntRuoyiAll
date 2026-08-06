# Backend API Evidence

## Scope

时间轴报工读模型补齐最新组长复核人姓名，用于生产组长报工历史展示“审核通过人”。

## Data Contract

- `MesProProcessPoolTimelineReadMapper.xml` 读取 `review_leader.nickname AS submissionReviewLeaderUserName`。
- `ProcessPoolTimelineEventReadDO`、`ProcessPoolTimelineEventRespVO`、前端 VO 均包含 `submissionReviewLeaderUserName`。
- `ProcessPoolTimelineServiceImpl.copyEventFields` 将字段从 DO 复制到响应 VO。

## BDD

- `BDD: 报工历史展示审核上下文 -> Given 一条报工记录已被组长审核通过 / When 时间轴分页读取该记录 / Then 响应包含审核通过人姓名和审核通过时间。`

## RED

- `RED: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> FAIL, mapper 缺少 review_leader.nickname AS submissionReviewLeaderUserName。`

## Validation

- mapper 必须从正式 `mes_pro_process_pool_submission_review` 最新复核日志读取 leader_user_id。
- mapper 必须通过 `system_users review_leader` 按租户和 deleted 标记读取审核人昵称。
- DO、RespVO、前端 VO、service copy 链路必须暴露 `submissionReviewLeaderUserName`。

## Verification

- `node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs`：PASS。
- `mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`：PASS。

## GREEN

- `GREEN: node IntRuoyiBackend/yudao-module-mes/src/test/js/process-pool-timeline-mapper-static.spec.cjs -> PASS。`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineRevisionSummaryTest,ProcessPoolTimelineFilterTest,ProcessPoolTimelineQueryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS，8 tests / 0 failures / 0 errors / 0 skipped。`

## Blockers

- 当前工作区存在并行任务改动和未推送提交，本任务未单独 commit/push。