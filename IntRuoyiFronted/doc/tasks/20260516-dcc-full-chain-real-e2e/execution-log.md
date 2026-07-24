# Execution Log: DCC Full Chain Real E2E

BDD: full chain reaches stamped published state -> Given a real DCC PDF, a real
category, a live approval route, and a deployed BPM definition / When the user
uploads the file, submits it, completes all four approval stages, opens the
protected preview, and waits for finalization / Then the preview shows the red
controlled stamp and the file reaches final persisted state with published file
metadata and readable PDF bytes.

BDD: missing runtime prerequisites fail fast -> Given the local runtime may miss
route data, approver assignments, directory bindings, preview assets, or BPM
definitions / When the real full-chain E2E runs / Then it must stop with the
exact blocker instead of pretending the chain passed.

- M1: Completed. Previous frontend task
  `20260516-dcc-upload-approval-persistence-real-e2e` is completed, so this
  new full-chain task can proceed independently.
- RED: pre-task coverage gap -> FAIL, the repository had no single real browser
  E2E that chained upload, four-stage approval, stamp verification, and final
  published-file persistence in one run.
- RED: existing real approval path -> FAIL, the live backend approval action
  threw `NoClassDefFoundError: DccControlledFileSignatureModeEnum` while the
  server process was still running from a stale `target/yudao-server.jar`
  instance.
- RED: approval task entry path under automation -> FAIL, the initial row-click
  helper did not reach the detail page because the action button had to be
  selected from the row's trailing action buttons rather than generic button
  filtering.
- M2: Completed. BDD scope plus RED evidence were recorded before final script
  refinement.
- M3: Completed. Added
  `doc/tasks/20260516-dcc-full-chain-real-e2e/scripts/verify-dcc-full-chain-real-e2e.mjs`
  by extending the upload-controls script with approval, stamp, and persistence
  checks.
- M4: Completed. Repaired the live runtime by restarting from a fresh runtime
  copy and reusing the already-prepared local DCC prerequisites.
- GREEN:
  `npx.cmd --yes --package @playwright/cli playwright-cli --session dcc-full-chain-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260516-dcc-full-chain-real-e2e\scripts\verify-dcc-full-chain-real-e2e.mjs`
  -> PASS, created controlled file `12`, completed all four approvals,
  reached `ACTIVE`, rendered the red controlled stamp, and verified published
  PDF metadata plus readable bytes.
