# Execution Log

BDD: Commit push and restart current runtime -> Given current int_main has backend code changes, When verification passes and user requested commit/push/restart, Then the code is committed, pushed to origin/int_main, and local frontend/backend runtime is restarted on 8081/48081.

Rules read:
- AGENTS.md
- docs/task-closeout-rules.md
- docs/local-runtime.md
- docs/backend-development.md
- docs/frontend-development.md
- docs/branch-runtime-ports.md
- docs/worktree-restrictions.md

Status:
- Task directory created.
- GREEN: branch-runtime-port-guard -> PASS, `scripts\preflight\branch-runtime-port-guard.ps1`.
- GREEN: targeted Maven regression -> PASS, `mvn.cmd -pl yudao-module-erp,yudao-module-mes -am '-Dtest=ErpKingdeeProductionOrderClientImplTest,MesKingdeeProductionOrderCreateServiceImplTest' '-Dsurefire.failIfNoSpecifiedTests=false' test`; ERP 13 tests PASS, MES 10 tests PASS.
- GREEN: git-diff-check -> PASS, `git diff --check`.
- Baseline commit: `15d72252e fix: use Kingdee template unit for production orders`.
- Baseline files: IntRuoyiBackend ERP/MES Kingdee production order source and tests; docs/e2e-rules.md; docs/experience-index.md.
- RED: not rerun in this operational commit task -> BLOCKED, code/test/doc changes already existed in dirty worktree before this task began; current task scope is verification, commit, push, and restart.
- GREEN: experience-preflight -> PASS, project-experience-consolidation skill read and existing experience index already covers ignored `doc/tasks` / `git add -f` handling; no new long-term memory document needed.
- GREEN: task-closeout-cleanup preview -> PASS, keep task.md/execution-log.md/verification-report.md, delete none, blocked none, warnings none.
- GREEN: task-closeout-cleanup apply -> PASS, linked worktree false, delete none.
- GREEN: standard local full restart -> PASS, `IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component full`; Maven package BUILD SUCCESS and restart command dispatched for int_main frontend=8081/backend=48081.
- GREEN: backend runtime -> PASS, PID 37360 listens on 48081, health UP, `Started YudaoServerApplication` at 2026-09-16 08:39:27, runtime jar SHA256 `380D7EDD7037EB095AE7E623A38823249F7ABF89DD3BE79D0DE5168308AE57EE`.
- GREEN: frontend runtime -> PASS, PID 42512 listens on 8081, Vite ready in 60559 ms, HTTP 200.
- BLOCKER: git push origin int_main -> FAIL, `TLS connect error: unexpected eof while reading`.
- BLOCKER: git ls-remote --heads origin int_main -> FAIL, same GitHub HTTPS TLS connect error; current branch remains ahead of origin.
