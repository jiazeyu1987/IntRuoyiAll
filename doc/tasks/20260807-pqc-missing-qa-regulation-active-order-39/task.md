# PQC Missing QA Regulation Active Order 39

## Task Goal

Fix or formally block the frontline PQC error:

`当前工序缺少已发布 QA 检验规程，activeOrderId=39，routeProcessId=980632，processId=922986`

The solution must preserve the formal QA regulation source of truth and must not introduce fallback, default regulation selection, silent success, or exception swallowing.

## Milestones

- [x] Reproduce and diagnose the active order 39 PQC regulation lookup path.
- [x] Confirm whether a published QA inspection regulation exists for routeProcessId `980632` / processId `922986`.
- [x] Add or update a regression test that fails for the current incorrect behavior.
- [x] Implement the smallest formal fix if the root cause is code or query logic.
- [x] Run targeted backend verification and record RED/GREEN evidence.
- [x] Refresh the local int_main runtime Jar and verify backend health.

## Expected Verification

- Read-only database evidence for active order `39`, route process `980632`, process `922986`, PQC task rows, and published QA regulation/version rows.
- Backend regression test covering the failing PQC regulation resolution path.
- Targeted Maven test command for the affected MES service passes.
- Runtime/API verification only if local prerequisites are already available and authorized.

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；活跃订单工序集合改为读取冻结的 active-order process snapshot，当前路线表仅用于解析展示/运行元数据。
- `是否存在临时补丁或绕过`：否。

## Completion Evidence

- Active order `39` is `work_order_id=980026`, `route_id=980091`, `route_version_id=622`, product `924008`.
- The active-order process snapshot contains only `route_process_id=980631`, `process_id=922985`.
- Pending PQC tasks `211`-`214` are bound to `980631/922985`, with published regulation version `36`.
- Current route `980091` also contains `980632/922986`, but that pair is not frozen into active order `39` and has no published QA regulation. It must not be checked for this order.
- Targeted regression and the full `MesFrontlinePqcContextServiceTest` class pass.
- Local int_main backend was refreshed on port `48081` with runtime Jar `backend-latest-20260807-2338-pqc-active-order-snapshot.jar`; health is `UP`.
- `task-closeout-cleanup` preview and apply completed without blocked paths or warnings; only `task.md`, `execution-log.md`, and `verification-report.md` remain.

## Experience Gate

- Trigger: PQC 填写、`active-order/processes`、QA 检验规程、活跃订单当前工序缺规程、PQC 待检工单。
- Preflight check: 按活跃订单冻结路线和版本枚举当前启用工序，逐工序核对发布 QA 规程、适用检验类型和可执行 `PENDING` PQC 任务；不能只核对准备提交的单个工序。
- Blocker: 任一当前工序缺发布规程、规程不适用、缺待执行任务、工序映射与冻结版本不一致，或非取消 PQC 任务缺正式 `routeProcessId/processId` 时必须停止。
- Verification: 记录逐工序只读查询证据；如修复代码，补后端回归覆盖该解析链路并运行 targeted Maven 测试。
- Forbidden action: 禁止默认规程、前端隐藏、直接提交 API、直接插入 PQC 事件、给无关工序批量补造规程/任务、空成功或异常吞掉。
- Evidence: `docs/backend-development.md#mes-pqc-项目级检验快照门禁`。
