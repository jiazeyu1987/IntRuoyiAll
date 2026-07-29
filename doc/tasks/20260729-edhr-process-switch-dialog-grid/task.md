# 20260729 eDHR 切换工序弹框网格化

## Task Goal

将 eDHR 填写页“切换工序”弹框从窄列表改为大尺寸 grid 卡片选择器，视觉范围接近用户截图红框区域；单屏可展示至少 30 个工序卡片，并保持正式工序切换链路不变。

## Milestones

- [x] 识别当前弹框组件、入口和既有工序切换数据契约。
- [x] 编写/更新聚焦静态合同，先覆盖大弹框、grid 卡片和单屏 30 卡片容量要求。
- [x] 修改前端组件样式与结构，保留现有切换行为和状态展示。
- [x] 运行聚焦验证和必要前端回归，记录 RED/GREEN/REGRESSION。
- [ ] 完成收尾、经验沉淀、提交与推送，或记录阻塞项。

## Expected Verification

- 聚焦静态合同证明弹框使用 grid 而非列表展示，且网格容量按视口高度可容纳至少 30 张卡片。
- 相关 eDHR 工序切换静态合同通过。
- 如本机真实入口、登录和运行态齐备，使用 Playwright 进行只读真实页面检查；否则记录缺失前置与影响。

## Current Status

completed

## Verification Evidence

- RED: `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js` -> FAIL，断言当前弹框仍为固定 `680px`，缺少工序 grid 容量约束。
- GREEN: `node tests/e2e/edhr-assist-process-switch-dialog-grid-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-assist-process-switch-all-statuses-static.spec.js` -> PASS。
- REGRESSION: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS。
- TYPECHECK: `pnpm ts:check` -> 首次 184s 超时无结论；延长到 600s 后 PASS。
- EVIDENCE: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-edhr-process-switch-dialog-grid/frontend-feature-evidence.md` -> PASS。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-process-switch-dialog-grid --mode preview` -> PASS，keep 4 个任务文件，delete none，blocked none。
- CLEANUP: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260729-edhr-process-switch-dialog-grid --mode apply` -> PASS，deleted none。
- Real E2E: 未运行。本任务为弹框结构/样式静态改造；当前验证以聚焦静态合同和类型检查覆盖。若需要截图验收，需在已确认本地登录态与运行态后补 Playwright 只读检查。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是改造现有正式弹框布局，不改变数据来源或切换链路。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：工序切换列表必须来自当前批次全部普通工序任务，切换行为继续沿用正式 `openTask`、只读执行页或批次详情选中工序链路；本任务仅改弹框布局，不改变候选来源或导航规则。
- `docs/frontend-development.md#前端静态契约隔离门禁`：若宽静态合同存在无关既有失败，本任务需使用聚焦静态合同证明当前弹框布局需求 RED/GREEN。
- `docs/powershell-memory.md#脏工作区基线门禁`：当前工作区存在既有脏改动，必须单独基线提交，并确保本任务文档和后续实现不混入基线。
- `docs/powershell-memory.md#PowerShell 分号串联测试退出码门禁`：验证命令逐条运行并记录退出码，不用串联命令的最后一条结果代表全部通过。
- Project experience consolidation: 已检查现有 `docs/frontend-development.md`、`docs/powershell-memory.md` 和 `docs/experience-index.md`，本任务未产生超出现有门禁的新长期经验，因此不新增长期经验文档。

## Cleanup Keep

- doc/tasks/20260729-edhr-process-switch-dialog-grid/frontend-feature-evidence.md
