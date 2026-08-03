# Execution Log

## User Intent

- 2026-08-03: 用户反馈页面提示 `请求地址不存在:admin-api/form-center/templates/28/versions/V3.0`。

## BDD / TDD

- BDD: FormCenter slot opens from embedded task snapshot -> Given a FormCenter route/form slot task has been opened through the official `openTask` path, When the frontend renders the task form, Then it must use the embedded template snapshot returned by `openTask` and must not call `/form-center/templates/{id}/versions/{versionNo}`.

## Preflight Evidence

- Read `bug-regression-fix-loop` skill and `references/bug-contract.md`.
- Read `docs/task-closeout-rules.md`, `docs/powershell-encoding.md`, and `docs/powershell-memory.md`.
- `docs/experience-index.md` matched `请求地址不存在` + `FormCenter` to `docs/frontend-development.md#切换填写人-formcenter-槽位导航门禁`.
- Initial `git status --short --branch`: branch `int_main...origin/int_main [ahead 5]` with pre-existing dirty files: `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/frontline/MesFrontlinePqcContextServiceTest.java`, `doc/tasks/20260803-dcc-distribution-recovery-e2e/execution-log.md`, `docs/e2e-rules.md`, `docs/experience-index.md`, and untracked `doc/tasks/20260803-dcc-directory-folder-border/`.

## Milestone Updates

- in_progress: Created task documentation before source changes.
