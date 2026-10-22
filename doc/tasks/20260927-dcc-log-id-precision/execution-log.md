# Execution Log

## 2026-09-27

- Preflight: read `AGENTS.md`, `docs/task-closeout-rules.md`, `docs/e2e-rules.md`, `docs/local-runtime.md`, `docs/worktree-restrictions.md`, and `docs/login-access.md`.
- Scope confirmed: DCC logs API/page, related VO/type/static/unit tests only. No database, business data, Git, or service restart actions are authorized for this fix.
- Root cause located: `src/views/dcc/controlled-file/logs/index.vue` used `Number(route.query.controlledFileId)` through `getNumberQueryValue`, so `2054545668044084022` was rounded before `getControlledFileLogPage` serialized request params. The page also did not initialize `keyword` from the URL.
- BDD recorded in `task.md`; RED/GREEN evidence will be appended after each verification command.

## RED

- `cd IntRuoyiFronted; node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> FAIL on the old implementation because `DccControlledFileLogPageReqVO.controlledFileId` was `number` and the page initialized it with `getNumberQueryValue(route.query.controlledFileId)`.
- The first post-implementation service run exposed a test-fixture error, not a product regression: the new 19-digit sample used `status=OBSOLETE` with `changeType=NEW`, so the existing lifecycle rules correctly emitted a submission candidate instead of an obsolete candidate. The fixture was corrected to `changeType=OBSOLETE` before the final run.

## GREEN

- `cd IntRuoyiFronted; node tests/e2e/dcc-controlled-file-logs-static.spec.js` -> PASS.
- `cd IntRuoyiBackend; .runtime/tools/apache-maven-3.9.11/bin/mvn.cmd -pl yudao-module-dcc -am -Dtest=DccControlledFileLogQueryServiceTest,DccControlledFileLogControllerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> BUILD SUCCESS; Controller `2/2`, Query Service `6/6`, total `8/8`, Failures `0`, Errors `0`. Coverage includes exact 19-digit filtering and invalid-ID rejection.
- Related static contracts -> PASS: `dcc-detail-lifecycle-timeline-static.spec.js`, `bpm-process-timeline-current-node-green-static.spec.js`, `dcc-traceability-ux-static.spec.js`, `dcc-detail-trace-lists-standard-template-static.spec.js`.
- `cd IntRuoyiFronted; pnpm ts:check` -> `TS_CHECK_EXIT=0`.
- `git diff --check` on the task files -> no whitespace errors; only the repository's existing LF/CRLF conversion warnings.

## Change Boundary

- Changed only the DCC log request VO/service boundary, DCC logs API/page/type/static contract, the DCC log Controller/Query Service unit tests, and this task evidence.
- No database schema, database data, business data, service restart, `git add`, commit, or push was performed.
