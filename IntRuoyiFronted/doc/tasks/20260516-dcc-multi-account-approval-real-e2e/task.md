# Task: DCC 多账号审批真实 E2E

## Goal

把现有单账号可跑通的 DCC `上传 -> 审批 -> 落盘` 真实 E2E 升级成多账号审批版，使真实浏览器链路满足：

- `提交人` 与审批人分离
- `文控审核`、`审核会签`、`批准`、`文控批准` 由四个不同真实账号完成
- 最终仍然验证文件落盘为 `ACTIVE`，并且 `publishedFileId` 对应的 PDF 可读

## Scope

- 检查前端仓上一条任务状态；如最新未完成任务与本次无关，则先显式标记阻塞或暂停。
- 在生产代码变更前创建当前任务目录、`task.md`、`execution-log.md`、`qa-test-suite-evidence.md` 和脚本目录。
- 复用现有 full-chain 实跑经验，新增多账号审批版 Playwright 脚本。
- 允许通过真实管理接口修复本地运行时前置数据：
  - 真实用户密码重置到可登录值
  - 真实审批路线
  - 真实岗位分配
  - 必需的类别可见权限
- 不使用 mock 数据，不增加 fallback，不用 API 代替真实审批前端路径；API 仅用于前置修复和最终校验。

## Previous Task Check

- Previous frontend task:
  `doc/tasks/20260516-dcc-full-chain-real-e2e/task.md`
- Status before this task: completed.
- Impact: the single-account full-chain baseline is already green, so this task focuses on stricter multi-account approval separation.

## Milestones

- [x] M1: Create the task package after confirming previous task status.
- [x] M2: Record BDD scenarios and RED evidence for missing multi-account approval coverage.
- [x] M3: Prove the live runtime can support separated approval accounts.
- [x] M4: Upgrade the real Playwright script to multi-account approval.
- [x] M5: Run GREEN verification and update evidence.
- [ ] M6: Commit only task-scoped frontend files if verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-multi-account-approval-real-e2e-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\scripts\verify-dcc-multi-account-approval-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\qa-test-suite-evidence.md`

## Current Status

Completed. The real browser E2E now proves one submitter plus four distinct approval accounts across the four DCC stages, and still verifies final persistence.

## Runtime Repairs Performed

- Paused the unrelated unfinished frontend task `20260516-dcc-training-closed-loop` due user reprioritization.
- Reset the real approval accounts to known passwords through the live admin API:
  - `admin123` (`userId=117`)
  - `yuanma` (`userId=103`)
  - `test` (`userId=104`)
  - `yudao` (`userId=100`)
- Re-saved category `产品技术要求` to a dedicated four-stage approval route:
  - stage 1 `DOC_CONTROL_REVIEW` -> position `31` -> user `117`
  - stage 2 `MATRIX_REVIEW` -> position `1` -> user `103`
  - stage 3 `MATRIX_APPROVAL` -> position `900333` -> user `104`
  - stage 4 `DOC_CONTROL_APPROVAL` -> position `900334` -> user `100`
- Replaced the corresponding DCC position assignments through the live admin API so each stage resolves to exactly one real user.
- Verified the route preview returned the expected resolved user ids `117 -> 103 -> 104 -> 100` before running the browser chain.

## Blocker And Impact

- Blocker: scoped Git commit is still pending because this frontend repository contains many unrelated in-progress changes outside this task package.
- Impact: the multi-account E2E is green and documented, but task-only closeout still needs careful staging.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-multi-account-approval-real-e2e-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\scripts\verify-dcc-multi-account-approval-real-e2e.mjs` -> PASS
- Real result:
  - controlled file id: `29`
  - submitter account: `admin (userId=1)`
  - stage 1 approver: `admin123 (userId=117)`
  - stage 2 approver: `yuanma (userId=103)`
  - stage 3 approver: `test (userId=104)`
  - stage 4 approver: `yudao (userId=100)`
  - final detail status: `现行`
  - final API status: `ACTIVE`
  - published file id: `2261`
  - published file config id: `4`
  - screenshot: `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-multi-account-approval-real-e2e-20260516.png`
