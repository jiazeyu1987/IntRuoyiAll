# eDHR 主数据追溯分页 itemCount 合同修复 Execution Log

BDD: 主数据追溯分页返回追溯项数量 -> Given eDHR 执行记录已有 DomainTrace snapshot 和真实 item 明细, When 前端查询 `/domain-trace/page`, Then 分页行返回 `itemCount` 等于该 snapshot 下真实 item 数量，并继续返回 `blockerCount`、`status`、`domainTraceHash`。

BDD: 主数据追溯分页未产生快照时不伪造数量 -> Given eDHR 执行记录尚无 DomainTrace snapshot, When 前端查询 `/domain-trace/page`, Then 分页行不得伪造 `itemCount=0` 作为已验证证据，而应保持缺少追溯快照的阻塞语义。

BDD: 主数据追溯列表 E2E 可放行 -> Given 测试租户存在已验证 DomainTrace 执行记录, When 前端真实 E2E 打开主数据追溯列表, Then 页面可展示来自后端分页的 `items=<itemCount>` 并从列表进入详情。

GREEN: M1 task package created before backend production code changes.

RED: Frontend reviewer real E2E `pnpm e2e:edhr:domain-trace` -> FAIL, `主数据追溯列表目标行 缺少 itemCount 来源：必须提供 itemCount 或 items.length。` Impact: `/domain-trace/page` cannot yet prove list item count for production release.

RED: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> FAIL, expected reason: new regression tests require `MesProBatchRecordDomainTracePageRespVO#getItemCount()` and a backend source for persisted DomainTrace item counts, both missing from current backend contract.

Root cause: `/domain-trace/page` built rows from execution and snapshot metadata only. `blockerCount` came from the persisted snapshot, but the page response contract had no `itemCount` field and the service never counted persisted snapshot item rows.

GREEN: `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors. Implementation adds `itemCount` to the page VO and fills it from persisted item rows only when a real snapshot exists; rows without snapshot keep `itemCount=null`.

GREEN: `git diff --check` -> PASS, no whitespace errors. Git reported LF-to-CRLF normalization warnings for touched Java files only.

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-domain-trace-page-item-count --mode preview` -> blocked, no delete candidates. Blockers are linked-worktree merge guards and main worktree dirty state; cleanup apply was not run.

Bug: eDHR domain trace page rows did not expose a real persisted item count, so the frontend list could not prove `items=<count>`.
Expected: Rows with a real DomainTrace snapshot return `itemCount` from persisted snapshot item records, while rows without snapshot keep `itemCount=null`.
Reproduction: Frontend reviewer real E2E `pnpm e2e:edhr:domain-trace` failed with `缺少 itemCount 来源`; backend RED test compile failed before the contract field/helper existed.
Root Cause: The page response VO only had `blockerCount`, and `toPageRow` never counted persisted DomainTrace item rows.
Verification: Targeted Maven test passed, `git diff --check` passed, and bug evidence validation passed after this section is present.
Blockers: Frontend real E2E rerun and final commit remain owned by the main reviewer; closeout cleanup apply is blocked by linked-worktree merge guards and dirty main worktree state.

GREEN: Reviewer `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors.

GREEN: Reviewer `mvn -pl yudao-server -am -DskipTests package` -> PASS after stopping the stale local Java process that locked `yudao-server\target\yudao-server.jar`; packaging then produced the fixed backend jar successfully.

GREEN: Reviewer started the current fixed backend runtime on `48098` because the existing frontend `http://localhost:8081` proxy targets `http://127.0.0.1:48098`. This is an explicit target-environment prerequisite for the current local E2E, not a fallback.

GREEN: Frontend reviewer real E2E `pnpm e2e:edhr:domain-trace` with tenant `测试租户`, executor `aoteman`, execution `40 / BRE202605280518101280040` -> PASS. The list page received `itemCount=8`, `blockerCount=0`, `status=VERIFIED`, then entered detail and completed final verification with the same `itemCount=8`, `blockerCount=0`, `status=VERIFIED`, hash `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

NOTE: Local backend logs still contain unrelated scheduled-task errors for missing table `dcc_nas_acl_restore_plan`; this did not affect the eDHR DomainTrace contract or E2E path, but should remain a separate environment/schema readiness item rather than be hidden by this task.

GREEN: Final reviewer `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors.

GREEN: Final reviewer `git diff --check` -> PASS, no whitespace errors; Git reported only LF-to-CRLF normalization warnings for touched Java files.

GREEN: Final reviewer `mvn -pl yudao-server -am -DskipTests package` -> PASS, full backend reactor through `yudao-server` built and repackaged `yudao-server\target\yudao-server.jar`.

GREEN: Final frontend real E2E reuse against the current fixed backend on `48098` -> PASS, proving the backend page contract supports the list-to-detail user path with real `itemCount=8`.

REVIEW: Independent reviewer -> PASS, no blocking findings. Reviewer confirmed backend scope is limited to DomainTrace page contract, rows without snapshot do not fake evidence, frontend E2E follows the real list-to-detail path, and BDD/RED/GREEN evidence is present.

GREEN: Reviewer performance hardening `mvn -pl yudao-module-mes "-Dtest=MesProBatchRecordDomainTraceServiceTest,MesProBatchRecordDomainTraceControllerTest" test` -> PASS, 11 tests run, 0 failures, 0 errors. The final implementation batch-loads page snapshots and snapshot item rows, then counts by snapshotId in memory, avoiding a new per-row item-count query on the list page.

BLOCKER: Starting the rebuilt backend jar on `48098` without DCC signature evidence config -> FAIL FAST, expected environment prerequisite surfaced as `DCC electronic signature evidence configuration is missing`. This is unrelated to DomainTrace and was not bypassed.

GREEN: Restarted current rebuilt backend jar on `48098` with explicit local DCC E2E signature evidence config from existing DCC task evidence (`CODEX-DCC-E2E-HMAC-SECRET-20260526` / `codex-e2e-v1`) -> PASS, actuator health returned UP. The recurring local `dcc_nas_acl_restore_plan` missing-table scheduled task error remains unrelated environment noise.

GREEN: Final frontend real E2E against the current rebuilt backend jar on `48098` -> PASS, `pnpm e2e:edhr:domain-trace` completed with list/final `status=VERIFIED`, `blockerCount=0`, `itemCount=8`, hash `2c7c5aa13178e7c452697672e86ca1efa2c22ca00ada1e2ff22da0e19dd72a79`.

CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260528-edhr-domain-trace-page-item-count --mode preview` -> blocked, delete candidates `<none>`. Cleanup apply was not run because the linked worktree cannot be fast-forward merged into `int_main`, the main worktree is dirty, and task code changes are still pending for the current commit.
