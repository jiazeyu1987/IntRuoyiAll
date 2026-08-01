# 第三方报工导入列表与排产进度修复

## Task Goal

修复本地服务器中芋道源码租户使用 admin 导入 `李萍.xlsx` 后，确认弹框后报工列表为空、排产工单进度未更新的问题。导入成功必须落到正式报工链路，并让报工列表与排产工单进度在真实路径验证中正确更新；如果目标排产工单已满量不再更新，也必须有正式链路证据证明不更新是正确结果。

## Milestones

1. 建立任务记录、读取适用规则，并保存既有脏工作区基线提交。
2. 通过现有代码、测试、接口和必要的真实页面路径复现导入后列表/进度异常。`completed`
3. 定位根因并补充 RED 回归测试，证明导入成功未进入正式报工或未刷新正式列表/排产进度。`completed`
4. 实施最小正式修复，不引入 fallback、默认成功或前端假新增。`completed`
5. 运行后端/前端定向回归和真实 Playwright 导入验证，确认报工列表与排产工单进度正确。`completed`
6. 更新验证报告、沉淀经验、清理任务自有临时产物，提交并推送。`completed`

## Expected Verification

- BDD 记录覆盖第三方导入成功、弹框确认后报工列表刷新、排产工单进度刷新或满量不更新。
- RED 先失败于当前缺陷的最小后端或前端静态/单元测试。
- GREEN 通过目标后端回归测试，至少覆盖匹配行创建/提交正式报工、缺用户跳过、重复导入再次正式报工、超剩余跳过。
- 前端静态合同确认导入确认后刷新正式报工列表，并广播受影响排产工单刷新 payload。
- 真实 Playwright 路径使用本机已登记运行态、芋道源码/admin 身份和用户指定 Excel 文件完成导入，并用页面或只读 API 证明报工列表和排产工单进度结果正确；若主工作区 `48081` 被无关任务占用，则使用正式登记的成对 worktree 端口隔离验证。

## Applicable Gates

### 第三方报工直报正式链路门禁

- Trigger: 第三方报工、李萍报工单、直接报工 Excel、`importDirectWorkReportWorkbook`、`DIRECT_WORK_REPORT`、导入结果弹框显示成功但正式报工列表无新增、排产进度疑似未增长。
- Preflight check: 先确认导入成功路径是否创建 `MesProFeedbackDO`、设置 `sourceImportRecordId`、回写导入记录 `feedbackId`、调用正式提交服务，并由正式报工状态参与排产进度汇总。
- Blocker: 若缺少报工人、审批人、唯一未完成任务、排产工序剩余数量或正式路线工序快照，不得写 `progressSourceType=DIRECT_WORK_REPORT` 伪造成功；必须返回结构化跳过原因或 fail fast。
- Verification: 后端回归必须同时覆盖匹配行创建/提交正式报工、缺用户跳过、重复导入再次正式报工、超剩余跳过；前端静态合同需确认导入确认后刷新正式报工列表并广播受影响排产工单刷新 payload。
- Forbidden action: 禁止用导入记录直接进度、前端假新增、默认成功、空列表刷新或 API-only 结果替代正式报工持久化链路。
- Evidence: 本任务执行日志和验证报告。

## Baseline

- Dirty workspace baseline commit before this task: `b99246f58 chore: baseline dirty workspace before feedback import fix`.
- Baseline scope: committed pre-existing DCC, role matrix, test-management docs/rules and related untracked artifacts before current task edits.

## Current Status

completed

## Closeout Status

- 本任务后端代码、目标回归、完整后端构建和真实 Playwright 导入验证已通过。
- 由于主工作区 `48081` 仍由无关任务运行态占用，最终 E2E 在当前任务 worktree 的正式登记成对端口完成：前端 `8090`、后端 `48090`、slot `9`。
- 真实导入结果：正式报工 `FB-000643` 出现在报工列表，排产工单 `SCH-881MO093613-20260707-0001` 进度更新为 `completedQuantity=4995`、`uncompletedQuantity=21005`、`progressPercent=19.211538`。
- 实现与验证证据已提交到任务分支 `codex/third-party-feedback-import-20260802`，实现提交 `b8533d59a1`。
- cleanup preview/apply 已执行并只删除本任务临时产物，保留 `task.md`、`execution-log.md`、`verification-report.md` 和真实 E2E 验证脚本。

## Cleanup Keep

- doc/tasks/20260802-third-party-feedback-import-list-progress/verify-direct-work-report-import-real.e2e.js

## Cleanup Candidates

- doc/tasks/20260802-third-party-feedback-import-list-progress/direct-work-report-real-e2e-result.json
- doc/tasks/20260802-third-party-feedback-import-list-progress/mes-fix-worktree.patch
- doc/tasks/20260802-third-party-feedback-import-list-progress/start-patched-backend.ps1

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是让第三方导入进入正式报工持久化与排产进度汇总链路。
- `是否存在临时补丁或绕过`：否。
