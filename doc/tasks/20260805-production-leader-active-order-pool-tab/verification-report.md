# Verification Report

## Result

本轮已修复“加入活跃订单池提示 `请求参数不正确:不能为null`”回归，并补齐截图中的“只输入完整订单号但未点候选”路径：提交前会按 `workOrderCode` 精确解析候选，命中后才发送 `workOrderId`。活跃订单聚焦静态合同、`pnpm ts:check` 和目标 `git diff --check` 已通过；2026-08-06 17:18 使用 `芋道源码/admin` 真实页面复跑生产组长页签，新增请求体为 `{"workOrderId":925868}`，旧 null 参数错误未再出现。当前任务仍不得标记 completed：本机无完整 QA 规程覆盖的可新增候选，真实新增成功被正式 PQC 前置条件阻塞。

## Acceptance

- AC1 PASS：新增活跃订单弹窗只保留“订单号”远程可搜索 `el-select`，候选展示生产工单编号并绑定 `workOrderId`。
- AC2 PASS：前端 `addTeamLeaderActiveOrder` 请求类型和提交 payload 只包含 `workOrderId`。
- AC3 PASS：后端 `POST /active-order/add` 只接收 `workOrderId`，并从唯一有效排产解析正式 `routeId` 和 `routeVersionId`。
- AC4 PASS：候选接口 `GET /active-order/candidates?keyword=...` 使用维护权限并返回已确认生产工单候选。
- AC5 PASS：无有效排产、多条有效排产或排产缺正式路线/版本时后端 fail fast，不创建活跃订单、工序快照或 PQC 任务。
- AC6 PASS：调拨关联输入已从新增动作拆除；既有调拨追溯仍为只读展示。
- AC7 PASS：未点击真实订单号候选、清空、搜索失败或候选刷新失配时，前端抛 `请选择订单号` 并阻止 `/active-order/add` 写请求。
- AC8 PASS：只输入完整订单号且精确命中候选时，前端提交前自动解析 `workOrderId`，避免后端收到 `null`。
- AC9 BLOCKED：完整真实新增成功要求本机存在已确认工单、唯一有效排产、启用工序、计划日期和已发布 QA 规程完整覆盖；当前只读统计显示满足全部条件的候选数为 0。

## Verification

- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，旧实现缺少候选 change/clear 事件和候选级提交校验。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，旧实现提交仍直接读取 `activeOrderForm.workOrderId`。
- RED: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> FAIL，第一轮修复仍缺少按完整订单号精确解析候选的提交前路径。
- RED: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> FAIL，第一轮修复仍要求点击候选，不能覆盖截图中的完整输入直接提交。
- GREEN: `node tests/e2e/production-leader-active-order-pool-tab-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/mes/pro/processpool/TeamLeaderWorkbenchPage.vue IntRuoyiFronted/tests/e2e/production-leader-active-order-pool-tab-static.spec.js IntRuoyiFronted/tests/e2e/team-leader-workbench-static.spec.cjs` -> PASS，仅 CRLF working-copy 提示。
- PRIOR PASS: `node --check tests/e2e/role-requirement-matrix-real-flow.e2e.js` -> PASS。
- PRIOR PASS: `node --check tests/e2e/team-leader-workbench-real-flow.e2e.js` -> PASS。
- PRIOR PASS: `mvn -pl yudao-module-mes -am "-Dtest=MesTeamLeaderActiveOrderServiceTest,MesProcessPoolTeamLeaderControllerTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，Tests run: 25, Failures: 0, Errors: 0, Skipped: 0。
- BLOCKED: `node tests/e2e/role-requirement-matrix-preflight-static.spec.cjs` -> FAIL，当前缺少 PQC 过程检验汇集稳定选择器 `data-pqc-process-inspection-aggregation`。
- BLOCKED: `node tests/e2e/mes-process-pool-team-leader-static.spec.js` -> FAIL，当前 PQC 组长切换后提交看板多维筛选重置链路合同失败。
- GREEN: `pnpm e2e:team-leader-workbench:real:check` -> PASS。
- BLOCKED: 注入 `TLW_FRONTEND_URL=http://127.0.0.1:8081` 与 `TLW_BACKEND_URL=http://127.0.0.1:48081` 后运行 `pnpm e2e:team-leader-workbench:real` -> non-zero，`IntRuoyiFronted/test-results/team-leader-workbench-real-flow/result.json` 记录 `status=BLOCKED`，原因是缺少真实写入型 E2E 前置条件。
- REAL E2E BLOCKED: `ACTIVE_ORDER_E2E_BASE_URL=http://127.0.0.1:8081 ACTIVE_ORDER_E2E_WORK_ORDER_CODE=881MO093613 node tests/e2e/production-leader-active-order-focused.e2e.js` -> non-zero；Playwright 真实登录 `芋道源码/admin`，进入生产组长页签，远程下拉选择候选 `881MO093613` 后提交。
- PAYLOAD PASS: `IntRuoyiFronted/test-results/production-leader-active-order-focused/result.json` 记录候选 `{workOrderId: 925868, workOrderCode: "881MO093613"}`，新增请求体仅为 `{"workOrderId":925868}`，无 `routeId`、`routeVersionId`、`transferIds` 或 null `workOrderId`。
- BUSINESS BLOCKED: 聚焦 E2E 进入后端正式服务后返回业务码 `1040506111`，消息为 `PQC 检验任务生成前置条件不满足：缺少已发布QA规程，activeOrderId=32，routeProcessId=926632，processId=922917`。
- DB ROLLBACK PASS: 只读核验 `mes_pro_process_pool_active_order`、`mes_pro_process_pool_active_order_process_snapshot`、`mes_pqc_inspection_task` 对 `activeOrderId IN (31,32)` 与 `workOrderId=925868` 的残留计数均为 0。
- CANDIDATE INVENTORY BLOCKED: 只读 DB 统计当前本机已确认工单 4,338 条、唯一有效排产 55 条、路线信息完整 55 条，但完整 QA 规程覆盖的可新增候选 0 条。
- RUNTIME: `http://127.0.0.1:8081/` -> HTTP 200；`http://127.0.0.1:48081/actuator/health` -> `UP`；端口 8081/48081 归属 `E:\IntRuoyi` 主工作区运行态。

## Evidence Validators

- `validate_frontend_feature.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/frontend-feature-evidence.md` -> PRIOR PASS。
- `validate_backend_api.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/backend-api-evidence.md` -> PRIOR PASS。
- `validate_change_request.py --evidence docs/changes/20260806-active-order-code-input.md` -> PRIOR PASS。
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260805-production-leader-active-order-pool-tab/bug-regression-evidence.md` -> PASS。

## Blockers

- Missing `TLW_TENANT`, `TLW_USERNAME`, `TLW_PASSWORD`, `TLW_WORK_ORDER_ID`, `TLW_WORK_ORDER_CODE`, `TLW_TASK_ID`, `TLW_ROUTE_ID`, `TLW_ROUTE_PROCESS_ID`, `TLW_PROCESS_ID`, `TLW_ITEM_ID`, `TLW_EMPLOYEE_PROFILE_ID`, `TLW_DEVICE_ID`, `TLW_RECORDBOOK_ID`, `TLW_SIGNATURE_ID`, `TLW_SIGNATURE_EMPLOYEE_ID`, `TLW_APPROVE_USER_ID`, `TLW_FEEDBACK_CODE`, and `TLW_FEEDBACK_TYPE`。
- 当前本机没有一条满足完整新增成功前置的正式候选；不得通过 SQL、隐藏字段、mock、自由输入或 API-only 写入补齐 QA 规程/排产数据来冒充 E2E PASS。
- Current frontend full-gate blockers: `role-requirement-matrix-preflight-static.spec.cjs`、`mes-process-pool-team-leader-static.spec.js` 均失败在并行 PQC 列表选择器/重置链路缺失，不属于本次活跃订单空值回归。
- Impact: 未执行写入型真实新增/移出/填报闭环 E2E；未使用 mock、自由输入、隐藏字段、API-only 或 admin 基线数据替代。
- Closeout: 因必需真实 E2E 和当前全量前端门禁阻塞，当前不运行 cleanup apply，不创建实现提交、收尾提交，也不推送 `int_main`。
