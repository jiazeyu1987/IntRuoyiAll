# Execution Log: DCC 多账号审批真实 E2E

BDD: each DCC approval stage is completed by a different real account -> Given the live runtime contains separate real users with valid login credentials and category access / When the submitter uploads a controlled file and each approval stage is processed through the real frontend by its designated account / Then the final signature trail must show distinct actor ids across the four approval stages.

BDD: submitter and approvers are separated -> Given a real submitter account and four real approval accounts / When the DCC file is uploaded and approved / Then the submitter account must not appear in the approval signature trail.

BDD: missing multi-account prerequisites fail fast -> Given the runtime may lack usable accounts, passwords, category permissions, or deterministic stage assignments / When the multi-account E2E runs / Then it must stop with the exact blocker instead of silently falling back to one account.

- M1: Completed. Previous frontend task `20260516-dcc-full-chain-real-e2e` was already completed before this stricter multi-account task started.
- RED: pre-task coverage gap -> FAIL, the existing full-chain real E2E proved the workflow with one runtime account but did not prove separated approval actors per stage.
- M2: Completed. Recorded BDD scenarios and RED coverage intent before code changes.
- M3: Completed. API-level viability checks proved that a live route with four distinct resolved users can complete to `ACTIVE` once the runtime route and position assignments are deterministic.
- M4: Completed. Added `doc/tasks/20260516-dcc-multi-account-approval-real-e2e/scripts/verify-dcc-multi-account-approval-real-e2e.mjs`.
- RED: initial multi-account exploration -> FAIL, the default fixed route still allowed the same `文控` position to resolve multiple users, so stage 1 and stage 4 could not be guaranteed to land on different accounts.
- RED: candidate DCC e2e users `dcce2edoc1/dcce2esign1/dcce2eappr1/dcce2edoc2` initially had unknown passwords, so they could not be used directly until the live admin API reset them.
- RED: using a shared `文控` position for both stage 1 and stage 4 still allowed runtime drift in assignee resolution, so a dedicated four-stage route had to be saved for category `产品技术要求`.
- GREEN: live route repair -> PASS after category `产品技术要求` was re-saved to a dedicated route resolving to users `117 -> 103 -> 104 -> 100`.
- GREEN: live position assignment repair -> PASS after positions `31 / 1 / 900333 / 900334` were replaced through the live admin API with exactly one assigned real user per stage.
- GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-multi-account-approval-real-e2e-green run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-multi-account-approval-real-e2e\scripts\verify-dcc-multi-account-approval-real-e2e.mjs` -> PASS, controlled file `29` progressed through all four stages with distinct approval actors `117, 103, 104, 100`, reached final API status `ACTIVE`, and preserved readable published-file bytes.
