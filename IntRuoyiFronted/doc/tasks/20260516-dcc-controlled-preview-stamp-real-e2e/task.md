# Task: DCC 受控预览受控章真实 E2E

## Goal

为 DCC 受控文件“再次预览显示受控章”补充一个真实 Playwright E2E 用例，
通过真实登录、真实上传、真实审批、真实预览页和真实 canvas 渲染结果，验证
预览页确实出现红色 `受控` 印章。

## Scope

- 检查同仓库前序任务状态，并显式记录不冲突或阻塞情况。
- 在生产代码变更前创建当前任务文档、执行日志和脚本目录。
- 新增 1 个真实浏览器 Playwright E2E 脚本，覆盖：
  - DCC 上传页真实提交
  - DCC 审批任务真实四阶段通过
  - DCC 详情页再次预览
  - 受控预览 canvas 中受控章像素校验
- 更新 QA 证据，记录 requirement-to-test matrix、RED/GREEN 结果和阻塞。
- 如果真实 E2E 暴露出阻断验证的最小前端缺陷，允许在同一任务内做最小修复并复跑。
- 不修改当前受控章业务实现，不引入 mock 数据，不走接口捷径替代前端路径。

## Previous Task Check

- Latest unfinished frontend task:
  `doc/tasks/20260516-dcc-routes-switch-auto-query/task.md`
- Status before this task: blocked by user reprioritization.
- Direct prerequisite task:
  `doc/tasks/20260516-dcc-controlled-preview-stamp/task.md`
- Prerequisite status before this task: completed.
- Impact: the blocked routes auto-query task remains paused and does not
  conflict with this E2E addition; the preview-stamp implementation baseline is
  already complete, so this task can focus on QA coverage only.

## Milestones

- [x] M1: Confirm previous task state and create this task package.
- [x] M2: Record BDD scenarios and add the RED E2E script for controlled
  preview stamp coverage.
- [x] M3: Run the real browser path, refine the script for runtime stability,
  and reach GREEN if prerequisites exist.
- [x] M4: Update QA evidence and task logs with exact verification or blocker
  details.
- [ ] M5: Commit only this task's QA/E2E files if verification fully passes.

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-controlled-preview-stamp-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp-real-e2e\scripts\verify-dcc-controlled-preview-stamp-real-e2e.mjs`
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md`

## Current Status

Functionally completed. The real Playwright path now creates a controlled file, approves all
four live stages, reaches the protected preview page, and verifies red
`受控` stamp pixels on the real preview canvas. The task also captured and
resolved the blocking runtime gaps uncovered along the way:
- missing DCC distribution/training department rules for `产品技术要求`
- backend status race that left the file stuck in `FINALIZING`
- missing PDF.js worker asset in the protected preview page
- preview stamp color drift away from the requested red seal look

## Blocker And Impact

- Blocker: scoped frontend commit is still pending because
  `src/views/dcc/controlled-file/view/index.vue` and
  `src/views/dcc/controlled-file/view/presentation.ts` already contain
  unrelated uncommitted preview-watermark work in the same file set.
- Impact: verification is complete and the runtime/browser path is green, but
  this repository still needs a later clean write set before we can create a
  task-only frontend commit without sweeping in another in-progress task.

## Final Verification Result

- `npx.cmd --yes --package @playwright/cli playwright-cli run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-controlled-preview-stamp-real-e2e\scripts\verify-dcc-controlled-preview-stamp-real-e2e.mjs` -> PASS
- `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\docs\qa\test-suite-evidence.md` -> PASS
- Real result:
  - controlled file id: `2054545668044042268`
  - final detail status: `现行`
  - preview URL: `http://127.0.0.1:8081/dcc/controlled-file/detail/2054545668044042268?viewer=1&from=detail`
  - canvas red stamp pixels: `1962`
  - screenshot: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\output\playwright\dcc-controlled-preview-stamp-real-e2e-20260516.png`

## Cleanup Keep

- `doc/tasks/20260516-dcc-controlled-preview-stamp-real-e2e/scripts/verify-dcc-controlled-preview-stamp-real-e2e.mjs`
- `docs/qa/test-suite-evidence.md`
