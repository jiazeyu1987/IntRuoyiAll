# eDHR 单元格链接主端口 E2E 修复复验

## Task Goal

修复当前 `int_main` 主端口复验中发现的 eDHR 单元格链接自动落库前端合同回归，并继续尝试通过真实 Playwright 路径验证“创建/打开执行记录时自动落库预填值”。

## Milestones

- [x] 复现主端口静态合同失败和真实 E2E 夹具阻塞。
- [x] 移除执行页 `/batch-record-cell-link/prefill` 草稿注入路径。
- [x] 运行聚焦静态合同、相邻静态合同和前端类型检查。
- [x] 继续处理真实 Playwright E2E 夹具前置并复验。
- [x] 修复传统批记录打开链路未传当前批次任务 ID 的后端上下文问题。
- [x] 使用隔离 slot 7 运行态复验真实 Playwright E2E。
- [ ] 清理任务自有隔离运行态和 worktree。

## Expected Verification

- `node tests/e2e/edhr-cell-link-auto-persist-static.spec.js`
- `node IntRuoyiBackend/yudao-module-mes/src/test/js/mes-edhr-cell-link-task-id-context-static.spec.cjs`
- `mvn.cmd -pl yudao-module-mes "-Dtest=MesProEdhrBatchExecutionServiceTest#openTask_bindsExistingSingleExecutionContext+openTask_withoutProductionTaskContext_stillOpensBatchRecordWithoutScheduleReference+openTask_ignoresSingleWorkOrderProductionTaskWhenOpeningBatchRecord" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `node --check tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8081 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48081 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`
- `EDHR_BATCH_E2E_BASE_URL=http://127.0.0.1:8088 EDHR_BATCH_E2E_BACKEND_URL=http://127.0.0.1:48088 node tests/e2e/edhr-batch-execution-real-flow.e2e.js`

## Current Status

completed

前端静态合同已恢复：执行页当前只调用 `hydrateDraftState(detail)`，不再调用 `BatchRecordCellLinkApi.getPrefill` 或保留 `normalizeCellLinkPrefillDraftValue`。聚焦静态合同、相邻静态合同和 `pnpm ts:check` 均通过。

真实 Playwright E2E 已在 `测试租户/codexedhrcell01` 下通过。排查并修复任务自有夹具问题：批次任务 `6955` 的 `form_slot_type` 从 `MAIN` 修正为正式报表槽位 `LOSS_REPORT`，并补入按当前任务字段计算的 `slot_config_snapshot_hash=0f84775df0c4a14feeedc6f606d4efc17434e2ce387ce93fb666ae91f26f8d52`。最终 E2E 通过真实批次详情点击“打开填写”，`task/open` 返回 `cellLinkAutoPersist`，执行详情 `cell_values_json` 和原表模式页面输入控件均显示 `EDHR-CELL-20260728-104808`。

用户补充真实截图：生产批号 `881M009889` 已在源表链接详情中绑定到“粗洗工序生产记录 / 生产批号”，但创建批次后粗洗工序单元格仍为空。当前重新排查“eDHR 传统批记录打开链路是否按当前批次任务 ID 隔离执行记录并触发自动落库”，不能用此前测试批次 PASS 替代该真实路径结论。

截图批次 `881M009889` 不在当前本机数据库，无法直接用该批号做本机复验；本次已定位并修复与症状一致的正式后端根因：传统批记录打开请求原先传 `.setTaskId(null)`，现在改为 `.setTaskId(task.getId())`，确保执行记录按当前 `batchExecutionId + batchTaskId` 隔离并触发自动落库。隔离 worktree `D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime` 使用 slot 7 (`8088/48088`) 构建并运行修复后 Jar，真实 Playwright E2E 已在 `测试租户/codexedhrcell01` 下 PASS，证据为 `real-e2e-slot7-evidence.md`。

Cleanup 已完成：slot 7 运行态已停止，`D:\IntRuoyiWorktree\20260728-edhr-cell-link-taskid-runtime` 已从 Git worktree 注册和物理目录删除，临时分支 `codex/20260728-edhr-cell-link-taskid-runtime` 已删除，端口登记项已标记 `active=false`。`task-closeout-cleanup` preview/apply 均通过且无删除项。

提交/推送未执行：当前主工作区存在大量并行任务脏改，且用户当前明确要求只进行 E2E 验证；本次没有进行宽泛 baseline commit，避免混入无关任务改动。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。不用 `/prefill` 或前端草稿值冒充已落库值。
- `是否从根因和长期维护角度解决`：是。恢复执行页只消费执行详情已保存 `cellValues` 的正式链路。
- `是否存在临时补丁或绕过`：否。不新增 mock、默认成功或 API-only 替代真实页面路径。

## 经验门禁

- `docs/backend-development.md#批记录单元格链接预填落库边界`：来源值存在且规则启用时，正式结果必须落库到 `cell_values_json` 并保留字段审计；禁止把 `/prefill` 或前端 hydrate 当作保存结果。
- `docs/e2e-rules.md#静态合同与真实-e2e-同步门禁`：静态合同和真实 E2E 必须同步，合同失败不得宣称融合通过。
- `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`：真实 E2E 必须使用本地数据库真实可打开夹具；缺少 `LOCAL_DATABASE_FIXTURE` 时阻塞，不用 mock、API-only 或直接 SQL 造数替代。
- `docs/e2e-rules.md#edhr-批次执行数据库夹具与证据文件门禁`：既有批次任务还必须核对 `form_slot_type` 与目标报表槽位一致、`slot_config_snapshot_hash` 非空；执行页单元格值断言需切到“原表模式”。
- `docs/frontend-development.md#前端静态契约隔离门禁`：聚焦静态合同可作为当前回归的 RED/GREEN 门禁。

## Cleanup Keep

- doc/tasks/20260728-edhr-cell-link-main-e2e-repair/bug-regression-evidence.md
- doc/tasks/20260728-edhr-cell-link-main-e2e-repair/frontend-feature-evidence.md
- doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-evidence.md
- doc/tasks/20260728-edhr-cell-link-main-e2e-repair/real-e2e-slot7-evidence.md
