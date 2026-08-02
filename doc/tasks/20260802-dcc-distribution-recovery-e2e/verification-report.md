# Verification Report

## Result

E2E BLOCKED.

## Scope

- Target scenario: DCC 文控“文件分发/旧版回收”真实 Playwright E2E。
- Required path: non-admin DCC user, real page distribution/receipt/recovery actions, V1->V2 version transition, V2 current effective use, V1 non-misuse, final read-only API/DB reconciliation.
- Safety boundary: no admin account, no API-only substitute, no direct SQL/API insert/update for distribution, receipt, recovery, version state, permissions, or approval state.

## Rule Reads

- Read `AGENTS.md`, `docs/e2e-rules.md`, `docs/login-access.md`, `docs/frontend-development.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, `docs/branch-runtime-ports.md`, `docs/database-rules.md`, `docs/powershell-encoding.md`, `docs/task-closeout-rules.md`, and Playwright skill.

## Runtime

- Frontend: `http://127.0.0.1:8081/` remained owned by the `E:\IntRuoyi\IntRuoyiFronted` Vite process.
- Backend: after the user reported startup, `http://127.0.0.1:48081/actuator/health` returned `UP`; port `48081` is owned by Java running `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`.
- Earlier blocked restart evidence remains recorded: copied runtime jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-205036.jar` failed on `MesProProcessPoolTimelineReadMapper.xml` MyBatis XML parse error.
- Browser: Playwright used local Chrome at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Password handling: password was injected only through `DCC_E2E_PASSWORD`; no plaintext password is written in this report.

## Current File

- Tenant: `芋道源码` / tenant ID `1`; non-admin users only.
- Category: `906104` / `其他`; this category has an active distribution rule for `质量体系部` (`department_id=253`) with medium `PUBLIC_FOLDER`, plus category `DISTRIBUTE` permission for role `dcc_distribute_e2e`.
- File number: `CODX-DCC-DIST-906104-DISTTENANT120260802195305`.
- V1: controlled file ID `2054545668044070297`, version `V1.0`, status `ACTIVE`, published at `2026-08-02 20:19:57`.
- V2: controlled file ID `2054545668044070302`, version `V2.0`, status `READY_TO_PUBLISH`, approved at `2026-08-02 20:22:48`.
- Evidence: `paper-chain-tenant1-result.json` and `tenant1-current-blocked-readonly-db-verification.json`.

## Candidate Category Scan

- Read-only DB scan after the user requested a category with distribution permission rules found no existing category that satisfies all required prerequisites at once: published DCC `PUBLISH / READY_TO_PUBLISH` policy, active distribution rule, non-admin `APPROVE`, and non-admin `DISTRIBUTE`.
- Tenant `芋道源码` category `906104 / 其他` has the published publish policy and distribution rule, but `APPROVE` is only granted to user ID `1`; non-admin `wangsiyu` has `UPLOAD` and `DISTRIBUTE`, not `APPROVE`.
- Tenant `122` category `900347 / Codex Local DCC Category` has non-admin `aoteman` with `APPROVE / DISTRIBUTE / UPLOAD` and a distribution rule, but no published DCC `PUBLISH / READY_TO_PUBLISH` business approval policy.
- Evidence: `candidate-permission-scan-20260802-210906.json`.

## Page Evidence

- Real upload/approval/training/release path completed for V1:
- `wangsiyu` uploaded V1 through the real upload page; four approval steps were completed by `zhaohaichen`, `zhaojie`, `zhaomingyu`, and `wangsiyu`.
- Training was assigned to and completed by `zhaomingyu` through the real “我的培训” page after the 10-minute controlled viewing threshold.
- `wangsiyu` completed the real “正式下发” page action, making V1 `ACTIVE`.
- Real upload/approval path completed for V2:
- `wangsiyu` uploaded V2 through the real upload page; four approval steps were completed by the same non-admin approvers.
- V2 reached `READY_TO_PUBLISH`, but the page did not render the “发布申请” button; this was rerun after backend health returned `UP` and still blocked at the same real page control.
- V1 paper issue path after backend restore:
- `wangsiyu` opened V1 traceability detail through the real page and clicked “确认纸质发放”.
- The page selected recipient `panhaitao` through the real user selector and submitted “确认发放”.
- The V1 distribution section then displayed `潘海涛 (panhaitao)`, status `已确认`, issue owner `王思雨 (wangsiyu)`, issue time `2026-08-02 21:25:23`, and the next action `确认回收`.

## Distribution And Recovery

- V1 distribution record ID: `4341`; medium `PAPER`; status `ACKNOWLEDGED`; recipient `panhaitao`; acknowledgedBy `910250 / wangsiyu`; acknowledgedAt `2026-08-02 21:25:23`; recoveredBy `null`; recoveredAt `null`.
- V2 distribution record ID: `4344`; medium `PAPER`; status `PENDING`; recipients `[]`; acknowledgedBy `null`; recoveredBy `null`; recoveredAt `null`.
- Recipient responsibility: recorded through the real page for V1 as `panhaitao` (`user_id=173`, recipient row `46820`).
- Recovery responsibility: not created; V2 could not be published to `ACTIVE`, so V1 never became `SUPERSEDED` and old-version recovery could not be triggered.
- Recovery record ID: BLOCKED / not generated.

## Blockers

- Permission/test-data blocker: category `906104` has `DISTRIBUTE` for `wangsiyu` via role `dcc_distribute_e2e`, but `APPROVE` is only assigned to user ID `1`. Since `DccControlledFileQueryServiceImpl` exposes `canPublish` only when the file is `READY_TO_PUBLISH` and the current user has category `APPROVE`, non-admin `wangsiyu` cannot submit V2 publish from the real page.
- Existing-data blocker: a full read-only scan found zero existing categories where published DCC publish policy, active distribution rule, non-admin `APPROVE`, and non-admin `DISTRIBUTE` all coexist without changing data.
- Runtime blocker cleared for this continuation: backend `48081` is `UP`, and V1 distribution was completed.
- Remaining blocker: V2 publish still cannot be started by non-admin `wangsiyu` because the real page does not render “发布申请”; read-only DB confirms category `906104` `APPROVE` is still only assigned to user ID `1`.
- Impact: the scenario has real-page “分发” coverage, but cannot complete old-version “回收” or old-version non-misuse for this category chain because V2 is not `ACTIVE` and V1 is not `SUPERSEDED`.

## Read-Only Reconciliation

- Evidence files: `tenant1-current-blocked-readonly-db-verification.json` and `tenant1-post-v1-ack-readonly-db-verification.json`.
- Confirmed V1 `ACTIVE`, V2 `READY_TO_PUBLISH`; V1 paper distribution `4341` is now `ACKNOWLEDGED`, V2 paper distribution `4344` remains `PENDING`.
- Confirmed recipient row `46820` records `panhaitao`; confirmed `acknowledgedBy=wangsiyu`; confirmed no recoveredBy values exist.
- Confirmed category `906104` has active `DISTRIBUTE` distribution responsibility but lacks non-admin `APPROVE` for the selected publish applicant.
- Publish retry evidence after backend restore: `publish-blocked-after-backend-up.json`.

## Required To Unblock

- Assign or select approved non-admin users through the product's formal configuration path so that one existing category has both category `APPROVE` and `DISTRIBUTE`, an active distribution rule, and a published DCC `PUBLISH / READY_TO_PUBLISH` policy.
- Keep backend `48081` on a cleanly starting runtime; this continuation used the restored `backend-runtime-control-20260802-170535.jar`.
- Do not unblock by admin login, API-only issue/recovery, direct SQL status changes, direct permission inserts, or direct insertion of distribution/recipient/recovery records.
