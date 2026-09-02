# Execution Log: Active Order Latest Version Upgrade Restart Code

BDD: Active order upgrade entry -> Given 用户打开活跃订单列表, When 订单处于可升级状态, Then 行操作展示“升级”按钮并进入版本升级重启确认流程。

BDD: No per-version selection -> Given 用户点击活跃订单升级, When 前端展示确认弹窗, Then 前端只展示后端返回的全部最新正式版本摘要，不展示逐项版本选择控件。

BDD: Backend upgrade contract -> Given 前端提交活跃订单版本升级申请, When 后端接收请求, Then 接口必须要求活跃订单 ID、升级原因、确认整单重启和幂等键，并返回申请编号/冻结状态，不得模拟新订单创建成功。

BDD: Version upgrade request persistence -> Given 活跃订单存在可升级的路线或 QA 最新正式版本, When 生产组长提交升级重启, Then 后端创建待审批申请、冻结旧活跃订单、保存当前/目标版本快照和快照哈希。

BDD: Version upgrade submit idempotency -> Given 同一活跃订单已经使用同一幂等键提交升级申请, When 前端重复提交, Then 后端返回既有申请，不重复冻结旧订单。

BDD: Approved version upgrade applies restart -> Given 升级申请处于 PENDING_APPROVAL 且旧订单已冻结, When 真实审批通过后调用生效服务, Then 系统作废旧批次、取消旧批次待办、移除旧活跃订单，并按全部最新正式版本强制创建新的活跃订单。

BDD: Approved upgrade must not recover removed history -> Given 旧活跃订单因版本升级被移除, When 生效服务重新加入活跃订单, Then 入池逻辑必须跳过历史恢复/复用，创建新活跃订单行。

RED: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs -> FAIL, frontend API and active-order upgrade entry were missing.

RED: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> FAIL, backend version-upgrade service contract was missing.

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs -> PASS.

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> PASS.

RED: mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile -> FAIL, MesProWorkOrderDO work order code getter was incorrectly referenced as getWorkorderCode().

GREEN: mvn -f IntRuoyiBackend\pom.xml -rf :yudao-module-mes -DskipTests compile -> PASS.

BLOCKED: pnpm -C IntRuoyiFronted ts:check -> FAIL, worktree frontend dependencies are incomplete and cross-env is missing from node_modules.

GREEN: pnpm -C IntRuoyiFronted install --frozen-lockfile -> PASS, restored worktree-local frontend dependencies without changing pnpm-lock.yaml.

GREEN: pnpm -C IntRuoyiFronted ts:check -> PASS.

RED: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> FAIL, version-upgrade request DO/Mapper/migration and freeze contract were missing.

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> PASS, pending approval request persistence and old-order freeze contract are present.

GREEN: python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q -> PASS, 3 tests.

RED: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> FAIL, approved apply result/service and forced-new active-order contract were missing.

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> PASS, approved apply service contract now covers old batch void, old task cancellation, pending-order removal, forced-new active-order creation, and request markApplied.

GREEN: mvn -f IntRuoyiBackend\pom.xml -rf :yudao-module-mes -DskipTests compile -> PASS after adding `MesProEdhrWorkTaskService` import.

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs -> PASS.

GREEN: pnpm -C IntRuoyiFronted ts:check -> PASS.

## Work Log

- 2026-09-02: Added backend active-order version-upgrade preview/submit API contract and fail-fast approval schema blocker.
- 2026-09-02: Added frontend active-order row “升级” button, version-upgrade confirmation dialog, and API calls.
- 2026-09-02: Kept submit from returning mock approval success; current submit blocks until approval persistence schema/policy is implemented.
- 2026-09-02: Fixed the frontend dependency blocker in the worktree and verified `ts:check` passes.
- 2026-09-02: Replaced submit-time approval-schema fail-fast with durable `PENDING_APPROVAL` request persistence, old active-order freeze to `VERSION_UPGRADE_PENDING`, frozen current/target JSON snapshots, SHA-256 snapshot hash, duplicate ongoing request blocker, and same-key idempotent return.
- 2026-09-02: Added additive MySQL migration and SQL contract tests for `mes_pro_process_pool_active_order_version_upgrade_request`.
- 2026-09-02: Added `applyApprovedUpgrade` service path for real approval callbacks: locks request/source order, voids the active-context eDHR batch, cancels old batch tasks, invalidates old report allocations, removes the pending old active order, forces a new latest-version active order, marks the request `APPLIED`, and records maintenance audit.

BDD: BPM approval callback closes version upgrade -> Given 活跃订单升级申请已冻结旧订单并发起统一业务审批, When BPM 审批通过, Then 业务审批 Executor 必须调用升级重开生效服务，作废旧批次并创建最新版本新活跃订单。

BDD: Rejected upgrade releases old order -> Given 活跃订单升级申请处于 BPM 待审批且旧订单已冻结, When BPM 审批驳回或取消, Then 系统必须释放旧活跃订单冻结，不得让生产订单永久停在 VERSION_UPGRADE_PENDING。

RED: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> FAIL, backend must register a business approval effect executor for active-order version upgrade.

RED: python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q -> FAIL, migration did not seed active-order version-upgrade business approval policy.

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> PASS, BPM orchestrator submit, business approval executor, pending process binding, approved apply, rejected/cancelled freeze release, registry, policy scope, and native approval summary contract are present.

GREEN: python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q -> PASS, 4 tests.

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs -> PASS.

GREEN: pnpm -C IntRuoyiFronted ts:check -> PASS.

GREEN: mvn -f IntRuoyiBackend\pom.xml -pl yudao-module-bpm,yudao-module-mes -am -DskipTests compile -> PASS.

SUPERSEDED BLOCKER: live E2E -> originally NOT RUN before runtime DB prerequisites and authorization were available; superseded by 2026-09-03 real approval/final-state E2E evidence below.

## Work Log Update

- 2026-09-02: Wired active-order version upgrade submit into `BusinessApprovalOrchestrator`; submit now starts BPM and stores the process instance id through the effect executor mark-pending callback.
- 2026-09-02: Added `MesTeamLeaderActiveOrderVersionUpgradeBusinessApprovalEffectExecutor` so BPM approve applies restart, while reject/cancel releases `VERSION_UPGRADE_PENDING` back to `ACTIVE`.
- 2026-09-02: Added business approval registry/policy scope/native approval summary support for `MES_ACTIVE_ORDER_VERSION_UPGRADE_RESTART`.
- 2026-09-02: Extended SQL contract and migration policy seed; migration fails fast if the active-order BPM process definition is missing instead of silently creating no policy.

BDD: Full version upgrade restart E2E terminal state -> Given 活跃订单升级审批已由真实审批页面通过, When 生产组长重新进入活跃订单池并打开新订单详情, Then 旧活跃订单不得继续出现在活跃池，新活跃订单必须按目标最新正式版本进入 ACTIVE 并可从头执行。

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-approve-real.e2e.cjs -> PASS, Playwright 登录真实前端 `8093`，从审批中心待办打开流程实例 `7f9ca694-a6da-11f1-a6b9-00155d07b6dd`，通过电子签名审核，随后确认新活跃订单 `1009200001` 出现在活跃订单池，工艺路线版本为 `742 / V12`，状态为 `ACTIVE/ACTIVE`。

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS, Playwright 登录真实前端 `8093`，打开生产组长活跃订单池，确认旧活跃订单 `45` 不再出现在活跃池，新活跃订单 `1009200001` 可见并可打开“工序提交详情”。证据文件：`doc/tasks/20260902-active-order-latest-version-upgrade-restart-code/e2e-artifacts/active-order-version-upgrade-final-state-real-result.json`。

GREEN: runtime DB readonly verification -> PASS, `mes_pro_process_pool_active_order_version_upgrade_request.id=1` 为 `APPLIED/APPROVED/APPLIED`，旧活跃订单 `45` 为 `REMOVED/VERSION_UPGRADED`，新活跃订单 `1009200001` 为 `ACTIVE/ACTIVE` 且 `route_version_id=742`，Flowable 历史流程实例 `7f9ca694-a6da-11f1-a6b9-00155d07b6dd` 已有 `END_TIME_`。

BLOCKED: fresh-submit E2E with visible active order -> current visible candidate `150` returned preview blocker `全部受控对象已是最新正式版本，无需发起版本升级重启`; existing active order `1009200000` has version difference but is filtered out by active-order list progress validation, so it cannot be used for a fresh Playwright submit path without creating or repairing a task-owned visible fixture.

GREEN: node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-submit-real.e2e.cjs -> PASS after removing hardcoded `V3/V12` assertions and validating changed version lines from the preview response.

GREEN: node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS.

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-entry-static.spec.cjs -> PASS.

GREEN: node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-version-upgrade-code-static.spec.cjs -> PASS.

GREEN: python -X utf8 -m pytest IntRuoyiBackend\script\tests\test_mes_active_order_version_upgrade_request_sql.py -q -> PASS, 7 tests.

## Work Log Update 2026-09-03

- 2026-09-03: Reinterpreted the user's E2E scope as complete terminal-state verification, not stopping at old-order freeze.
- 2026-09-03: Verified worktree runtime `8093/48093` is active and backend health is UP; did not restart `int_main`.
- 2026-09-03: Confirmed runtime database contains request table, approval policy, and Flowable process definition key `mes-active-order-version-upgrade-v1`.
- 2026-09-03: Added `active-order-version-upgrade-final-state-real.e2e.cjs` to verify the completed chain from the real frontend after approval.
- 2026-09-03: Fixed the submit E2E script to use preview response version lines instead of hardcoded version text.

GREEN: task_closeout preview -> BLOCKED for closeout only, preserved core task records and e2e-artifacts; did not apply cleanup because main worktree is dirty and branch cannot be fast-forward merged into int_main from current state.

## Work Log Update 2026-09-03 / Full Chain E2E Rerun

- 2026-09-03: 用户明确要求“E2E 验证完整链路，不止到旧订单进入冻结”。本轮在 worktree `D:\IntRuoyiWorktree\20260902-active-order-latest-version-upgrade-restart-docs` 的真实运行态 `8093/48093` 重跑终态 E2E，未重启或修改 `int_main`。
- 2026-09-03: 运行态预检确认前端 `8093` 返回 HTTP 200，后端 `48093` health 为 `UP`。

GREEN: node tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS, Playwright 使用真实前端登录后进入生产组长活跃订单池，验证旧活跃订单 `45` 已不在活跃池，新活跃订单 `1009200001` 可见、状态为 `ACTIVE/ACTIVE`、路线版本为 `742 / V12`，并能打开详情弹窗。证据文件：`doc/tasks/20260902-active-order-latest-version-upgrade-restart-code/e2e-artifacts/active-order-version-upgrade-final-state-real-result.json`。

GREEN: runtime DB readonly verification rerun -> PASS, `mes_pro_process_pool_active_order_version_upgrade_request.id=1` 为 `APPLIED/APPROVED/APPLIED` 且 `target_active_order_id=1009200001`；旧活跃订单 `45` 为 `REMOVED/VERSION_UPGRADED`；新活跃订单 `1009200001` 为 `ACTIVE/ACTIVE` 且 `route_version_id=742`；路线版本 `633=V3/SUPERSEDED`、`742=V12/ACTIVE`；Flowable 历史流程实例 `7f9ca694-a6da-11f1-a6b9-00155d07b6dd` 已有 `END_TIME_`。

## Work Log Update 2026-09-03 / Frontend-Only E2E Gate

- 2026-09-03: 按用户目标“E2E 验证只能通过前端实际操作，不能直接调用接口”，收紧 `active-order-version-upgrade-final-state-real.e2e.cjs`：不再读取活跃订单列表/详情接口响应 JSON 作为断言依据，只保留页面自然触发请求的 HTTP 状态记录；断言改为真实页面 DOM 可见内容，包括活跃池表格行、旧活跃订单 ID 缺失、新活跃订单 ID/生产订单号/版本号/正式订单文本和详情弹窗内容。

GREEN: node --check tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS.

GREEN: node tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS, Playwright 通过真实前端登录、进入生产组长活跃订单池、点击详情按钮完成验证；证据模式为 `FRONTEND_DOM_ONLY`。可见行包含 `1009200001`、`CODX-PQC-20260807-SP-WO-05`、`V12`、`正式订单`、生产/检验进度 `0%`；旧活跃订单 `45` 在当前活跃池可见表格中不存在；详情弹窗显示“订单 CODX-PQC-20260807-SP-WO-05 · 工序提交详情”且工序数为 `15`。

GREEN: static frontend-only E2E gate -> PASS, `active-order-version-upgrade-final-state-real.e2e.cjs` 未命中 `fetch(`、`request.get`、`APIRequest`、`axios`、`response.json`、`.json()`、`docker exec`、`mysql`、`Invoke-RestMethod` 或 `Invoke-WebRequest`；E2E 只通过 Playwright 前端页面操作和 DOM 可见内容完成断言。

## Work Log Update 2026-09-03 / Complete E2E Confirmation

- 2026-09-03: 针对用户再次强调“完整链路不止冻结”，复核既有 `approve-real` 与 `final-state-real` 证据：审批中心真实页面已通过流程实例 `7f9ca694-a6da-11f1-a6b9-00155d07b6dd`，并触发旧订单移出、新订单入池。
- 2026-09-03: 修复终态 E2E 脚本的网络响应等待顺序，避免 `waitForResponse` 超时 Promise 在页面 DOM 验证前被 Node 24 作为未处理异常中断；该修复不改变业务断言口径，仍然只以真实前端 DOM 作为验收依据。

GREEN: node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS, Playwright 登录真实前端 `8093`，进入生产组长活跃订单池，确认旧活跃订单 `45` 不在活跃池，新活跃订单 `1009200001` 可见，行内容包含 `CODX-PQC-20260807-SP-WO-05`、`V12`、`正式订单`、生产/检验进度 `0%`，并可打开详情弹窗查看 `15` 道工序。

GREEN: node --check IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs -> PASS.

GREEN: static frontend-only E2E gate rerun -> PASS, `active-order-version-upgrade-final-state-real.e2e.cjs` 未命中直接 API、DB 或 HTTP 客户端断言模式；完整终态仍由 Playwright 页面操作完成。

WARNING: final-state E2E 期间页面后台审批待办角标加载出现 `系统异常` console 错误，但验收链路本身的登录、活跃订单池、详情弹窗和页面触发的活跃订单接口均成功；该警告未作为本次升级重启链路阻塞项处理。

## Work Log Update 2026-09-03 / Feature Branch Commit

- 2026-09-03: 已在 worktree 分支 codex/20260902-active-order-latest-version-upgrade-restart-docs 提交实现、测试、文档与 E2E 证据；实现提交：$full。
- 2026-09-03: 提交前通过 ranch-runtime-port-guard.ps1、前端静态合同、后端静态合同、SQL 合同、git diff --cached --check，并强制纳入任务保留的 E2E 证据图片与 JSON。

## Work Log Update 2026-09-03 / Fresh Full-Chain Rerun Boundary

- 2026-09-03 01:55:28 +08:00: 在 worktree `D:\IntRuoyiWorktree\20260902-active-order-latest-version-upgrade-restart-docs`、真实运行态 `8093/48093` 继续验证用户要求的完整链路。
- 修复 E2E 脚本分页定位问题：`submit-real`、`approve-real` 不再只查活跃订单池第一页；`final-state-real` 改为扫描活跃订单池全部分页后再验证旧单缺席和新单详情。
- 重新运行终态真实 E2E：`node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-final-state-real.e2e.cjs` -> PASS。Playwright 从真实前端登录，扫描活跃池可见 ID `[150,348,396,1009200001]`，确认旧活跃订单 `45` 不在任一分页，新活跃订单 `1009200001` 可见并可打开详情。
- 重新运行定向静态验证：`node --check` 三个真实 E2E 脚本、前端入口静态合同、后端静态合同、SQL 合同 `pytest` -> PASS，SQL 合同 7 tests。
- Fresh submit rerun boundary: `node IntRuoyiFronted\tests\e2e\active-order-version-upgrade-submit-real.e2e.cjs` 使用任务自有旧版本样本 `1009200000 / CODX-AOUP-20260902205106` -> BLOCKED。真实页面活跃订单池全部分页只渲染 `150/348/396/1009200001`，该样本未进入 DOM，无法从真实前端点击“升级”重新发起。
- 只读数据库核对显示 `1009200000` 是旧路线 `633 / V3`，但其 `active_order.work_order_id=1009200000`，而进度快照/PQC 任务绑定 `work_order_id=980032`，因此不满足活跃订单列表读取所需的一致进度合同；本轮未进行数据库写入修复，也未修改共享路线/QA 基础版本来制造差异。

BLOCKED: fresh continuous submit -> approve -> final rerun requires a new or repaired task-owned visible old-version active-order fixture. Existing approved chain terminal state remains PASS, but this rerun did not create a fresh approval instance because the only old-version candidate is not visible through the real active-order page.
