# 执行日志

## User Intent

- 用户要求：已完成的排产工单在“来源生产工单号”后面追加“(已完成)”。
- 范围：仅修改排产工单列表的来源工单号展示；不修改完成状态、排产规则或后端接口。

## BDD

- BDD: 已完成工单显示完成标识 -> Given 排产工单的 `manualFinished=true` 或 `status=3`；When 页面渲染“来源生产工单号”；Then 工单号后紧跟“(已完成)”。
- BDD: 未完成工单保持原文 -> Given 排产工单未人工完成且状态不是已完成；When 页面渲染“来源生产工单号”；Then 只显示原工单号，不追加完成标识。

## Milestone Log

- M0：已读取前端开发、任务收尾、PowerShell 编码规则和前端功能交付证据契约。
- 经验门禁：采用任务专用静态合同、真实页面只读核对、独立记录每条测试退出码、脏工作区基线与任务提交隔离。
- 启动时工作区存在其他任务的已跟踪和未跟踪改动；将按强制规则先建立独立基线提交，当前任务目录不纳入基线。
- 基线提交：`de6b84628 chore: baseline concurrent changes before schedule order label`，共 60 个既有文件；完整文件清单可由 `git show --name-status --oneline de6b84628` 复核，包含 DCC 上传链路、MES 路线/组长链路、并行任务测试与任务记录、`docs/database-rules.md`、`docs/frontend-development.md`，不包含本任务目录或排产工单页面。
- 基线提交后残余复扫发现其他并行任务继续修改 6 个文件：`MesTeamEmployeeBindingServiceTest.java`、角色对齐任务 3 个 SQL、全量 PQC 搜索执行日志、PQC 组长五记录执行日志；均保持未暂存且不触碰。
- M1：新增任务专用静态合同并取得预期 RED。
- M2：来源生产工单号改为调用 `getScheduleOrderSourceCodeText(row)`；`manualFinished=true` 或正式 `status=3` 时追加“(已完成)”，未从进度或数量推断。
- M3：类型检查、任务专用静态合同、相邻合同、真实页面只读路径和证据校验均已通过。
- 并行提交归属复扫：本任务源码、测试和初始任务记录在共享分支被另一并行任务的基线提交 `20d6fe43e` 收录，后续任务记录又被 `8e71bc24f` 收录；为避免破坏并行历史，不执行回退或重复改写，收尾提交仅纳入本任务后续记录。
- 经验沉淀门禁：本次采用的任务专用静态合同、只读 E2E 无写请求核验和共享工作区隔离规则已分别存在于 `docs/frontend-development.md`、`docs/e2e-rules.md` 和 `docs/task-closeout-rules.md`，未产生新的可复用长期经验，不修改长期经验文档。

## Verification Evidence

- `RED: node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs -> FAIL, expected reason: Schedule order page must define a bounded source work order display text resolver.`
- `GREEN: node tests/e2e/mes-schedule-order-completed-source-label-static.spec.cjs -> PASS`。
- `REGRESSION: node tests/e2e/mes-schedule-order-workorder-link-static.spec.js -> PASS`。
- `REGRESSION: node tests/e2e/mes-schedule-order-replan-finished-disabled-static.spec.js -> PASS`。
- `REGRESSION: node tests/e2e/mes-schedule-order-main-table-wrap-static.spec.js -> PASS`。
- `REGRESSION: node tests/e2e/mes-replan-product-code-current-selection-static.spec.js -> PASS`。
- `GREEN: pnpm ts:check -> PASS`。
- `E2E: Playwright /mes/pro/schedule-order -> PASS`；本机真实页面中已完成行 `881MO090880(已完成)` 与正式状态完成行 `881MO090863(已完成)` 均显示标识，未完成对照行 `881MO093613` 保持原编号。
- `E2E: GET /admin-api/mes/pro/schedule-order/page?pageNo=1&pageSize=20 -> 200 OK`；验证过程 MES 写请求数为 0，控制台错误数为 0。
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --self-test -> PASS`。
- `GREEN: python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence E:\IntRuoyi\doc\tasks\20260807-schedule-order-completed-label\frontend-feature-evidence.md -> PASS`。
- `GREEN: git diff --check（本任务文件） -> PASS`。
- Playwright 会话已关闭；包含登录快照的任务专用临时目录 `output/playwright/20260807-schedule-order-completed-label` 已删除且复核不存在。
- 相邻既有合同 `mes-pro-schedule-order-manual-finish-static.spec.js` 失败在 `completionFilter: 'INCOMPLETE'` 旧断言；`git show HEAD:IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue` 证明基线源码已不含该默认值，本任务 diff 仅涉及来源工单号文本和 helper，故记录为非本任务历史缺口，不修改该筛选行为。

## Blockers

- 无。
