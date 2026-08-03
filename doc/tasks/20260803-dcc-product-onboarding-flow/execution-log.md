# Execution Log

## User Intent

- 用户要求补全“产品立项/建档闭环”：从一个还不在 DCC 项目代码里的产品发起建档、审批、生成 DCC 项目代码、关联 MDM 产品，并进行开发验证。

## Rule And Skill Bootstrap

- 已读取 `docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/frontend-development.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`。
- 已读取 `docs/experience-index.md`，命中 DCC 文控审批、DCC 基础条目/项目代码、数据库 schema 核对、前端静态契约隔离等门禁。
- 已读取技能：`backend-api-delivery`、`frontend-feature-delivery`、`database-schema-delivery`、`behavior-driven-development` 及其 evidence contract。

## BDD

- BDD: 产品建档申请生成待审批单 -> Given 一个产品尚未存在 DCC 项目代码 / When 用户提交包含 MDM 产品信息和目标 DCC 项目代码的建档申请 / Then 系统创建待审批申请 / And 不立即生成正式 DCC 项目代码。
- BDD: 审批通过生成正式 DCC 项目代码并绑定 MDM -> Given 产品建档申请处于待审批状态 / When 审批人审批通过 / Then 系统必须创建或绑定启用状态的 MDM 产品 / And 生成启用的 DCC 项目代码 / And DCC 项目代码记录 `productMasterId`。
- BDD: 重复 DCC 项目代码必须拒绝 -> Given DCC 项目代码已存在 / When 用户提交相同目标项目代码的建档申请 / Then 请求被拒绝 / And 不创建申请、MDM 产品或 DCC 项目代码。
- BDD: 禁用 MDM 产品不能被绑定 -> Given 目标 MDM 产品存在但状态为禁用 / When 用户审批通过建档申请 / Then 审批动作失败并提示 MDM 产品不可用 / And 不生成 DCC 项目代码。
- BDD: 受控文件提交沿用 MDM 产品绑定 -> Given DCC 项目代码已由建档闭环生成并绑定 MDM 产品 / When 用户基于该项目代码提交受控文件 / Then 受控文件必须保存 `productMasterId`、产品编码和产品名称的正式 MDM 来源。
- BDD: 页面入口暴露建档申请失败原因 -> Given 用户在 DCC 项目代码基础数据页发起产品建档 / When 后端因重复编码、缺必填或禁用 MDM 产品拒绝请求 / Then 页面必须展示真实失败原因，不吞掉错误或默认成功。

## Command Log

- Bootstrap: `git status --short --branch` -> 当前 `int_main` 领先 origin 5 个提交，存在多个无关脏改动；本任务将只触碰 `doc/tasks/20260803-dcc-product-onboarding-flow` 和本功能相关代码，提交前按项目策略复核。
- Continue bootstrap: `git status --short --branch` -> 当前 `int_main...origin/int_main [ahead 15]`，存在本任务文件与多个无关 MES、DCC 目录图标、历史任务文档脏改动；本次继续只编辑本任务证据文件和已属本任务范围的功能代码。
- RED: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` -> FAIL, expected reason: 实现前 `package.json` 缺少静态契约脚本，项目代码页缺少“产品建档申请”入口、申请/审批按钮和建档 API 调用。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 实现前缺少 DCC 产品建档申请服务、审批生成项目代码、MDM 产品绑定和受控文件提交沿用 MDM 来源；首轮实现后曾暴露错误消息断言不一致并已修正。
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL, expected reason: 实现前基础 schema/test fixture 缺少 `dcc_project_code.product_master_id` 与 `dcc_product_onboarding_request`。
- GREEN: `pnpm ts:check` in `IntRuoyiFronted` -> PASS, exit code 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-DskipTests" compile` in `IntRuoyiBackend` -> PASS, BUILD SUCCESS.
- GREEN: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js` in `IntRuoyiFronted` -> PASS, exit code 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 106, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/backend-api-evidence.md` -> PASS, Backend API evidence is valid.
- GREEN: `python C:\Users\BJB110\.codex\skills\database-schema-delivery\scripts\validate_database_schema.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/database-schema-evidence.md` -> PASS, Database schema evidence is valid.
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-dcc-product-onboarding-flow/frontend-feature-evidence.md` -> PASS, Frontend feature evidence is valid.
- Experience consolidation: updated `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁` and `docs/experience-index.md` route keywords for DCC 产品立项 / 建档申请 / `dcc_product_onboarding_request` / `productMasterId`.
- Verification: `rg -n "DCC 项目代码 MDM 产品建档绑定|dcc_product_onboarding_request|productMasterId" docs\experience-index.md docs\database-rules.md` -> PASS, new route and gate located.
- Verification: `git diff --check -- <task evidence files and updated docs>` -> PASS, only CRLF conversion warnings, no whitespace errors.
- REGRESSION NOTE: full `DccBaseSchemaTest` 未作为当前完成门禁复跑；此前已知存在与本任务无关的 destructive SQL 检测和 NAS nullable 断言失败，不写作 PASS。
- Continuation runtime check: `Invoke-WebRequest http://127.0.0.1:8081/` -> 200；`Invoke-RestMethod http://127.0.0.1:48081/actuator/health` -> UP。
- Runtime schema precondition: prior continuation had applied `IntRuoyiBackend\sql\mysql\20260803_dcc_product_onboarding_flow.sql` to local Docker MySQL and verified `dcc_project_code.product_master_id` / `dcc_product_onboarding_request` exist; no production/test-server data touched.
- RED: `node ..\doc\tasks\20260803-dcc-product-onboarding-flow\dcc-product-onboarding-real.e2e.cjs` -> FAIL, expected reason: create request succeeded (`requestId=1`) but approve returned `1080000192 DCC product onboarding project code already exists or is pending`; root cause was approval duplicate check counted the current pending request itself.
- RED: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest#approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> FAIL before fix, expected reason: current pending request was treated as duplicate.
- Fix: `DccProductOnboardingServiceImpl.validateProjectCodeAvailable` now keeps create-time duplicate blocking unchanged, and approval passes current request id so only other pending requests or existing DCC project codes block approval.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest#approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 1, Failures: 0, Errors: 0, Skipped: 0.
- GREEN: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS, Tests run: 107, Failures: 0, Errors: 0, Skipped: 0.
- Runtime blocker: standard `.\IntRuoyiBackend\script\deploy\restart-int-ruoyi-local.ps1 -Component backend` failed before startup because unrelated dirty file `DccControlledFileNasTransferServiceImpl.java` does not compile (`requireNonNull`, `SelectedUncontrolledImportFile`, `PreparedUncontrolledImportFile` missing); this blocker is outside the product onboarding task and was not modified.
- Isolated build: created task-owned detached worktree `D:\IntRuoyiWorktree\dcc-product-onboarding-build-20260803`, applied only this service/test patch, and ran the same focused JUnit -> PASS, Tests run: 1.
- Isolated package: `mvn -pl yudao-server -am "-DskipTests" package` in the detached worktree -> PASS, BUILD SUCCESS; copied resulting jar to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-121411-dcc-product-onboarding.jar`, SHA256 `0BDB594204E0FF55CCEB2744D7566493643A27231C404D4424B50BA83051F02B`.
- Runtime recovery: stopped old/conflicting same-profile PIDs `13048` and `58148`; started new jar on `48081` as PID `5852`; health returned UP and listener command line points to the new product-onboarding jar.
- E2E script check: `node --check ..\doc\tasks\20260803-dcc-product-onboarding-flow\dcc-product-onboarding-real.e2e.cjs` -> PASS.
- E2E first post-fix pass exposed script-page pagination assumption: create/approve succeeded (`requestId=2`, `projectCodeId=256`, `productMasterId=330`) but script waited for the new project code on the current unfiltered page; patched script to use the page's standard quick filter by `项目代码`.
- GREEN: `node ..\doc\tasks\20260803-dcc-product-onboarding-flow\dcc-product-onboarding-real.e2e.cjs` in `IntRuoyiFronted` with local Chrome -> PASS: `requestId=3`, `projectCodeId=257`, `productMasterId=331`, `projectCode=CODXONB03042211`; result JSON records `criticalNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- Cleanup: removed task-owned detached worktree `D:\IntRuoyiWorktree\dcc-product-onboarding-build-20260803`; deleted temporary startup helper script; retained real E2E `.cjs` and result JSON via `## Cleanup Keep`.
- Cleanup preview/apply: `task_closeout.py --task-id 20260803-dcc-product-onboarding-flow --mode preview/apply` -> PASS; kept `task.md`, `execution-log.md`, `verification-report.md`, real E2E script and result JSON; deleted temporary backend/database/frontend/bug evidence files after their PASS results were copied into this log and verification report.
- Cleanup keep visibility: `git check-ignore -v doc/tasks/20260803-dcc-product-onboarding-flow/dcc-product-onboarding-real.e2e.cjs` -> `.gitignore:99:doc/tasks/**/*.cjs`; if this task is committed later, the retained E2E script must be staged with `git add -f`.
- Experience consolidation: updated existing `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁` to include approval duplicate checking that ignores the current pending request but blocks other pending requests.
- Continue after runtime recovery: `git status --short --branch --untracked-files=all` -> `int_main...origin/int_main`, no ahead marker, but multiple non-task dirty files remain; task-owned changed files remain limited to product onboarding service/test, task evidence, cleanup deletes, real E2E result and `docs/database-rules.md`.
- Continue after runtime recovery: `Get-NetTCPConnection -LocalPort 48081,8081 -State Listen` -> backend `48081` PID `32276`, frontend `8081` PID `28264`; frontend `Invoke-WebRequest http://127.0.0.1:8081/` -> 200; backend health endpoint returned `UP`.
- RED: rerun real E2E after user-reported runtime recovery without explicit browser executable -> FAIL, expected reason: Playwright bundled Chromium executable missing at `E:\Int\DevCache\playwright-browsers\chromium_headless_shell-1223\...`; per E2E rules, no browser download was performed.
- RED: rerun real E2E with local Chrome executable -> FAIL, expected reason: current `48081` was running unrelated `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`, so `/admin-api/dcc/product-onboarding-requests/create` returned `404 请求地址不存在`.
- Runtime switch: confirmed `48081` owner PID `40500` belonged to `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`; stopped that same-profile runtime and started verified product-onboarding jar `E:\IntRuoyi\output\runtime\int_main\backend-runtime-control-20260803-121411-dcc-product-onboarding.jar` as PID `57996`.
- GREEN: post-switch runtime check -> backend `48081` PID `57996` command line points to the product-onboarding jar, backend health `UP`, frontend `8081` HTTP `200`.
- GREEN: rerun real Playwright E2E through `/mdm/project-code` with local Chrome -> PASS: `requestId=4`, `projectCodeId=258`, `productMasterId=332`, `projectCode=CODXONB03045351`; result JSON records `criticalNetworkFailures=[]`, `consoleErrors=[]`, `pageErrors=[]`.
- Experience consolidation check: runtime Jar mismatch was already covered by `docs/local-runtime.md#2026-07-24 隔离构建 Jar 加载门禁`; DCC product onboarding domain rules were already merged into `docs/database-rules.md#DCC 项目代码 MDM 产品建档绑定门禁`, so no new long-term experience document was created.
- Final runtime boundary check: after the successful product-onboarding E2E, shared `48081` was externally restored to PID `43876` running `backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`; health is `UP`, but current shared runtime no longer contains the product-onboarding controller. This task does not stop or replace that unrelated active runtime again.

## Milestone Status

- M1 任务文档、BDD 和门禁：completed。
- M2 现有代码定位：completed。
- M3 RED 测试：completed。
- M4 实现：completed。
- M5 GREEN/回归验证：completed。
- M6 收尾证据：completed，verification report、真实 E2E、隔离构建、cleanup 证据和恢复后运行态复核已补齐；最终 commit/push 因无关脏工作区阻塞。

## Implementation Summary

- Backend API: 新增 `/dcc/product-onboarding-requests/create` 和 `/{id}/approve`，使用 `dcc:project-code:create/update` 权限；建档申请只生成待审批单，审批通过后创建启用 DCC 项目代码并写入 `productMasterId`。
- Backend service: `DccProductOnboardingServiceImpl` 统一校验目标项目代码唯一性、待审批状态、MDM 产品有效性、14 位 DCC 产品编号；未选择 MDM 时审批阶段正式创建启用 MDM 产品。
- Backend bug fix: 审批通过的重复项目代码校验排除当前待审批申请自身，避免审批动作被自身 pending 记录阻塞；创建申请仍会拦截任何待审批重复。
- MDM integration: `MdmProductApi` 增加正式创建产品能力，审批和受控文件提交流程通过 `getEnabledDccProduct` 读取启用 MDM 产品，不用默认值或前端文本替代。
- Controlled file flow: DCC 项目代码绑定 MDM 产品时，受控文件提交保存正式 MDM `productMasterId`、DCC 产品编号和中文名；旧的未绑定项目代码仍保留项目代码/项目名作为历史兼容数据来源。
- Database: 基础 schema、迁移和 DCC 测试 fixture 增加 `dcc_project_code.product_master_id`、`dcc_product_onboarding_request`、待审批唯一约束和状态/产品/生成项目索引。
- Frontend: DCC 项目代码基础数据页新增“产品建档申请”入口、MDM 产品选择、产品信息和目标项目代码表单、提交申请和审批通过动作；错误不在本地吞掉，沿用请求异常。
- Experience: 将 DCC 项目代码与 MDM 产品建档绑定的正式来源门禁沉淀到 `docs/database-rules.md`，并补充 `docs/experience-index.md` 路由关键词。

## Verification Evidence

- PASS: `pnpm ts:check`。
- PASS: `mvn -pl yudao-module-dcc -am "-DskipTests" compile`。
- PASS: `node tests\e2e\dcc-project-code-product-onboarding-static.spec.js`。
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，106 tests。
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccBaseSchemaTest#mysqlSchemaShouldSupportProductOnboardingAndProjectMdmBinding" "-Dsurefire.failIfNoSpecifiedTests=false" test`，1 focused schema test。
- PASS: backend/database/frontend evidence validators。
- PASS: bug regression evidence validator (`validate_bug_regression.py`) -> Bug regression evidence is valid.
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest#approveRequest_shouldIgnoreCurrentPendingRequestWhenCheckingDuplicatePendingProject" "-Dsurefire.failIfNoSpecifiedTests=false" test`，1 focused regression。
- PASS: `mvn -pl yudao-module-dcc -am "-Dtest=DccProductOnboardingServiceImplTest,DccControlledFileWorkflowServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`，107 tests。
- PASS: isolated `mvn -pl yudao-server -am "-DskipTests" package`，runtime jar SHA256 `0BDB594204E0FF55CCEB2744D7566493643A27231C404D4424B50BA83051F02B`。
- PASS: real Playwright E2E through `/mdm/project-code` with local Chrome after runtime switch, `requestId=4`, `projectCodeId=258`, `productMasterId=332`, `projectCode=CODXONB03045351`。

## Blockers

- Standard restart remains blocked by unrelated dirty DCC NAS import code compile errors in `DccControlledFileNasTransferServiceImpl.java`; product onboarding verification used a clean detached build jar instead of modifying or reverting that unrelated work.
- Current shared runtime `48081` is healthy but owned by another task jar (`backend-runtime-control-20260803-115911-rrm-m6-pqc-skip-submitted.jar`); product-onboarding E2E already passed during the controlled temporary switch to the verified jar, and rerun would require another explicit runtime switch.
- 最终提交/推送未执行：当前分支已不再 ahead，但工作区含多项无关脏改动/未跟踪产物；按任务所有权边界，不能把无关任务改动打包进本任务提交或推送。
