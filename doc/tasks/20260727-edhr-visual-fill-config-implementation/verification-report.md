# Verification Report

## Scope

- Task: eDHR 可视化填写配置实现验证。
- Completed through: T09 真实用户路径 E2E。
- Remaining integration scope: 提交推送、融合 `int_main`、融合后完整 E2E。

## Passed Verification

- Main int_main runtime Jar reload: copied `IntRuoyiBackend\yudao-server\target\yudao-server-exec.jar` to `output\runtime\int_main\yudao-server-exec.jar`, SHA256 `38503FDC04059E47E52862B575F097C31289A8AAFCBC4BCA4118E66303A43C51`; stopped old `48081` PID `29284`, started stable Jar as listener PID `54028`, backend health `UP`, frontend `8081` HTTP `200`.
- Post-merge main-port E2E RED: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> FAIL，`frontend port must be int_main worktree slot 1..19, got 8081`，脚本仅允许附加 worktree，阻塞融合后主运行态验证。
- Post-merge main-port E2E GREEN: `node tests\e2e\edhr-visual-fill-config-static.spec.js` -> PASS，`node --check tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS，脚本仅允许 `int_main 8081/48081` 或成对 `int_main slot 1..19`。
- Main int_main real E2E: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS，tenant `芋道源码`，accounts `admin` / `jiazeyu` / `wangxin`，base URL `http://127.0.0.1:8081`，backend URL `http://127.0.0.1:48081`。
- Main int_main real E2E cleanup: task-owned batch `900000000893 / EDHRB-1785177596364` was voided, task-owned route `922231 / CODX-VFC-20260727` was deleted, and target report config was restored to `ruleCount=87`、`assistRowCount=87`、`fillRuleStatus=CONFIGURED`。
- Post-integration route-copy attachment owner regression: `mvn "-Dtest=MesProRouteServiceImplTest#copyRoute_shouldRefreshActiveVersionWithCompleteConfigSnapshotAfterChildConfigsCopied" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS，目标路线 ACTIVE 快照保留来源 `batchRecordAttachmentOwners`。
- Route/backend focused regression after fix: `mvn "-Dtest=MesProRouteServiceImplTest,MesProRouteFlowConfigServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS，`Tests run: 57, Failures: 0, Errors: 0`。
- Backend focused integration regression after fix: `mvn "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProEdhrRehearsalReadinessServiceTest,MesProRouteServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS，`Tests run: 321, Failures: 0, Errors: 0`。
- Frontend static contracts after fix: `node tests\e2e\edhr-visual-fill-config-static.spec.js`、`mes-route-flow-batch-record-detail-slot-filter-static.spec.js`、`mes-route-flow-legacy-batch-record-detail-static.spec.js`、`mes-route-flow-batch-record-panel-visible-static.spec.js`、`mes-route-flow-batch-record-item-restore-static.spec.js`、`route-batch-record-save-contract-static.spec.js` -> PASS。
- Frontend type check after fix: `pnpm ts:check` -> PASS。
- Backend package after fix: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS，runtime Jar SHA256 `37CC88F87847D7A1965C08A02DDFE285E3414119E2E2FAA1CA4E40045255CAB1`。
- Runtime reload after fix: slot 2 backend `48083` restarted from `output\runtime\slot2\yudao-server-exec.jar` as port PID `42408`; backend health `UP` and frontend `8083` HTTP `200`。
- Real E2E after fix: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS，tenant `芋道源码`，accounts `admin` / `jiazeyu` / `wangxin`，base URL `http://127.0.0.1:8083`，backend URL `http://127.0.0.1:48083`。
- Real E2E cleanup after fix: task-owned batch `900000000891 / EDHRB-1785175736424` was voided, task-owned route `922229 / CODX-VFC-20260727` was deleted, and target report config was restored to `ruleCount=87`、`assistRowCount=87`、`fillRuleStatus=CONFIGURED`。
- Backend focused regression: `mvn "-Dtest=MesProEdhrWorkTaskServiceImplTest,MesProEdhrBatchExecutionServiceTest,MesProEdhrProcessFormPermissionRuleServiceImplTest,MesProRouteFlowConfigServiceImplTest,MesProEdhrRehearsalReadinessServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test -pl yudao-module-mes -am` -> PASS，`Tests run: 288, Failures: 0, Errors: 0`。
- Frontend static contracts: `node tests\e2e\edhr-visual-fill-config-static.spec.js`、`mes-route-flow-batch-record-detail-slot-filter-static.spec.js`、`mes-route-flow-legacy-batch-record-detail-static.spec.js`、`mes-route-flow-batch-record-panel-visible-static.spec.js`、`mes-route-flow-batch-record-item-restore-static.spec.js`、`route-batch-record-save-contract-static.spec.js` -> PASS。
- Frontend type check: `pnpm ts:check` -> PASS。
- Backend package: `mvn -pl yudao-server -am "-DskipTests" package` -> PASS，Jar time `2026-07-27 23:44:09 +08:00`，SHA256 `CB4F4650E6D70806694E76DF4C18A0FEECF8CAEFDC1A67316B45E3064077DAAD`。
- Runtime reload: slot 2 backend `48083` restarted from the current worktree Jar as PID `50564`; backend health `UP` and frontend `8083` HTTP `200`。
- Cleanup-only regression: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js --cleanup-only` -> PASS，already-voided task-owned batch is handled idempotently and task route is absent.
- Real E2E: `node tests\e2e\edhr-visual-fill-config-real-flow.e2e.js` -> PASS，tenant `芋道源码`，accounts `admin` / `jiazeyu` / `wangxin`，base URL `http://127.0.0.1:8083`，backend URL `http://127.0.0.1:48083`。
- Real E2E cleanup evidence: task-owned batch `900000000889 / EDHRB-1785167428536` was voided, task-owned route `922227 / CODX-VFC-20260727` was deleted, and target report config was restored to `ruleCount=87`、`assistRowCount=87`、`fillRuleStatus=CONFIGURED`。


## Closeout Verification

- Main push: `git push origin int_main` -> PASS，推送后 `git status --short --branch` 不再 ahead。
- Main cleanup apply: task-closeout cleanup applied in `E:\IntRuoyi`; deleted only `backend-api-evidence.md` and `frontend-feature-evidence.md`.
- Linked worktree closeout: `git cherry int_main codex/202607727_yingshe_integration` marked all 5 integration commits as already applied; task-owned `8083/48083` processes stopped; `D:\IntRuoyiWorktree\202607727_yingshe` removed; slot 2 registry entry set inactive.

## Superseded Blockers

- `edhr-shared-form-binding-static.spec.js` still fails because historical file `IntRuoyiFronted/src/views/mes/pro/route/RouteProcessList.vue` is absent; it remains outside this visual-fill-config scope.
- Earlier `task_owned_batch_route_missing_target_report_binding` was resolved by creating and deleting a task-owned route copy through the real UI instead of modifying shared route `RT000028`.
- Earlier cleanup blocker was resolved by treating list-hidden already-voided task-owned batches as idempotent cleanup after read-only status confirmation.

## Design Constraints

- No fallback, downgrade, mock, API-only write path, direct SQL shortcut, or swallowed exception added.
- Existing `BatchRecordCellRulesConfirmDialog.vue`、`cell-rules` API、`save-by-report` API、execution snapshot, work task, route copy, route candidate, route publish and batch page capabilities were reused.
- 工序开始、批记录表单、表单槽位继续严格分离；正式批记录报表只使用显式 `batchRecordReports`，不从 `formBindings` 补齐或推断。
