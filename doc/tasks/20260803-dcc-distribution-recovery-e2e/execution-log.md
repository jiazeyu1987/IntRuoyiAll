# Execution Log

## User Intent

- 用户要求在 `E:\IntRuoyi` 对 DCC 文控“文件分发/旧版回收”进行一次完整真实 Playwright E2E 验证。
- 必须使用非 admin 账号，密码通过环境变量注入且不记录明文。
- 必须通过真实页面操作；API/DB 只用于最终只读核验。
- 不得直接 SQL/API 插入或更新分发/回收记录；缺入口、权限、测试数据或运行态时记录 E2E BLOCKED，不用 API-only 替代。

## Rule Reads

- Read `AGENTS.md`.
- Read `docs/e2e-rules.md`.
- Read `docs/login-access.md`.
- Read `docs/frontend-development.md`.
- Read `docs/local-runtime.md`.
- Read `docs/database-rules.md`.
- Read `docs/powershell-encoding.md`.
- Read `docs/task-closeout-rules.md`.
- Read Playwright skill `C:\Users\BJB110\.codex\skills\playwright\SKILL.md`.

## BDD

BDD: DCC controlled-copy distribution and old-version recovery -> Given a non-admin DCC distribution owner, a non-admin recipient, and a task-owned V1 controlled file that can become ACTIVE before being revised to ACTIVE V2, When the owner performs distribution registration and confirmation through the real page, V2 is published through the real DCC/BPM/training/release page chain, and the old V1 copy is recovered through the real page, Then the page and final read-only reconciliation show distribution, receipt/issue responsibility, recovery responsibility, timestamps, V1 no longer current effective, and V2 remains distributable.

## Command Intent Log

- Created task directory `doc/tasks/20260803-dcc-distribution-recovery-e2e`.
- Created baseline `task.md`, `execution-log.md`, and `verification-report.md`.
- Confirmed `npx` is available for Playwright wrapper prerequisites.
- Confirmed local Chrome is available at `C:\Program Files\Google\Chrome\Application\chrome.exe`.
- Confirmed frontend `http://127.0.0.1:8081/` returned HTTP 200.
- Confirmed backend `http://127.0.0.1:48081/actuator/health` returned `UP`.

## RED / GREEN

- RED: pending-full-e2e -> FAIL, full real Playwright scenario not yet executed in this task directory.
- RED: first full-chain run `DISTTENANT1202608030001` -> FAIL, real upload page project selector did not contain old default `PTCABC`.
- RED: second full-chain run `DISTTENANT1202608030002` -> FAIL, real upload page file classification did not contain old default `Codex分类20260719075725L1`.
- RED: third full-chain run `DISTTENANT1202608030003` -> FAIL, category `其他` requires child directory and `DCC_E2E_CHILD_DIRECTORY_NAME` was not yet supplied.
- RED: fourth full-chain run `DISTTENANT1202608030004` -> FAIL, real page created V1 `2054545668044070309` and completed three approval nodes, but selecting applicant `wangsiyu` as final upload approver produced a handling detail page without an approval action button. No SQL/API state change was used to repair it.
- RED: fifth full-chain run `DISTTENANT1202608030005` -> FAIL, real page created V1 `2054545668044070310` and completed `zhaohaichen` / `zhaojie` approval nodes, then target DCC detail APIs returned 500 during `zhaomingyu` approval and backend `48081` stopped accepting connections.

## Evidence

- Runtime preflight complete; passwords are not recorded in task artifacts.
- Project option probe through real page showed `PTCABC` was not available and current project API returned enabled projects including `按压式球囊扩充压力泵 / IDI`.
- Replaced stale file type path with existing path `技术文档 / 清单 / DHF文件清单`, based on prior real DCC file-type evidence in this workspace.
- `paper-chain-full-result.json` records the final BLOCKED run and page diagnostics.
- `screenshots/DISTTENANT1202608030004-approve-V1.0-wangsiyu-button-missing.png` records the final-approver no-button detail page for abandoned candidate `0004`.
- `screenshots/DISTTENANT1202608030005-login-zhaomingyu-blocked.png` records the login/tenant failure after backend `48081` became unavailable.
- Read-only DB verification after blocker: `CODX-DCC-DIST-906104-DISTTENANT1202608030005` has V1 ID `2054545668044070310`, status `PENDING_MATRIX_APPROVAL`, active task `批准`, assignee `424 / zhaomingyu`.

## Blockers

- E2E BLOCKED: backend `http://127.0.0.1:48081` refused connection at `2026-08-03 00:31:33 +08:00`; login tenant lookup showed “租户识别失败：请检查租户名称、本机后端服务和租户配置。”
- Impact: cannot continue real Playwright login, approval, V1 release, V2 revision/publish, distribution, receipt/issue confirmation, old-version recovery, or controlled-browser non-misuse verification.
- Safety boundary: no admin fallback, no API-only substitute, and no SQL/API insert/update for distribution, receipt, recovery, approval, or version status.

## Resume 2026-08-03

- Backend recovery check: `48081` is listening on Java process `48940`, command line points to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260802-170535.jar`, health endpoint returned `UP`, and tenant lookup returned `data=1`.
- Frontend recovery check: `http://127.0.0.1:8081/` returned HTTP 200.
- Read-only DB resume point: file `CODX-DCC-DIST-906104-DISTTENANT1202608030005`, V1 ID `2054545668044070310`, status `PENDING_MATRIX_APPROVAL`, active task `批准`, task ID `0cf42401-8e8f-11f1-a5cc-00155d2984a0`, assignee `zhaomingyu / 424`.
- Script maintenance: task-owned `dcc-paper-chain-prepare-e2e.cjs` now separates upload approvers from publish approvers and reads active upload approval assignees from the runtime DB as read-only evidence before performing each approval through the real page.
- GREEN: `node --check` -> PASS for `dcc-paper-chain-prepare-e2e.cjs` and `dcc-distribution-recovery-e2e.cjs` after the resume script update.

## Continuation Attempts After Backend Recovery

- RED: resume candidate `DISTTENANT1202608030005` -> FAIL, real page opened V1 detail for assignee `zhaomingyu` but no approval action button was available; page showed `受控打印动作投影缺失` and `无受控打印权限 当前账号缺少受控打印菜单权限，或该文件类别未授予 PRINT 打印权限`.
- Read-only config inventory -> BLOCKED, no active current-runtime category was found with the complete paper recovery prerequisite set `APPROVE + PRINT + DISTRIBUTE + PAPER distribution rule + bound directory`.
- RED: existing tenant `芋道源码` paper candidate `CODX-DCC-DIST-REC-DISTREC20260802173908` as `wangsiyu` -> FAIL, real page displayed V2 paper row and `确认纸质发放`, but submit returned business code `1080000049 / Current user cannot acknowledge this paper distribution`.
- RED: same existing candidate as `panhaitao` -> FAIL, real detail page opened but the distribution section showed `当前版本暂无分发记录`, so no issuer/recoverer action was available.
- RED: tenant `测试租户` candidate `CODX-DCC-DIST-900347-DIST90034720260802185602` as `aoteman` -> FAIL, real publish dialog showed `No published business approval policy matched action PUBLISH`; submit returned `code=500 / 系统异常`; read-only DB confirmed V2 stayed `READY_TO_PUBLISH` and no publish action instance was created.
- Final blocker impact: current run cannot cover both required actions (`分发` and `回收`) or prove old-version non-misuse through a newly completed V2 `ACTIVE` chain without adding/repairing formal category distribution/print/publish policy prerequisites.
- Safety boundary maintained: no admin account, no direct SQL/API inserts or updates for distribution, receipt, recovery, approval, publish, or version state.

## Non-Other Category Inventory 2026-08-03

- Read-only DB inventory confirms non-`其他` DCC files exist.
- Tenant `芋道源码` category `907233 / 过程检验规程` has many files, including V2 `ACTIVE` chains such as `CODX-DCC-REV-FULL-20260802-20260802201023` (`V1 SUPERSEDED / V2 ACTIVE`) and `CODX-DCC-DIST-REC-DISTREC20260802173908` (`V1 SUPERSEDED / V2 ACTIVE`). Current category actions are `APPROVE,DOWNLOAD,PRINT,UPLOAD,VIEW`; missing `DISTRIBUTE`, and active category distribution rules are empty, so paper distribution/recovery remains blocked until formal distribution permission/rule is configured.
- Tenant `测试租户` category `900347 / Codex Local DCC Category` has file `CODX-DCC-DIST-900347-DIST90034720260802185602` with `V1 ACTIVE / V2 READY_TO_PUBLISH` and `PAPER` distribution rows. Current category actions include `DISTRIBUTE`, but publish is blocked by missing published `PUBLISH` business approval policy.

## Closeout Checks

- GREEN: `node --check` -> PASS for `dcc-paper-chain-prepare-e2e.cjs` and `dcc-distribution-recovery-e2e.cjs`.
- GREEN: JSON evidence parse -> PASS, `JSON_OK 6`.
- GREEN: task secret scan -> PASS, plaintext password not found in task directory.
- GREEN: `git diff --check -- doc/tasks/20260803-dcc-distribution-recovery-e2e` -> PASS.
