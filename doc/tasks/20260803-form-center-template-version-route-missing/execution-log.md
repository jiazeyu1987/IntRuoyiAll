# Execution Log

## User Intent

- 2026-08-03: 用户反馈页面提示 `请求地址不存在:admin-api/form-center/templates/28/versions/V3.0`。

## BDD / TDD

- BDD: FormCenter slot opens from embedded task snapshot -> Given a FormCenter route/form slot task has been opened through the official `openTask` path, When the frontend renders the task form, Then it must use the embedded template snapshot returned by `openTask` and must not call `/form-center/templates/{id}/versions/{versionNo}`.
- RED: `node -e "<pre-merge 03646727b ActionFormPanel contract>"` -> FAIL, expected reason: pre-merge runtime panel still called `getTemplateVersion(templateId, versionNo)`.
- GREEN: `node tests\e2e\edhr-switch-filler-formcenter-slot-static.spec.js` -> PASS, current `ActionFormPanel` uses the embedded `openTask` template snapshot and does not call the template management endpoint.
- GREEN: `node tests\e2e\edhr-dynamic-form-action-panel-prefill-static.spec.js` -> PASS.
- GREEN: `node tests\e2e\form-center-static.spec.js` -> PASS.
- GREEN: `mvn.cmd -pl yudao-module-bpm -am "-Dtest=FormCenterRuntimeContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" "-DforkCount=0" test` -> PASS, 9 tests, backend route mapping includes `/form-center/templates/{templateId}/versions/{versionNo}`.
- GREEN: `Invoke-WebRequest http://127.0.0.1:48081/admin-api/form-center/templates/28/versions/V3.0` without login -> HTTP 200 wrapper with business `code=401,msg=账号未登录`; this proves the live local backend no longer returns `请求地址不存在` for the reported URL.

## Preflight Evidence

- Read `bug-regression-fix-loop` skill and `references/bug-contract.md`.
- Read `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, and `docs/powershell-memory.md`.
- `docs/experience-index.md` matched `请求地址不存在` + `FormCenter` to `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁`.
- Initial `git status --short --branch`: branch `int_main...origin/int_main [ahead 5]` with pre-existing dirty files: `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java`, `doc/tasks/20260803-dcc-distribution-recovery-e2e/execution-log.md`, `docs/e2e-rules.md`, `docs/experience-index.md`, and untracked `doc/tasks/20260803-dcc-directory-folder-border/`.
- Read `docs/frontend-development.md`, `docs/backend-development.md`, `docs/local-runtime.md`, and `docs/worktree-restrictions.md` before frontend/backend/runtime checks.

## Milestone Updates

- completed: Current source already contains the FormCenter route-missing fix in the recent merge; no production source edits were needed in this turn.
- completed: Verified the current local backend process on PID `45524` is listening on `48081`, health is `UP`, and the target URL now reaches authentication instead of request-address-not-found.
- completed: `task_closeout.py --task-id 20260803-form-center-template-version-route-missing --mode preview` -> PASS, keep task records, delete none, blocked none.
- completed: `task_closeout.py --task-id 20260803-form-center-template-version-route-missing --mode apply` -> PASS, deleted none.
- note: Concurrent baseline commit `61d406ca6` landed during this turn and already included `task.md`, `verification-report.md`, and earlier `execution-log.md` content for this task. This follow-up log delta records cleanup evidence and the no-mixed-commit decision only.
- note: Scoped closeout commit `8261fe08d` accidentally included three unrelated DCC controlled-print task docs. Follow-up repair commit `b869579ac` reversed only those DCC doc hunks from the branch, then the saved patch was reapplied to the working tree so the other task's edits remain preserved and unstaged.
- blocker: Full branch push/clean closeout is still not task-owned because the branch has unrelated ahead commits and unrelated dirty DCC task files. This task's remaining log update can be committed separately, but broad cleanup or push must not mix those unrelated changes without user coordination.

## Experience Consolidation

- No new long-term experience entry added. Existing `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁` and `docs/local-runtime.md#2026-07-24 隔离构建 Jar 加载门禁` already cover this failure mode.
