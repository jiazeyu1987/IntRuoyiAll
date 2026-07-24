# Execution Log: DCC Four Real Approvers Full E2E

BDD: four stage approvals are performed by four real accounts ->
Given the DCC full chain is already green in the local runtime
When the browser upgrades the approval flow to use stage-specific real user
accounts
Then the signature trail and approval transitions must prove the intended real
users acted on their respective stages.

BDD: runtime setup gaps fail fast ->
Given the local runtime may still lack stage-specific user accounts or correct
position assignments
When the upgraded E2E runs
Then it must stop with the exact missing prerequisite instead of treating the
single-account path as sufficient.

- M1: Completed. Previous frontend task `20260516-dcc-full-chain-real-e2e` is
  completed, so this follow-up actor-correctness task can proceed.
- RED: pre-task coverage gap -> FAIL, the repository had no single real browser
  E2E that proved the four approval actions were performed by four distinct
  real accounts.
- RED: current single-account baseline -> FAIL for actor correctness, because
  the existing full-chain E2E completed approvals with one shared local
  approver account.
- RED: initial runtime investigation -> FAIL, `文控审核` and `文控批准` are single-user
  BPM tasks that randomly choose one assignee from the shared document-control
  candidate set, so a naive fixed-user script cannot guarantee four distinct
  real actors.
- M2: Completed. BDD scope plus RED evidence were recorded before the final
  script refinement.
- M3: Completed. Added
  `doc/tasks/20260516-dcc-four-real-approvers-e2e/scripts/verify-dcc-four-real-approvers-e2e.mjs`
  to prepare live users, inspect actual BPM assignees, and verify distinct
  actors in the final signature trail.
- M4: Completed. Reused the live runtime and prepared the required users,
  roles, category matrix, shared doc-control position assignments, and category
  permission rules.
- GREEN:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-four-real-approvers-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-four-real-approvers-e2e\scripts\verify-dcc-four-real-approvers-e2e.mjs`
  -> PASS, created controlled file `30`, completed four live approvals through
  users `117 -> 103 -> 104 -> 100`, reached `ACTIVE`, rendered the red
  controlled stamp, and verified published PDF persistence.
