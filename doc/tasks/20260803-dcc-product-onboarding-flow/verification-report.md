# Verification Report

## Summary

- Result: Targeted backend, schema, frontend static/type, isolated server package, runtime load, and real Playwright E2E verification passed.
- Completion state: Implementation and required verification are ready for closeout; final commit/push is blocked by pre-existing worktree state outside this task.
- 2026-08-03 worktree migration update: independent worktree setup and runtime verification passed. The branch `codex/dcc-product-onboarding-flow-20260803` was created under `D:\IntRuoyiWorktree\dcc-product-onboarding-flow-20260803` and registered as `int_main slot=15` (`8096/48096`). Worktree backend JUnit, frontend static contract, server package, backend health, frontend HTTP, and real Playwright E2E all passed.

## Commands

- PASS: `pnpm ts:check` in `E:\IntRuoyi\IntRuoyiFronted`, exit code 0.
- PASS: `mvn -pl yudao-module-dcc -am "-DskipTests" compile` in `E:\IntRuoyi\IntRuoyiBackend`, BUILD SUCCESS.
- PASS: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` in `E:\IntRuoyi\IntRuoyiFronted`, exit code 0.
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`, Tests run: 106, Failures: 0, Errors: 0, Skipped: 0.
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test`, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest#approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject" "-Dsurefire.failIfNoSpecifiedTests=false" test`, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`, Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- PASS: isolated `mvn -pl yudao-server -am "-DskipTests" package`, BUILD SUCCESS.
- PASS: `node --check ..\doc\tasks\20260803-dcc-product-onboarding-flow\dcc-product-onboarding-real.e2e.cjs`.
- PASS: real Playwright E2E `node ..\doc\tasks\20260803-dcc-product-onboarding-flow\dcc-product-onboarding-real.e2e.cjs`, `requestId=4`, `projectCodeId=258`, `productMasterId=332`, `projectCode=CODXONB03045351`.

## Coverage

- Backend: 建档申请创建、重复目标项目代码拒绝、审批通过生成 DCC 项目代码并绑定 MDM、禁用或无效 MDM 拒绝、受控文件提交继承 MDM 绑定；审批重复检查已覆盖“当前待审批申请自身不算重复”。
- Database: 基础 schema、迁移文件和 DCC 测试 fixture 包含 `dcc_project_code.product_master_id`、`dcc_product_onboarding_request`、状态索引和待审批唯一约束。
- Frontend: 项目代码基础数据页暴露产品建档入口、申请/审批按钮、MDM 产品选择、申请表单字段、API 契约和错误不吞掉的静态合同。
- Compile/type: 后端 DCC 依赖模块 compile 通过；前端 `ts:check` 通过。
- Runtime/E2E: clean detached build jar copied to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-121411-dcc-product-onboarding.jar` with SHA256 `0BDB594204E0FF55CCEB2744D7566493643A27231C404D4424B50BA83051F02B`; during E2E, `48081` was switched to PID `57996` running this product-onboarding jar and health was `UP`; frontend `8081` returned HTTP `200`. After E2E, shared `48081` was externally restored to PID `43876` running `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`, so the current shared runtime is healthy but is not the product-onboarding verification jar.
- Real E2E: 页面真实点击“产品建档申请”、提交申请、审批通过、按项目代码 quick filter 回显生成记录；最终只读 API 核验 DCC 项目代码 `productMasterId=332` 且 MDM 产品 `status=ENABLE`，`criticalNetworkFailures=[]`、`consoleErrors=[]`、`pageErrors=[]`。

## Validator Evidence

- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/backend-api-evidence.md` -> `Backend API evidence is valid.`
- PASS: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/database-schema-evidence.md` -> `Database schema evidence is valid.`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/frontend-feature-evidence.md` -> `Frontend feature evidence is valid.`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/bug-regression-evidence.md` -> `Bug regression evidence is valid.`

## Experience Consolidation

- PASS: Added `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁`.
- PASS: Added `docs/experience-index.md` keywords for DCC 产品立项、产品建档申请、`dcc_product_onboarding_request`、`dcc_project_code.product_master_id` and `productMasterId`.
- PASS: Updated `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁` with approval duplicate checking that ignores the current pending request but blocks other pending requests.
- PASS: `rg -n "DCC 项目代码 MDM 产品建档绑定|dcc_product_onboarding_request|productMasterId" docs\experience-index.md docs\database-rules.md`.
- PASS: `git diff --check -- <task evidence files and updated docs>` returned no whitespace errors.

## Cleanup Evidence

- PASS: `task_closeout.py --task-id 20260803-dcc-product-onboarding-flow --mode preview` kept the real E2E script/result and core task reports; no blocked paths or warnings.
- PASS: `task_closeout.py --task-id 20260803-dcc-product-onboarding-flow --mode apply` deleted temporary backend/database/frontend/bug evidence files after validator PASS was copied here.
- PASS: rerun cleanup preview/apply after latest evidence update kept the real E2E script/result and core task reports; no deleted paths, blocked paths, or warnings.
- BLOCKED: 2026-08-03 worktree closeout preview from `D:\IntRuoyiWorktree\dcc-product-onboarding-flow-20260803` kept only the real E2E script/result and core task reports, but blocked final apply/merge because main worktree `E:\IntRuoyi` is dirty and cannot receive the ff-only merge safely.

## Known Non-Goals And Blockers

- Standard backend restart blocked: `restart-int-ruoyi-local.ps1 -Component backend` failed because unrelated dirty `DccControlledFileNasTransferServiceImpl.java` currently has compile errors (`requireNonNull`, `SelectedUncontrolledImportFile`, `PreparedUncontrolledImportFile` missing). This unrelated file was not modified or reverted; verification used a clean detached build jar for this task.
- Current shared runtime scope: `48081` is currently owned by another task jar (`backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`). Product-onboarding E2E has already passed during the controlled temporary switch to the verified jar; rerunning that E2E would require another explicit runtime switch.
- Full schema suite not claimed: 未将完整 `DccBaseSchemaTest` 作为当前完成门禁；此前已知全量 schema 测试存在与本任务无关的 destructive SQL 检测和 NAS nullable 断言问题。
- Commit/push blocked: 当前 `int_main` 存在并发产生的未推送本地提交，其中包含本任务证据和其它任务文件；工作区还存在非本任务未跟踪/暂存任务文件。按任务所有权边界，本任务未继续打包、提交或推送这些无关改动。

## Worktree Verification Attempt

- PASS: `git worktree add -b codex/dcc-product-onboarding-flow-20260803 D:\IntRuoyiWorktree\dcc-product-onboarding-flow-20260803 origin/int_main`.
- PASS: `reserve-worktree-slot.ps1` registered `slot=15`, frontend `8096`, backend `48096`; both ports had no listeners before startup.
- PASS: `scripts\runtime\show-branch-runtime.ps1` in the worktree reported profile `int_main`, slot `15`, frontend URL `http://127.0.0.1:8096`, backend health URL `http://127.0.0.1:48096/actuator/health`.
- PASS: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` in the worktree frontend.
- PASS: `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` in the worktree backend -> Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- PASS: `mvn.cmd -pl yudao-module-dcc -am "-Dmaven.test.skip=true" install` installed this worktree's DCC/MDM dependency chain into the local Maven repository.
- PASS: `mvn.cmd -pl yudao-server "-Dmaven.test.skip=true" package` generated a worktree server exec jar after the DCC install; refreshed runtime jar SHA256 `6B26B7B7F09F4CCB6C45D45D7B64AA419B74BA9AEFBC64D6621BE1CFCF496FA5`.
- PASS: worktree backend `48096` health `UP`, PID `63408`, using `output\runtime\int_main-slot15\backend-runtime-control-20260803-150013-dcc-product-onboarding-worktree.jar`.
- PASS: `pnpm install --frozen-lockfile` in the worktree frontend; Vite started via `scripts\runtime\start-branch-frontend.ps1 -Slot 15` and `http://127.0.0.1:8096/` returned HTTP `200`.
- PASS: real Playwright E2E through `http://127.0.0.1:8096/mdm/project-code` with local Chrome before origin merge -> `requestId=5`, `projectCodeId=259`, `productMasterId=333`, `projectCode=CODXONB03073324`, `criticalNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- NOTE: Initial worktree E2E failed with `404 请求地址不存在` because the first server-only package reused a stale local-repository DCC module. This was fixed by installing the worktree DCC module and repackaging the server jar.
- PASS: Experience consolidation updated `docs/worktree-memory.md#Worktree Server-Only 打包旧本地仓库模块门禁`.
- PASS: merged latest local `origin/int_main` into the worktree branch; branch runtime port guard passed.
- PASS: post-merge static contract `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js`.
- PASS: post-merge real Playwright E2E through `http://127.0.0.1:8096/mdm/project-code` with local Chrome -> `requestId=6`, `projectCodeId=260`, `productMasterId=334`, `projectCode=CODXONB03074144`, `criticalNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- PASS: merged local `int_main` committed history into the worktree branch; branch runtime port guard passed.
- PASS: post-local-main-merge static contract `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js`.
- PASS: post-local-main-merge real Playwright E2E through `http://127.0.0.1:8096/mdm/project-code` with local Chrome -> `requestId=7`, `projectCodeId=261`, `productMasterId=335`, `projectCode=CODXONB03074622`, `criticalNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- PASS: latest branch runtime port guard on `codex/dcc-product-onboarding-flow-20260803` confirmed worktree profile `int_main slot=15`, frontend `8096`, backend `48096`.
- PASS: stopped task-owned worktree backend PID `63408` and frontend PID `10460` after verifying both command lines belonged to `D:\IntRuoyiWorktree\dcc-product-onboarding-flow-20260803`; `8096/48096` have no remaining listeners.
- PASS: merged the latest local `int_main` commits into the worktree branch again; `git rev-list --left-right --count int_main...codex/dcc-product-onboarding-flow-20260803` -> `0 8`, so local `int_main` is an ancestor of the worktree branch.
- PASS: post-latest-local-main-merge static contract `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` returned exit code `0`.
- PASS: post-latest-local-main-merge DCC JUnit `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- BLOCKED: latest cleanup preview still blocks final apply/merge only because main worktree `E:\IntRuoyi` is dirty and cannot receive ff-only merge safely.
- PASS: after user-requested main workspace commits, main worktree returned to a clean status; worktree merged latest local `int_main` again with merge commit `f1c78e92b`, branch runtime port guard passed, and `git rev-list --left-right --count int_main...codex/dcc-product-onboarding-flow-20260803` returned `0 10`.
- PASS: final pre-merge static contract `node IntRuoyiFronted\tests\e2e\dcc-project-code-product-onboarding-static.spec.js` returned exit code `0`.
- PASS: final pre-merge DCC JUnit `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- PASS: after the latest main baseline commit, worktree merged local `int_main` again with merge commit `e9f52e5c2`, branch runtime port guard passed, and `git rev-list --left-right --count int_main...codex/dcc-product-onboarding-flow-20260803` returned `0 12`.
- PASS: final pre-ff static contract `node IntRuoyiFronted\tests\e2e\dcc-project-code-product-onboarding-static.spec.js` returned exit code `0`.
- PASS: final pre-ff DCC JUnit `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- PENDING: integration still requires committing this latest evidence, merging back to local `int_main`, and pushing if GitHub HTTPS proxy/network permit.
