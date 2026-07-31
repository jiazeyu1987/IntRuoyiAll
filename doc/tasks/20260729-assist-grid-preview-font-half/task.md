# eDHR 辅助网格单元格字号减半

## Task Goal

按截图反馈，将 eDHR 填写辅助模式网格中每个单元格内文字字号减小为当前的 1/2；不改变字段值、校验、保存、提交、工序切换、填写人切换、`assistRows` 或 rowKey 协议。

## Milestones

1. `completed`：读取前端、E2E、任务收尾、PowerShell 和经验门禁。
2. `completed`：保存追加需求前脏工作区基线提交。
3. `completed`：补充字号减半 RED 静态合同。
4. `completed`：实施最小 CSS 调整，作用域限定在辅助网格单元格。
5. `completed`：运行目标合同、相邻回归、类型检查和证据校验。
6. `completed`：cleanup preview/apply、提交并推送。
7. `completed`：按用户“继续”确认口径，覆盖后续并发放大规则，将标题、输入提示和单位统一恢复为 1/2 字号。
8. `completed`：证据校验、cleanup、提交并推送。

## Expected Verification

- `node tests/e2e/edhr-fill-workspace-card-density-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-static.spec.js`
- `node tests/e2e/edhr-fill-workspace-static.spec.js`
- `node tests/e2e/edhr-assist-fill-mode-configured-grid-static.spec.js`
- `pnpm ts:check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260729-assist-grid-preview-font-half/frontend-feature-evidence.md`

## Applicable Gates

- 前端静态契约隔离门禁：使用专用静态合同锁定辅助网格字号，不扩大到无关页面。
- eDHR 辅助模式当前工序 assistRows 路由门禁：只调整展示层字号，不改写 `assistRows`、`ASSIST_GRID_U` rowKey、原始行列、工序切换或 `openTask` 链路。
- Element Plus 选择框显示门禁：输入、选择、日期、选项、单位和校验文字都在单元格内按目标字号展示。
- 同文件并行改动选择性暂存门禁：`ExecutionPage.vue` 同时存在并发 hunks，本任务只提交字号相关 CSS hunks。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接在辅助网格单元格样式层统一收敛字号。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Cleanup Keep

- doc/tasks/20260729-assist-grid-preview-font-half/frontend-feature-evidence.md
