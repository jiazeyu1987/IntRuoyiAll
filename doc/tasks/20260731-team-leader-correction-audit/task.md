# 20260731 班组长复核修改与 PQC 提交日志

## Task Goal

让生产组长和 PQC 组长可以对负责员工提交逐条判定“正确/不正确”，对不正确内容发起带原因的修改，并保证组长修改、复核判定和 PQC 检验员提交均有可追溯日志记录。

## Milestones

- [x] 核对现有班组长复核、审核副本、原始修改历史、PQC 提交事件和日志表/服务边界。
- [x] 补充 BDD/TDD：复核判定、组长修改写日志、PQC 提交日志可见。
- [x] 实现最小正式链路，复用事件修订、提交复核和时间轴日志能力，不新增 fallback 或默认成功。
- [x] 更新组长工作台前端入口与静态契约。
- [x] 运行前后端定向验证，更新验证报告并进入收尾。

## Expected Verification

- `node tests/e2e/mes-process-pool-team-leader-static.spec.js`
- `mvn -pl yudao-module-mes -am "-Dtest=MesProcessPoolTeamLeaderControllerTest,MesTeamLeaderSubmissionReviewServiceTest,MesProcessPoolPqcEventTest,MesProcessPoolEventRevisionServiceTest,MesProcessPoolEventRevisionControllerContractTest,ProcessPoolTimelineQueryTest,ProcessPoolTimelineRevisionSummaryTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm ts:check`

## Experience Gates

- `前端静态契约隔离门禁`：已先用任务专用静态契约 RED/GREEN 锁定当前需求，未用全量无关失败替代。
- `一对多读模型聚合门禁`：最新组长复核日志通过 `ROW_NUMBER()` 聚合后 JOIN，列表不因复核历史重复。
- `GitHub HTTPS 443 本地代理门禁`：当前共享工作区存在大量并行脏改和分支 ahead，未执行提交/推送；收尾提交前需重新确认 Git 状态。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。修正入口缺少原始 payload、签名、原因或字段差异时直接报错，不默认成功。
- `是否从根因和长期维护角度解决`：是。复核判定走 `mes_pro_process_pool_submission_review`，修改不正确内容走正式 `event-revision` 修订日志链路，PQC 提交继续走工序池事件与 `mes_pro_process_pool_pqc_record`。
- `是否存在临时补丁或绕过`：否。

## Current Status

ready_for_closeout

## Closeout Blockers

- 当前分支 `int_main` 已领先 `origin/int_main` 且工作区存在大量其它任务改动；为避免混入非任务文件，未执行提交和推送。
