# DCC 受控浏览追溯与签核页面范围拆分

## Task Goal

修复受控浏览列表中“追溯”和“签核”按钮打开同一详情内容的问题：追溯入口只展示生命周期/版本/分发/培训/打印等追溯信息，签核入口只展示签核追溯与签名留痕信息，减少无关内容干扰。

## Milestones

- [x] 建立任务文档并记录既有脏工作区基线。
- [ ] 补充 RED 静态契约，证明两个入口和详情页面范围未拆分。
- [ ] 最小修改受控浏览入口 query 与详情页区块可见性。
- [ ] 运行定向静态契约、类型检查或记录明确阻塞。
- [ ] 完成验证报告和收尾状态。

## Expected Verification

- `node tests/e2e/dcc-traceability-ux-static.spec.js`
- `node tests/e2e/dcc-browser-file-number-detail-entry-static.spec.js`
- `node tests/e2e/dcc-detail-signature-view-mode-static.spec.js`
- `pnpm ts:check`

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，计划通过明确入口模式和页面区块边界解决，而不是隐藏错误或复用同一页面语义。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 适用 `docs/frontend-development.md#前端静态契约隔离门禁`：当前工作区已有并行改动，本任务使用 DCC 详情页专用静态契约隔离 RED/GREEN。
- 适用 `docs/e2e-rules.md#dcc-文控审批处理入口门禁`：受控浏览 traceability 入口必须保留正式详情路径和只读追溯语义，不得改成 API-only 或绕过路由守卫。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：任务启动前已有大量并行改动，已先做基线提交并记录 hash。
