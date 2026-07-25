# Execution Log

## User Intent

- 用户授权在本机 芋道源码/admin 身份下执行一次全量 E2E 验证，并要求融合后进行 E2E 验证、解决验证过程中遇到的问题。
- 2026-07-25 追加授权范围：从创建批次执行、填写、放行到追溯执行一次真实数据全 E2E 流程，解决流程中遇到的问题并记录在文档中。
- 用户提供的密码仅用于本次临时运行，不写入文档、日志、提交信息或证据文件。


## BDD Scenarios

- BDD: 批次执行全流程 -> Given 已授权本机 `芋道源码/admin` 身份和可追踪测试数据 When 用户通过真实前端创建批次执行、填写记录、提交放行并进入追溯 Then 页面与后端状态应展示同一批次执行链路且无 API-only 或 mock 替代。
- BDD: 失败根因修复 -> Given 全流程任一真实页面步骤失败 When 失败属于当前融合实现问题 Then 先记录可复现失败与预期，再补回归测试、最小修复并用真实路径复验。
## Rule And Skill Gates

- 使用技能：`playwright`，用于真实浏览器路径验证。
- 使用技能：`quality-assurance-test-suite`，用于验证矩阵、证据和阻塞项归档。
- 已读取：`docs/task-closeout-rules.md`。
- 已读取：`docs/e2e-rules.md`。
- 已读取：`docs/login-access.md`。
- 已读取：`docs/local-runtime.md`。
- 已读取：`docs/worktree-restrictions.md`。
- 已读取：`docs/branch-runtime-ports.md`。
- 已读取：`docs/powershell-encoding.md`。
- 已读取：`docs/powershell-memory.md`。

## Initial Git State

- `git status --short --branch` 显示 `int_main` 相对 `origin/int_main` ahead 2。
- 开始本任务前已存在非本任务脏改动：
  - `IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordCellRuleSupport.java`
  - `IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/batchrecordreport/MesProBatchRecordCellRuleSupportTest.java`
  - `IntRuoyiFronted/tests/e2e/edhr-batch-execution-real-flow.e2e.js`
  - `docs/e2e-rules.md`
  - `docs/experience-index.md`
  - `doc/tasks/20260725-edhr-route-form-filler-e2e/`
- 本任务不会把上述非任务自有变更纳入验证结论、修复或提交边界，除非后续证明它们是当前 E2E 阻塞根因并获得明确处理依据。

## Milestone Evidence

### 1. Task Setup

- Status: in_progress
- Evidence: 创建 `task.md` 与 `execution-log.md`，建立本次 E2E 验证边界。
- GREEN: experience-preflight -> PASS，已读取 `docs/experience-index.md` 并命中真实 E2E、登录端口、任务专用证据、eDHR 只读与填写人显示门禁。
- NOTE: `apply_patch` 更新本任务文档时被 sandbox 读 ACL 拦截，改用显式 UTF-8 PowerShell 写入并立即复核。
### 2. Runtime And Coverage Gate Preflight

- GREEN: local runtime ports -> PASS，`8081` 为 `E:\IntRuoyi\IntRuoyiFronted` Vite，`48081` 为 `E:\IntRuoyi\IntRuoyiBackend` 后端 Jar。
- GREEN: frontend/backend reachability -> PASS，登录页 HTTP 200，后端 health `UP`。
- BDD: eDHR release coverage gate stays current -> Given eDHR/批记录源码与真实 E2E 脚本继续演进 When 执行 release 覆盖门禁 Then 矩阵必须绑定存在的源码、真实脚本 token 和持久证据，不得因过期路径阻塞后续真实 E2E。
- RED: `node scripts/edhr-release-e2e-coverage-gate.mjs --check --report ..\doc\tasks\20260725-full-e2e-admin-validation\edhr-release-check-report.json` -> FAIL，原因包括过期 open-or-create token、旧批次执行端点 token、缺失历史 evidence 路径、旧模板页面路径和非 release-scope eDHR 源码被误判 uncovered。
### 3. E2E Script Repair And Admin Readonly Validation

- BDD: 管理员只读预览目标动态发现 -> Given 历史固定批次/任务 ID 可能过期 When 运行管理员只读预览 E2E Then 脚本必须从授权租户本机数据库发现真实未开始批记录任务，凭据来自环境变量，证据写入当前任务目录。
- RED: `EDHR_ADMIN_PREVIEW_PASSWORD=<redacted> node tests\e2e\edhr-batch-admin-preview-runtime-fix.e2e.js` -> FAIL，旧脚本等待固定 `batchExecutionId=900000000480/taskId=3041` 预览响应超时，且源码含历史默认密码与旧证据目录。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\edhr-batch-admin-preview-runtime-fix.e2e.js` -> PASS。
- GREEN: `EDHR_ADMIN_PREVIEW_PASSWORD=<redacted> node tests\e2e\edhr-batch-admin-preview-runtime-fix.e2e.js` -> PASS，动态命中真实未开始任务并生成 `admin-preview-e2e-output`。
- BDD: 普通工序计数不包含特殊节点 -> Given 批次详情页面同时渲染普通工序和特殊节点 When 工序命名 E2E 对比接口工序分组 Then 选择器必须排除 `.edhr-batch-detail__special-process-task-group`，只比较普通工序。
- RED: `EDHR_PROCESS_ITEM_E2E_PASSWORD=<redacted> node tests\e2e\edhr-batch-process-item-uniform-name-real.e2e.js` -> FAIL，页面普通工序计数误把特殊节点计入，18/14 不一致。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\edhr-batch-process-item-uniform-name-real.e2e.js` -> PASS。
- GREEN: `EDHR_PROCESS_ITEM_E2E_PASSWORD=<redacted> node tests\e2e\edhr-batch-process-item-uniform-name-real.e2e.js` -> PASS，19 个工序/节点卡片高度和普通工序名称显示通过。
- BDD: 官方登录前置存在且跟随当前页面文案 -> Given 多个 E2E 依赖 `scripts/preflight/login-preflight.mjs` When 工作区缺失该脚本或目标文案过期 Then 必须补正式登录前置并使用当前真实页面可见文本，不得跳过 preflight。
- RED: `EDHR_ASSIST_FILL_ADMIN_PASSWORD=<redacted> node tests\e2e\edhr-assist-fill-mode-admin-readonly.e2e.js` -> FAIL，缺少 `scripts/preflight/login-preflight.mjs`；补脚本后旧目标文本 `执行列表` 超时。
- GREEN: `node --check scripts\preflight\login-preflight.mjs` -> PASS。
- GREEN: `PLAYWRIGHT_CHROMIUM_EXECUTABLE_PATH=<chrome> node scripts\preflight\login-preflight.mjs --base-url http://localhost:8081 --tenant 芋道源码 --username admin --password <redacted> --target-path /mes/pro/feedback/edhr-batch-execution --target-text 批次 --timeout 90000` -> PASS。
- BLOCKED: `EDHR_ASSIST_FILL_ADMIN_PASSWORD=<redacted> node tests\e2e\edhr-assist-fill-mode-admin-readonly.e2e.js` -> BLOCKED，preflight 已通过，但旧执行列表路径未发现 admin 可读执行行；未改用 API-only，也未扩大为写入型测试。
- BDD: 表单日志时间格式跟随页面组件 -> Given 表单日志页面使用 `formatEdhrDateTime` When 验证填写时间 Then E2E 必须断言 `YYYY-MM-DD HH:mm:ss` 且不显示 ISO `T` 分隔符，不能硬编码历史中文年月日格式。
- RED: `EDHR_FORM_FILL_LOG_E2E_PASSWORD=<redacted> node tests\e2e\edhr-form-fill-log-menu-time-real.e2e.js` -> FAIL，旧脚本要求固定历史中文年月日时间，当前页面正式格式为 `YYYY-MM-DD HH:mm:ss`。
- GREEN: `node --check IntRuoyiFronted\tests\e2e\edhr-form-fill-log-menu-time-real.e2e.js` -> PASS。
- GREEN: `EDHR_FORM_FILL_LOG_E2E_PASSWORD=<redacted> node tests\e2e\edhr-form-fill-log-menu-time-real.e2e.js` -> PASS，证据写入 `form-fill-log-e2e-output`。

### 4. Final Admin E2E And Static Gate Results

- GREEN: `EDHR_BATCH_E2E_TASK_ID=20260725-full-e2e-admin-validation node tests\e2e\edhr-batch-execution-real-flow.e2e.js` -> PASS，真实批次执行主路径完成，证据 `edhr-batch-execution-real-e2e-final.md`。
- GREEN: `EDHR_COMPANION_E2E_READONLY_ADMIN=1 EDHR_COMPANION_E2E_STRUCTURAL_ONLY=1 EDHR_COMPANION_E2E_PASSWORD=<redacted> node tests\e2e\edhr-batch-process-companion-forms-real.e2e.js` -> PASS，伴随单据结构只读验证通过。
- GREEN: `EDHR_GOLDEN_FINGER_PASSWORD=<redacted> node tests\e2e\edhr-golden-finger-admin-permission-real.e2e.js` -> PASS，金手指权限只读验证通过。
- BLOCKED: `EDHR_FILL_WORKSPACE_E2E_READONLY_ADMIN=1 EDHR_FILL_WORKSPACE_E2E_PASSWORD=<redacted> node tests\e2e\edhr-fill-workspace-real.e2e.js` -> BLOCKED，admin-only 范围下不应将历史 execution 直连填写页作为通过；当前活动填写需正式 openTask，历史执行需 tracking 只读路径。
- BLOCKED: 多用户/写入型 eDHR 发布链路脚本 -> BLOCKED，本次仅授权 `芋道源码/admin`；写入型或多用户脚本需要测试租户、多账号凭据、任务自有数据和清理责任。
- GREEN: `node scripts\edhr-release-e2e-coverage-contract.test.mjs` -> PASS，12/12。
- GREEN: `node scripts\edhr-batch-version-phase1-contract.test.mjs` -> PASS。
- GREEN: `node tests\e2e\edhr-full-chain-api-response-static.spec.js` -> PASS。
- GREEN: `node tests\e2e\edhr-full-chain-evidence-pack-static.spec.js` -> PASS。
- GREEN: `node scripts/edhr-release-e2e-coverage-gate.mjs --check --report ..\doc\tasks\20260725-full-e2e-admin-validation\edhr-release-check-report-final.json` -> PASS，features=14, checkScripts=11, syntaxFiles=11。
- GREEN: `powershell -ExecutionPolicy Bypass -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS。
- GREEN: `git diff --check` -> PASS，仅 CRLF 工作区提示，无空白错误。
- GREEN: edited-script-secret-scan -> PASS，本次触达脚本未保留明文密码或默认密码表达式。
- NOTE: 多个普通 shell/Node 只读命令被 Windows sandbox ACL 拦截，已按 PowerShell/任务日志规则使用窄范围 `require_escalated` 复跑并记录关键结果。

### 5. Closeout Readiness

- Status: ready_for_closeout。
- Verification summary: 管理员可安全执行的 eDHR/记录本相关真实前端 E2E 与静态覆盖门禁已完成；写入型、多用户或旧直连填写页脚本按项目门禁记录为 BLOCKED，未用 mock、API-only 或默认成功替代。
- Remaining blocker: 当前工作区包含大量本任务开始前和并行线程留下的 staged/unstaged/untracked 改动；已执行 task-closeout-cleanup apply；未执行提交或推送，避免把非本任务变更混入当前收尾。
### 6. Cleanup Preview / Apply

- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-full-e2e-admin-validation --mode preview` -> PASS，status=ready，blocked=<none>，warnings=<none>；正式 E2E 证据加入 `Cleanup Keep` 后重新 preview。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260725-full-e2e-admin-validation --mode apply` -> PASS，删除本任务临时 full-chain/admin 初始失败产物与旧中间报告，保留 task.md、execution-log.md、verification-report.md、最终批次执行证据、管理员预览证据、表单日志证据和最终覆盖报告。
- BLOCKER: commit/push closeout -> 当前 `int_main` 工作区仍包含多项本任务开始前和并行任务的 staged/unstaged/untracked 改动；为避免混入非本任务变更，本轮未执行提交或推送，任务状态保持 `ready_for_closeout`。
### 7. Final Closeout Check

- GREEN: cleanup-lock-owner-check -> PASS，未发现持有当前任务日志文件的运行进程。
- GREEN: task-closeout apply -> PASS，删除 runtime 残留日志。
- GREEN: residual-artifact-cleanup -> PASS，已在路径归属校验后删除当前任务临时 artifacts/full-chain-admin/ 目录。
- GREEN: cleanup-preview-final -> PASS，delete=<none>，blocked=<none>，warnings=<none>。
- GREEN: final-secret-scan -> PASS，任务证据、触达脚本和经验文档未保留明文密码或默认密码表达式。
- GREEN: git diff --check -> PASS，仅 Git CRLF 提示，无空白错误。
- GREEN: branch-runtime-port-guard -> PASS。
- BLOCKER: commit/push closeout -> git status --short --branch 显示 int_main...origin/int_main [ahead 3]，且仍有非本任务改动/未跟踪目录；为避免混入并发任务，本任务不提交、不推送，保持 ready_for_closeout。

### 8. Authorized Full Write E2E Continuation

- Status: in_progress.
- NOTE: 用户已追加授权使用 `芋道源码/admin` 执行创建批次、填写、放行、追溯全链路真实写入 E2E；凭据仍只通过临时环境变量使用，不写入日志或证据。
- NOTE: 本轮再次尝试 `apply_patch` 更新任务文档时被 Windows sandbox ACL 拦截，改用窄范围 Node UTF-8 写入并立即复核。
- BDD: FormCenter 共享/动态路线表单可完成真实页面提交 -> Given 批次路线任务包含 `formCenterInstanceId/formTemplateId` 且没有 `executionId` When 全链路 E2E 从待办/批次详情处理该 ROUTE_FORM Then 脚本必须通过批次详情抽屉保存草稿并提交 FormCenter 实例，不得把它当作普通批记录 execution 强制断言。
- RED: `node tests\e2e\edhr-full-chain-evidence-pack-static.spec.js` -> FAIL，原因：完整演练缺少 `isFormCenterRouteTask` / `processRouteFormCenterTask`，无法覆盖共享/动态表单任务。
- RED: `EDHR_FULL_E2E_ADMIN_SINGLE_ACTOR=1 ... node tests\e2e\edhr-full-chain-multi-user-real-flow.e2e.js` -> FAIL，真实批次任务进入 LOSS_REPORT/FormCenter 共享表单后 `openFillTaskFromBoard` 仍要求 `executionId`，实际 openTask 返回 `formCenterInstanceId`。

- GREEN: `node --check tests\e2e\edhr-full-chain-multi-user-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\edhr-full-chain-evidence-pack-static.spec.js` -> PASS，FormCenter 动态/共享表单任务静态合同已覆盖。
- GREEN: `node tests\e2e\edhr-full-chain-api-response-static.spec.js` -> PASS。

- RED: `EDHR_FULL_E2E_ADMIN_SINGLE_ACTOR=1 ... node tests\e2e\edhr-full-chain-multi-user-real-flow.e2e.js` -> FAIL，FormCenter 静态修复后进入普通路线任务，但 `processRouteTask` 在已加载详情后立即重复 reload，同批次详情响应等待超时。
- FIX: 移除普通路线任务入口的重复批次详情 reload，直接使用循环刚从真实详情页加载出的待办任务；保留工作台处理必须返回 executionId、填写页加载、提交和审批断言。

- GREEN: `node --check tests\e2e\edhr-full-chain-multi-user-real-flow.e2e.js`; `node tests\e2e\edhr-full-chain-evidence-pack-static.spec.js`; `node tests\e2e\edhr-full-chain-api-response-static.spec.js` -> PASS，普通路线任务重复 reload 修复后静态门禁通过。

- BDD: FormCenter 路线表单提交完成工作任务并推进下一工序 -> Given 动态/共享路线表单通过 FormCenter DIRECT 策略提交 When 业务生效 executor 执行 Then 对应 eDHR FILL 工作任务必须 DONE，并复用正式 advance gate 创建下一可填写任务。

- RED: `mvn -pl yudao-module-mes -Dtest=MesProEdhrRouteFormFillEffectExecutorTest test` -> FAIL，包含当前新增合同缺少 `completeRouteFormFillAndCreateNextFill(long,long)`，同时存在非本任务 GoldenFinger bulk void / route projection 测试编译缺符号。
- GREEN: `mvn -pl yudao-module-mes -am -DskipTests compile` -> PASS，FormCenter 路线表单工作任务完成实现的主源码编译通过；定向单测运行仍受非本任务测试编译缺符号阻塞。

- FIX: 后端打包被非本任务 GoldenFinger bulk void 源码缺 import 阻塞；VO 文件已存在，补齐 service/controller 显式 import，未新增 fallback。
