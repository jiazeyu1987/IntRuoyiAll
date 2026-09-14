# Execution Log

## User Intent

用户授权在芋道源码中验证排产工单手动重排：选择来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单，点击“手动重排”，在弹框中选择“开始重排”，点击“确认应用重排”，并验证四项目标。

## Preconditions

- 任务创建时 `docs/experience-index.md` 检查结果：缺失；用户已明确授权“在芋道源码里验证”，记录风险后继续。
- 收尾时 `docs/experience-index.md` 已存在且为未跟踪文件；只读命中并读取 `docs/e2e-rules.md`、`docs/login-access.md`，未修改该并发产生的经验文件。
- 用户授权：已授权本机 `芋道源码/admin` 身份标签的真实路径验证。
- 当前 Git 状态：存在大量非本任务未提交变更；本任务只允许写入 `doc/tasks/verify-manual-reschedule-881mo-20260724/` 下的任务证据文件。

## BDD Scenarios

- `BDD: 手动重排两个指定来源生产工单 -> Given` 用户位于排产工单页签且存在来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单；`When` 勾选这两个工单并执行“手动重排 -> 开始重排 -> 确认应用重排”；`Then` 页面反馈重排成功。
- `BDD: 仅指定工单产品编号变橙色 -> Given` 手动重排已成功；`When` 查看排产工单页签产品编号列；`Then` 只有来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号为橙色。
- `BDD: 最近一次成功排产时间更新 -> Given` 手动重排已成功；`When` 查看最近一次成功排产时间；`Then` 时间更新为本次操作时间。
- `BDD: 生产排产甘特图仅显示指定工单 -> Given` 手动重排已成功；`When` 切换到生产排产页签查看甘特图；`Then` 甘特图有且仅有来源生产工单号为 `881MO093613`、`881MO093615` 的两个工单。

## Command Evidence

- `GREEN: Test-Path -LiteralPath 'E:\IntRuoyi\docs\experience-index.md' -> PASS, returned False and recorded missing gate`.
- `GREEN: node --check 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> PASS`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: task verification script initially unpacked web-storage-cache token as a quoted JSON string, causing backend API auth to return 401 while preparing target row evidence`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: inherited test-tenant E2E default found zero schedule orders for source work order 881MO093613/881MO093615; user authorization is for 芋道源码 tenant, so script must use the local default 芋道源码/admin identity`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: current 芋道源码 page layout only exposed the default 排产工单号 filter, while both target rows were already visible in the current 排产工单 tab; script selection path must select visible target rows directly instead of relying on a hidden 来源生产工单号 filter input`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: backend response order did not match the frontend's locally sorted visible table rows, causing the script to map target row indexes to disabled frozen rows; selection must be based on current DOM visible rows`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: coordinate-based checkbox click did not wait for Element Plus table selection state to enable 手动重排; use row-text checkbox click and wait for button enabled state`.
- `RED: node 'E:\IntRuoyi\doc\tasks\verify-manual-reschedule-881mo-20260724\manual-reschedule-verify.e2e.cjs' -> FAIL, expected reason: Playwright timed out before login page domcontentloaded via localhost while direct HTTP health checks for 8081/48081 passed; pin baseUrl to 127.0.0.1 and extend login page navigation timeout`.
- `GREEN: local runtime preflight -> PASS, http://127.0.0.1:8081/login returned 200 and http://127.0.0.1:48081/actuator/health returned 200`.
- `GREEN: real UI manual replan -> PASS, selected only schedule orders sourced from 881MO093613 and 881MO093615; UI displayed "应用重排成功：正式排程已更新，新增任务 136 个，删除任务 136 个，保留任务 7 个。"` 
- `GREEN: latest successful schedule time -> PASS, /mes/pro/auto-schedule/apply/latest-success returned operationType=REPLAN_APPLY and appliedAt=2026-07-24 14:39:48; UI toolbar displayed 2026-07-24 14:39`.
- `RED: product-code orange state -> FAIL, after successful replan both target product-code cells retained class schedule-order-pool__product-code--unscheduled and computed color rgb(23, 32, 51), not the orange scheduled class/color`.
- `RED: node tests/e2e/mes-replan-product-code-current-selection-static.spec.js -> FAIL, expected reason: successful replan apply flow does not call updateLastReplanParticipatingScheduleOrders(freshPreview) after replanApply`.
- `GREEN: node tests/e2e/mes-replan-product-code-current-selection-static.spec.js -> PASS`.
- `GREEN: node tests/e2e/mes-pro-schedule-order-replan-apply-enabled-static.spec.js -> PASS`.
- `GREEN: node tests/e2e/mes-pro-schedule-order-replan-settings-progress-static.spec.js -> PASS`.
- `GREEN: node tests/e2e/mes-pro-schedule-order-replan-skipped-selected-confirm-static.spec.js -> PASS`.
- `GREEN: node tests/e2e/mes-replan-whole-day-apply-static.spec.js -> PASS`.
- `GREEN: pnpm ts:check:schedule -> PASS`.
- `GREEN: node --check doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> PASS`.
- `RED: node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> FAIL, expected reason: repair verification script lacked a pre-apply assertion that both target rows were selected; the run applied one visible target and therefore only 881MO093615 turned orange`.
- `RED: node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> FAIL, expected reason: Element Plus table row-selection helper still did not reliably click the first target row; verification helper must use the visible body-row locator and assert each target source is selected before apply`.
- `RED: node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> FAIL, expected reason: latest-success API returned appliedAt as a millisecond timestamp; verification helper must parse both numeric timestamps and string datetimes strictly`.
- `GREEN: preview structure inspection -> PASS, two-target replan preview returned workOrderCount=2, generatedTaskCount=136, preservedTaskCount=7, and task rows for both 881MO093613 and 881MO093615`.
- `GREEN: production gantt scope -> PASS, gantt-list returned only workOrderCode values 881MO093613 and 881MO093615 (145 task/project records); UI screenshot after collapsing 881MO093613 showed the only two work-order roots 881MO093613 and 881MO093615`.
- `GREEN: node --check doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> PASS`.
- `GREEN: node doc/tasks/verify-manual-reschedule-881mo-20260724/manual-reschedule-repair-verify.e2e.cjs -> PASS, real UI path verified a/b/c/d at 2026-07-24 17:32; evidence JSON output/playwright/verify-manual-reschedule-881mo-20260724-repair/repair-verification-report.json`.
- `GREEN: project-experience-consolidation -> PASS, added Element Plus el-table body-row selection gate to docs/e2e-rules.md and routed keywords through docs/experience-index.md`.
- `GREEN: rg -n "Element Plus 表格选择门禁|表头全选误点|Playwright body-wrapper row selection" docs/e2e-rules.md docs/experience-index.md -> PASS`.

## Milestone Updates

- 创建任务记录：完成。
- 真实验证脚本：已创建并通过语法检查；首次执行在鉴权准备阶段失败，已定位为任务脚本缓存解包问题。
- 目标租户修正：从旧测试租户默认切换为用户授权的本机 `芋道源码/admin` 身份标签；密码只从前端环境文件读取，不写入任务记录。
- 页面选择路径修正：当前排产工单页签默认列表已显示 `881MO093613` 与 `881MO093615`，改为按当前可见行勾选目标工单。
- 行号映射修正：改为先从当前 DOM 可见业务行定位目标来源生产工单，再点击同序号选择列复选框。
- 选中状态修正：改为按目标行文本直接点击该行复选框，并等待“手动重排”按钮从 disabled 切换为 enabled。
- 导航稳定性修正：本机前端入口固定为 `http://127.0.0.1:8081`，避免 localhost 偶发解析或导航等待波动。
- 真实重排与全部目标核验：完成。a/c/d 通过；b 失败。
- 用户在 2026-07-24 授权修复 b：任务重新打开，进入严格 TDD 修复。
- 产品编号橙色状态修复：完成；`confirmApplyReplanStartChoice()` 在 `replanApply` 成功之后调用 `updateLastReplanParticipatingScheduleOrders(freshPreview)`，只基于本次真实成功应用的预览结果更新参与工单集合。
- 修复后真实重排复测：完成；2026-07-24 17:32 再次通过真实前端路径应用重排，a/b/c/d 全部通过。
- 项目经验沉淀：完成；将 Element Plus 表格行复选框必须限定可见 body row 且写入前断言选中集合的门禁合并到 `docs/e2e-rules.md`，未新建长期经验文档。

## Blockers

- 暂无功能阻塞。功能修复和真实路径验证已完成。
- 收尾风险：当前仓库存在非本任务脏改且分支已领先远端，二次收尾、提交与推送必须先按 Git/closeout 规则处理，不能混入无关变更。

## Closeout

- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id verify-manual-reschedule-881mo-20260724 --mode preview -> PASS, keep task.md/execution-log.md/verification-report.md; delete only task helper and output/playwright/verify-manual-reschedule-881mo-20260724`.
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace E:\IntRuoyi --task-id verify-manual-reschedule-881mo-20260724 --mode apply -> PASS, deleted only task-owned helper and temporary Playwright evidence`.
- Git commit：未执行。原因：功能验收存在失败项，且本任务未产生需要集成的业务实现变更。
- 二次收尾状态：`ready_for_closeout`。本次修复已产生前端实现变更、回归验证与真实 E2E 证据；提交和推送尚未执行。
