# 任务：DCC PDF 预览缩放改为即时 CSS 缩放

- Task ID: 20260702-dcc-preview-css-zoom
- Created: 2026-07-02
- Current Status: completed

## Task Goal

优化 DCC 受控文件 PDF 预览缩放性能：大文件首次加载后，点击放大/缩小不再重新调用 pdf.js 渲染整份文件，而是通过前端 CSS transform 即时缩放。

## Milestones

1. 确认当前 PDF 缩放每次点击会重新渲染。completed
2. 补充 RED 契约，禁止缩放点击触发 PDF 全量重渲染。completed
3. 改造 PDF 缩放为 CSS transform 即时变换。completed
4. 运行 DCC 静态测试、类型检查与运行态确认。pending
5. 收尾清理并提交本任务改动。pending

## Expected Verification

- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`
- `git diff --check`

## 经验门禁

- 已读取 `frontend-feature-delivery` 与前端证据契约。
- 已读取 `docs/powershell-memory.md`，PowerShell 中文与文件读写使用 UTF-8。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，根因是缩放事件绑定到 pdf.js 全量重渲染；本次将缩放状态改为纯前端 transform。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。

## Current Status

in_progress


## Final Verification Result

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> `PASS`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> `PASS`
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`
- `validate_frontend_feature.py` -> `PASS`
- Local Vite served source/style markers -> `PASS`

## Current Status

completed
