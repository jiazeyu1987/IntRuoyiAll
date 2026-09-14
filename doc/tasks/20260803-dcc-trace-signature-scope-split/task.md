# DCC 受控浏览追溯与签核页面范围拆分

## Task Goal

修复受控浏览列表中“追溯”和“签核”按钮打开同一详情内容的问题：追溯入口只展示生命周期/版本/分发/培训/打印等追溯信息，签核入口只展示签核追溯与签名留痕信息，减少无关内容干扰。

## Milestones

- [x] 建立任务文档并记录既有脏工作区基线。
- [x] 补充 RED 静态契约，证明两个入口和详情页面范围未拆分。
- [x] 最小修改受控浏览入口 query 与详情页区块可见性。
- [x] 运行定向静态契约、类型检查或记录明确阻塞。
- [x] 完成验证报告和收尾状态。

## Expected Verification

- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js`
- `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js`
- `pnpm ts:check`

## Current Status

blocked

实现、定向验证、cleanup preview/apply 均已完成；收尾提交/推送阶段共享分支持续出现非本任务脏改动和并发基线提交，无法稳定形成只属于本任务的最终 closeout 提交，因此暂不标记 completed。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划通过明确入口模式和页面区块边界解决，而不是隐藏错误或复用同一页面语义。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：当前工作区已有并行改动，本任务使用 DCC 详情页专用静态契约隔离 RED/GREEN。
- 适用 `docs/frontend-development.md#前端同路由多入口分面门禁`：追溯和签核复用同一详情路由时必须显式建模入口 scope，并按 scope 正向控制区块可见性。
- 适用 `docs/e2e-rules.md#dcc-文控审批处理入口门禁`：受控浏览 traceability 入口必须保留正式详情路径和只读追溯语义，不得改成 API-only 或绕过路由守卫。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：任务启动前已有大量并行改动，已先做基线提交并记录 hash。

## Closeout Blocker

- 2026-08-03 收尾期间多次基线后又出现新的非本任务改动；为避免误提交或误推送其它任务，当前停在已验证但未 completed 的安全边界。
- 本任务验证证据已进入共享提交 `39f4b1eda chore: baseline dirty worktree before DCC preview E2E validation`；最终 completed 状态需在工作区稳定后单独收口。

## Cleanup Keep

- doc/tasks/20260803-dcc-trace-signature-scope-split/task.md
- doc/tasks/20260803-dcc-trace-signature-scope-split/execution-log.md
- doc/tasks/20260803-dcc-trace-signature-scope-split/verification-report.md
