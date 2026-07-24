# Task: DCC 同名新版本替代真实 E2E

## Goal

新增 1 条真实 Playwright E2E，用真实 DCC PDF、真实前端页面、真实审批矩阵、
真实审批账号和真实后端接口，证明同一文件类别、同一文件名称、同一文件编号的
受控文件链路满足以下行为：

- 首次上传并审批 `1.0` 后成为 `ACTIVE`
- 再次提交相同 `1.0` 时被明确拒绝，页面暴露版本错误
- 再次提交同名同编号的 `2.0` 并完成审批后成为新的 `ACTIVE`
- 旧版 `1.0` 自动转为 `SUPERSEDED`，并写入 `supersededByFileId`
- 新版本详情或版本历史能看到 `1.0` 与 `2.0` 两条真实记录

## Scope

- 先显式阻塞并暂停当前仓库里最新未完成的前端任务，避免跨任务混改。
- 在本任务包中记录 BDD、RED/GREEN 证据、最终验证结果和收尾预览结果。
- 复用现有已通过的 DCC 上传、审批、详情和持久化真实链路脚本能力。
- 不新增公共接口，不改现有前端 API 协议，优先复用详情/查询返回中的
  `status`、`supersededByFileId`、`versionHistory` 字段做断言。
- 默认只新增真实 E2E 脚本和任务证据；仅当 RED 暴露真实产品缺陷时，才进入
  最小修复并补对应测试。
- 不使用 mock、fallback 或 API 捷径代替真实前端用户路径；API 仅用于最终校验。

## Previous Task Check

- Previous frontend tasks paused before this task:
  - `doc/tasks/20260516-dcc-four-real-approvers-e2e/task.md`
  - `doc/tasks/20260516-dcc-multi-account-approval-real-e2e/task.md`
  - `doc/tasks/20260516-dcc-upload-name-version-linkage/task.md`
- Status before this task: blocked by explicit user reprioritization.
- Impact: those tasks are intentionally paused and do not block this version
  supersede E2E delivery.

## Milestones

- [x] M1: Block the previous unfinished frontend tasks and create this task
  package before task-specific code edits.
- [x] M2: Record BDD scenarios and RED evidence for the missing version
  supersede real E2E coverage.
- [x] M3: Implement the real Playwright script covering initial publish,
  duplicate-version rejection, and higher-version supersession.
- [x] M4: Run live GREEN verification and update exact evidence or blockers.
- [x] M5: Preview task closeout cleanup and commit only task-scoped files if
  verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-version-supersede-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\scripts\verify-dcc-version-supersede-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\qa-test-suite-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260516-dcc-version-supersede-real-e2e --mode preview`

## Current Status

Completed for the requested QA scope. The real Playwright E2E now proves first
publish, duplicate-version rejection, and higher-version supersession on the
same logical controlled-file chain.

## Runtime Repairs Performed

- Reused the existing real DCC four-approver runtime setup by resetting and
  enabling the real local users `100 / 103 / 104 / 117`, assigning admin roles,
  and replacing the live DCC position assignments through real admin APIs.
- Re-saved the live `产品技术要求` category matrix so route preview and live BPM
  routing consistently resolve to the deterministic real approver set needed by
  this E2E.
- Adjusted the script to the current upload-page input shape, where `文件名称`
  is now a history-enabled combobox rather than the earlier plain text field.
- Repaired the live MySQL runtime schema at `127.0.0.1:23306/ruoyi-vue-pro` by
  adding the missing `distribution_medium` column to
  `dcc_file_category_distribution_rule` and `dcc_controlled_file_distribution`,
  which otherwise caused `DCC审批任务` to 500 during real verification.

## Blocker And Impact

- Blocker: none for the requested QA scope after verification and closeout
  preview completed.
- Impact: none beyond keeping the Git staging set limited to task-owned files.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-version-supersede-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-version-supersede-real-e2e\scripts\verify-dcc-version-supersede-real-e2e.mjs` -> PASS
- Real result:
  - logical file name: `DCC-VERSION-SUPERSEDE-1778946949161-文件`
  - first published file id: `25`
  - duplicate submit rejected with message:
    `Controlled file version must be greater than the current chain version`
  - second published file id: `26`
  - first revision final status: `SUPERSEDED`
  - second revision final status: `ACTIVE`
  - `1.0.supersededByFileId = 26`
  - screenshot:
    `D:/ProjectPackage/Int/IntRuoyi/yudao-ui-admin-vue3/output/playwright/dcc-version-supersede-real-e2e-20260516.png`

## Cleanup Keep

- doc/tasks/20260516-dcc-version-supersede-real-e2e/task.md
- doc/tasks/20260516-dcc-version-supersede-real-e2e/execution-log.md
- doc/tasks/20260516-dcc-version-supersede-real-e2e/qa-test-suite-evidence.md
- doc/tasks/20260516-dcc-version-supersede-real-e2e/scripts/verify-dcc-version-supersede-real-e2e.mjs
