# Backend API Evidence

## Scope

- 班组长复核接口继续只写入复核日志：`eventId`、登录态组长、`leaderType`、`reviewStatus`、`reviewRemark`、服务端复核时间。
- 工序池时间轴读模型新增最新复核日志字段：`submissionReviewStatus`、`submissionReviewRemark`、`submissionReviewLeaderUserId`、`submissionReviewedAt`。
- 修改不正确内容不新增班组长覆盖接口，继续复用正式 `event-revision/update-original`，记录 before/after payload、原因、修改人、修正签名和字段 diff。
- PQC 提交日志继续由工序池事件和 `mes_pro_process_pool_pqc_record` 保存，回归测试覆盖 raw payload、提交人、签名和服务端提交时间。

## API Contract and Data Contract

- `POST /mes/pro/process-pool/team-leader/submission/review`：只接收复核判定和说明，不接收 corrected payload。
- `POST /mes/pro/process-pool/event-revision/update-original`：正式修正入口，要求 `afterPayload`、`changeReason`、`revisionSignatureId`、`revisionSignatureUserId`、`revisionSignatureSnapshot`、`modifiedByUserId`、`changedFields`。
- `GET /mes/pro/process-pool/team-leader/submission/page` / `detail`：返回最新复核日志字段，供组长列表和详情回看。

## Auth, Permissions, Validation, Error Behavior

- 班组长复核仍由后端从登录态注入组长用户，不允许前端传 `leaderUserId`。
- 修正缺少签名、原因、合法 payload、字段差异或 FIFO 锁状态时继续 fail fast。
- 未引入 fallback、mock success、默认成功或静默吞错。

## BDD

- `BDD: 组长判定员工提交是否正确 -> Given 员工或PQC检验员提交了一条工序池事件 / When 组长在检查列表复核该提交 / Then 组长可以标记正确或不正确并保存复核说明`
- `BDD: 组长修改不正确内容留痕 -> Given 组长判定提交内容不正确 / When 组长提交修正后的字段内容和修改原因 / Then 系统保存修正内容并记录修改前、修改后、修改人、修改时间和原因日志`
- `BDD: PQC提交日志可追溯 -> Given PQC检验员提交过程检验内容 / When 组长或审核视图查看该提交 / Then 系统展示PQC提交日志，包含提交人、提交时间、原始payload和提交事件编号`

## RED

- `RED: node tests\e2e\mes-process-pool-team-leader-static.spec.js -> FAIL, 缺少 submissionReviewStatus 等组长复核日志字段`
- `RED: mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> FAIL, ProcessPoolTimelineEventRespVO 缺少复核日志 getter`

## GREEN

- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=ProcessPoolTimelineQueryTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 3 tests`
- `GREEN: mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolPqcEventTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionControllerContractTest,ProcessPoolTimelineQueryTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -> PASS, 22 tests`

## Contract or Integration Verification

- 时间轴 mapper 使用 `ROW_NUMBER()` 聚合每个事件最新复核日志，避免复核历史一对多 JOIN 造成列表重复。
- 复核接口未携带修正 payload；修正仍由事件修订接口完成。

## Observability Touchpoints

- 复核日志字段直接来自 `mes_pro_process_pool_submission_review`。
- 修正日志来自 `mes_pro_process_pool_event_revision` 和 `mes_pro_process_pool_event_revision_diff`。
- PQC 提交日志来自工序池事件与 `mes_pro_process_pool_pqc_record`。

## Blockers

- 提交/推送未执行：当前共享工作区已有大量并行改动且分支 ahead，需在独立收尾窗口处理。
