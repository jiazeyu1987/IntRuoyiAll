# Execution Log: DCC Training Closed Loop

BDD: training page shows read-only recipient mapping and execution tracking ->
Given the DCC training menu is opened by an admin / When the page loads /
 Then the rules tab explains that training recipients inherit distribution
 recipients and the execution tab tracks file-user completion state.

BDD: training users must view the file for 600 focused seconds before
 acknowledgement -> Given a training user opens a dedicated training preview
 task / When the page remains focused and the preview session heartbeats
 accumulate / Then the UI shows accumulated seconds, blocks acknowledgement
 before 600 seconds, and enables it at or after 600 seconds.

BDD: admins can see which file and which user finished training -> Given one
 or more training users complete acknowledgement / When the admin opens the
 training execution tab or the controlled-file detail page / Then the UI shows
 the file, user, accumulated seconds, acknowledgement timestamp, and completion
 status truthfully.

RED: focused frontend/runtime validation -> FAIL initially, because:
- `DCC 培训` still only edited training departments and had no execution tab
- no `我的培训` route existed
- no dedicated training preview page existed
- the frontend had no heartbeat/session client for the 600-second threshold

GREEN: `pnpm exec eslint src/api/dcc/controlledFile/training.ts src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/training/index.vue src/views/dcc/controlled-file/training/presentation.ts src/views/dcc/controlled-file/training/components/TrainingRulesReadonlyTab.vue src/views/dcc/controlled-file/training/components/TrainingExecutionTab.vue src/views/dcc/controlled-file/training/mine/index.vue src/views/dcc/controlled-file/training/task/index.vue src/views/dcc/controlled-file/detail/index.vue src/views/dcc/controlled-file/detail/presentation.ts src/router/modules/remaining.ts`
-> PASS

GREEN: `set NODE_OPTIONS=--max-old-space-size=8192 && pnpm exec vue-tsc --noEmit`
-> PASS

GREEN: live runtime observation through the new training UI/API chain -> PASS
for the core business facts:
- dedicated training-task preview created `view-session/start`
- heartbeat accumulation reached `600+` seconds on real file `44`
- acknowledge wrote `training_progress.acknowledged_at`
- `training-executions/page` and `training-tasks/my-page` expose the latest
  generated file-user rows

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-training-closed-loop run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-closed-loop\scripts\verify-dcc-training-closed-loop-real-e2e.mjs`
-> PASS, created controlled file `46`, completed the real approval chain,
accumulated `604+` focused-view seconds on progress `61`, wrote
`acknowledged_at`, rejected a non-recipient opening the training task, and
confirmed the acknowledged row in `training-executions/page`.

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-training-closed-loop run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-training-closed-loop\scripts\verify-dcc-training-closed-loop-real-e2e.mjs`
-> PASS again after the API-login auth-context check and stage-assignee
verification were added to the script. The rerun created controlled file `51`,
completed the real approval chain, accumulated `606+` focused-view seconds on
progress `67`, wrote `acknowledged_at`, rejected a non-recipient opening the
training task, and confirmed the acknowledged row in
`training-executions/page`.
