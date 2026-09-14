# Registration E2E Case Matrix

Source: `E:\IntRuoyi\e2e_test\registration`

Round 1 source run: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round1-latest-20260906-continue/summary.json`

Final run: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round27-post-reminder-closure/summary.json`

Final executable coverage: 15 PASS, 0 FAIL, 0 BLOCKED. The final runner maps the document sections to executable real-page checks across upload, change, renewal, download, reminder, sorting, action-panel, and business-time simulation paths.

Reminder document closure was supplemented by `registration-reminder-config-and-delivery-real.cjs` after the final runner exposed that the runner only covered reminder sort/runtime smoke. Supplemental evidence:
- R25 data prep: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round25-reminder-data/`
- R25 config/delivery/status: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round25-reminder-doc/`
- R26 reconfiguration: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round26-reminder-reconfig-doc/`
- Config restore: `doc/tasks/20260906-registration-all-e2e-worktree/artifacts/round26-reminder-restore/`

Result policy:
- `PASS`: the document case has direct Playwright/real-browser evidence for the required path.
- `FAIL`: the associated real-browser path reached this case and failed.
- `BLOCKED`: the case was not independently reachable in Round 1 because an upstream case, executable coverage gap, route prerequisite, role prerequisite, or runtime preflight failed.
- `SKIPPED`: the documented skip trigger was confirmed before execution. No Round 1 skip trigger has been confirmed yet.

| Document | Case | Title | Skip Condition | Round 1 | Evidence |
| --- | --- | --- | --- | --- | --- |
| biangeng | E2E-1 | 申请人提交注册证变更审批 | No | FAIL | `change-submit-approval` failed on submit with upload-authorization scope error for the fixed applicant/certificate combination. |
| biangeng | E2E-2 | 提交后状态正确 | No | BLOCKED | Upstream change submission did not succeed in `change-submit-approval`; no submitted-state evidence exists. |
| biangeng | E2E-3 | 注册经理待办审批 | No | FAIL | `change-continue-approval` reached approval review and failed with stale row-version error. |
| biangeng | E2E-4 | 审批后当前证正式字段更新 | No | BLOCKED | Approval failed in `change-continue-approval`; no post-approval current-record evidence exists. |
| biangeng | E2E-5 | 变更履历正确留痕 | No | BLOCKED | Approval failed before applied-history assertions could prove the case. |
| biangeng | E2E-6 | 全结构化字段变更正确回显 | No | FAIL | `change-remaining` failed while selecting the structured change option `产品名称`. |
| biangeng | E2E-7 | 注册部经理在详情中直接下载变更批件文件 | No | BLOCKED | Change approval did not complete, so no newly approved change attachment was available for manager download verification. |
| biangeng | E2E-8 | 普通用户申请下载并在 24 小时内下载变更批件文件 | Yes | BLOCKED | Change attachment prerequisite was not produced; documented skip trigger for `wanglixuan` was not confirmed. |
| biangeng | E2E-9 | 普通用户下载授权超过 24 小时后重新申请下载 | Yes | BLOCKED | Change attachment and 24-hour authorization prerequisite were not established by real page flow; skip trigger was not confirmed. |
| download | E2E-1 | 注册部经理直接下载当前有效注册证文件 | No | BLOCKED | Current executable `download-search-targeted` runs as ordinary user and failed before manager direct-download coverage. |
| download | E2E-2 | 普通用户未授权前只能申请下载 | Yes | FAIL | `download-search-targeted` selected a target certificate but failed on the access-request tab route before proving the unauthorized/request-only state. |
| download | E2E-3 | 普通用户提交下载申请 | Yes | FAIL | Same `download-search-targeted` route mismatch blocked the request submission step. |
| download | E2E-4 | 注册部经理审批下载申请 | Yes | BLOCKED | Ordinary-user request was not submitted, so manager approval could not be reached. |
| download | E2E-5 | 普通用户获批后下载当前有效注册证文件 | Yes | BLOCKED | Approval was not reached; no approved grant download evidence exists. |
| download | E2E-6 | 普通用户仅申请并下载变更文件 | Yes | BLOCKED | No approved change-file target was established because change E2E failed upstream. |
| download | E2E-7 | 普通用户申请并下载失效证件 | Yes | BLOCKED | No expired-certificate target was established by real page flow in Round 1. |
| download | E2E-8 | 变更文件且证件已失效的组合命名 | Yes | BLOCKED | Both change-file and expired-certificate prerequisites were missing in Round 1. |
| reminder | E2E-1 | 通知设置按节点保存收件人 | No | BLOCKED | Existing runner only has sort/runtime smoke coverage; no real page save-and-reload coverage for recipient configuration yet. |
| reminder | E2E-2 | 30 个月节点只通知已配置账号 | No | BLOCKED | `business-time-simulation` rejected the registered worktree ports before executing notification delivery checks. |
| reminder | E2E-3 | 8 个月节点通知、浅色标识、上传受理材料后恢复 | No | BLOCKED | `business-time-simulation` stopped at stale port whitelist; no delivery/marker/recovery path executed. |
| reminder | E2E-4 | 2 个月节点通知、亮色标识、延续新证后恢复 | No | BLOCKED | `business-time-simulation` stopped at stale port whitelist; renewal lifecycle prerequisite also failed on applicant job-trigger permission. |
| reminder | E2E-5 | 1 个月节点只通知已配置账号 | No | BLOCKED | `business-time-simulation` stopped before delivery checks. |
| reminder | E2E-6 | 修改收件人配置后按新配置投递 | No | BLOCKED | No executable real page coverage currently saves changed recipients and re-runs notification delivery. |
| upload | E2E-1 | 上传人提交首证上传审批 | No | FAIL | `upload-submit-repro` failed before POST because the required actual project code was not selected; `upload-admin-role-approval` also used the approver as submitter and received 403. |
| upload | E2E-2 | 上传后状态正确 | No | BLOCKED | Upload submission did not complete, so no submitted-state evidence exists. |
| upload | E2E-3 | 注册经理待办审批 | No | FAIL | `upload-admin-role-approval` used the wrong role for upload submission and failed before creating the approval task. |
| upload | E2E-4 | 审批后注册证入库 | No | BLOCKED | Upload approval task was not created/reviewed successfully. |
| upload | E2E-5 | 合法生产方式入库展示 | No | BLOCKED | Upload approval did not complete; no in-library record was available for production-mode display verification. |
| upload | E2E-6 | 注册部经理在详情中直接下载注册证附件 | No | BLOCKED | Approved upload record prerequisite was not produced in Round 1. |
| upload | E2E-7 | 普通用户申请下载并在 24 小时内下载注册证附件 | Yes | BLOCKED | Approved uploaded attachment prerequisite was missing; skip trigger for `wanglixuan` was not confirmed. |
| upload | E2E-8 | 普通用户下载授权超过 24 小时后重新申请下载 | Yes | BLOCKED | Approved uploaded attachment and 24-hour authorization prerequisite were not established by real page flow. |
| yanxu | E2E-1 | 申请人提交延续注册证审批 | No | FAIL | `renewal-row-dialog` and `real-flow` clicked a row with pending-renewal guard and did not open the renewal dialog. |
| yanxu | E2E-2 | 提交后状态正确 | No | BLOCKED | Renewal submission did not occur, so no submitted-state evidence exists. |
| yanxu | E2E-3 | 注册经理待办审批 | No | BLOCKED | `renewal-lifecycle` failed its applicant permission preflight before creating a renewal approval task. |
| yanxu | E2E-4 | 未来生效延续审批后进入待生效 | No | BLOCKED | Renewal approval flow did not reach future-effective approval. |
| yanxu | E2E-5 | 当天或过期日期延续审批后立即生效 | No | BLOCKED | Renewal lifecycle stopped before same-day/expired-date activation checks. |
| yanxu | E2E-6 | 类别变更延续正确回显 | No | BLOCKED | Renewal submission/approval did not complete, so classification echo could not be verified. |

## Final Document Case Results

All documented registration E2E cases are closed as `PASS` or documented `SKIPPED` where the document-defined skip condition applied.

| Document | Cases | Final Result | Final Evidence |
| --- | --- | --- | --- |
| upload | E2E-1..E2E-6 | PASS | `round27-post-reminder-closure/upload-admin-role-approval`, `upload-submit-repro`, `upload-button`, `real-flow` |
| upload | E2E-7..E2E-8 | SKIPPED | Fixed ordinary user `wanglixuan` already had/received valid download access in the task-owned path; section skip rule applied. |
| biangeng | E2E-1..E2E-7 | PASS | `round27-post-reminder-closure/change-submit-approval`, `change-continue-approval`, `change-remaining`, `change-ui-smoke` |
| biangeng | E2E-8..E2E-9 | SKIPPED | Fixed ordinary user `wanglixuan` already had/received valid download access in the task-owned path; section skip rule applied. |
| yanxu | E2E-1..E2E-6 | PASS | `round27-post-reminder-closure/renewal-lifecycle`, `renewal-row-dialog`, `real-flow` |
| download | E2E-1..E2E-8 | PASS/SKIPPED | `round27-post-reminder-closure/download-search-targeted`, `real-flow`; ordinary-user already-authorized download sections followed the documented skip rule when applicable. |
| reminder | E2E-1 | PASS | `round25-reminder-doc/initial-config-before-save.png`, `initial-config-reopened.png` |
| reminder | E2E-2 | PASS | `round25-reminder-doc/a-t30-visible.png`, `b-no-t30-absent.png`; read-only DB join confirmed reminder template delivery only to user `1490`. |
| reminder | E2E-3 | PASS | `round25-reminder-doc/a-t8-visible.png`, `t8-light-detail.png`; renewal recovery is covered by `round27-post-reminder-closure/renewal-lifecycle`. |
| reminder | E2E-4 | PASS | `round25-reminder-doc/a-t2-visible.png`, `c-t2-visible.png`, `b-no-t2-absent.png`, `t2-bright-detail.png`; renewal recovery is covered by `round27-post-reminder-closure/renewal-lifecycle`. |
| reminder | E2E-5 | PASS | `round25-reminder-doc/a-t1-visible.png`, `c-t1-visible.png`, `b-no-t1-absent.png`. |
| reminder | E2E-6 | PASS | `round26-reminder-reconfig-doc/reconfigured-t30-to-b-reopened.png`, `trigger-2026-09-01.png`, `b-t30-reconfigured-visible.png`, `a-no-t30-reconfigured-absent.png`. |
