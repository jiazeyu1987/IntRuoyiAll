# Execution Log

BDD: fixed DCC approval policy rejects unsupported editable values -> Given the DCC BPMN currently executes a fixed four-stage approval model, When a route save request tries to set unsupported approval method, approval ratio, or required-switch values, Then the backend rejects the route and does not persist misleading approval policy evidence.

BDD: route form exposes fixed approval policy -> Given an admin edits a DCC approval route, When the route form renders the approval policy columns, Then approval method, approval ratio, and required state are shown as fixed rules and the save payload uses only supported fixed values.

RED: backend focused route approval policy test -> expected FAIL before implementation because `DccApprovalRouteAdminServiceImpl` currently accepts request `approveMethod`, `approveRatio`, and `required` values without validating them against fixed BPMN policy.

RED: frontend static route approval policy contract -> expected FAIL before implementation because `RouteForm.vue` currently renders editable approval method, approval ratio, and required controls.

RED: `mvn -pl yudao-module-dcc -Dtest=DccApprovalRouteAdminServiceImplTest#testSaveRoute_unsupportedFixedApprovalPolicy_throwsExplicitFailure test` -> FAIL, expected `ServiceException` but nothing was thrown, proving unsupported route approval policy values were accepted before the fix.

RED: `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> FAIL, missing `FIXED_ROUTE_APPROVAL_POLICY` / `normalizeRouteNodeFixedApprovalPolicy` and current form still had editable `row.approveMethod`, `row.approveRatio`, and `row.required` controls.

GREEN: `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> PASS, 2 frontend static contract tests passed.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest" test` -> PASS, 21 backend tests passed.

CHECK: `git diff --check` -> PASS.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-018-route-approval-policy\verification-report.md` -> PASS, bug regression evidence is valid.

CHECK: UTF-8 task docs readback -> PASS for `task.md`, `execution-log.md`, and `verification-report.md`.

BLOCKED-NONGATING: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-dcc-static-018-route-approval-policy --mode preview` -> BLOCKED, current linked worktree branch could not be resolved because HEAD is detached. Preview kept the three task evidence files and had no delete candidates.

BLOCKED-NONGATING: `pnpm exec eslint src/views/dcc/controlled-file/routes/components/RouteForm.vue scripts/dcc-route-fixed-approval-policy-static.test.mjs` -> FAIL before eslint ran because `eslint` is not installed/resolvable in this clean frontend worktree.

BLOCKED-NONGATING: `pnpm ts:check` -> FAIL before vue-tsc ran because `cross-env` is not installed/resolvable and pnpm reported local `node_modules` is missing.

CHECK: project-experience-consolidation -> reviewed existing `docs/worktree-memory.md` and `docs/test-release-preflight.md`; no new long-term document was created because PowerShell Surefire comma quoting and clean worktree frontend dependency checks are already covered by existing memory/preflight guidance.

CHECK: user authorization -> 2026-09-13 user explicitly authorized Git closeout after being told the remaining actions were branch creation/binding, task commit, push, cleanup apply, and worktree merge/removal.

CHECK: worktree migration -> created `D:\IntRuoyiWorktree\20260913-dcc-static-018-route-approval-policy` on branch `codex/20260913-dcc-static-018-route-approval-policy` from `6c6487c9f151244910b9ff80454c397bc303e330`, then rebased to `origin/int_main` and registered the current mainline runtime registry as `int_main` slot 8 with frontend `8089` and backend `48089`.

GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS in the registered D worktree for `codex/20260913-dcc-static-018-route-approval-policy/int_main`, frontend `8089`, backend `48089`.

GREEN: `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> PASS in the registered D worktree, 2 frontend static contract tests passed.

GREEN: `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest" test` -> PASS in the registered D worktree, 21 backend tests passed.

CHECK: `git diff --check` -> PASS in the registered D worktree.

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-dcc-static-018-route-approval-policy\verification-report.md` -> PASS in the registered D worktree.

CHECK: experience-preflight -> PASS, existing `docs\worktree-memory.md` already covers dirty main workspace remote fast-forward integration and task branch closeout blockers; no new long-term document was created.

REGRESSION: rebase to `origin/int_main` -> initial targeted Maven rerun failed because upstream future-effective-route tests still used the old stage 3 `ALL` fixture, which conflicts with the fixed BPMN policy.

GREEN-FIX: future-effective-route fixture alignment -> updated only the upstream-added test fixture rows so matrix approval stage 3 uses fixed `ANY` / not-all approval metadata.

GREEN: post-rebase `mvn -pl yudao-module-dcc "-Dtest=DccApprovalRouteAdminServiceImplTest,DccControlledFileApprovalRouteAssigneeResolverTest" test` -> PASS, 24 backend tests passed.

GREEN: post-rebase `node scripts\dcc-route-fixed-approval-policy-static.test.mjs` -> PASS, 2 frontend static contract tests passed.

GREEN: post-rebase `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS for `codex/20260913-dcc-static-018-route-approval-policy/int_main`, frontend `8089`, backend `48089`.

CHECK: post-rebase `git diff --check` -> PASS.