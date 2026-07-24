# 任务：DCC PDF 预览 ArrayBuffer detached 修复

- Task ID: 20260702-dcc-pdf-preview-arraybuffer-detach-fix
- Created: 2026-07-02

## Current Status

completed

## Task Goal

修复 DCC 受控 PDF 预览加载时报 `Failed to execute 'postMessage' on 'Worker': ArrayBuffer at index 0 is already detached.` 的问题，确保 PDF 首次预览和缩放重渲染都使用可传递给 pdf.js worker 的有效字节副本。

## Milestones

1. 复现并定位 PDF worker 接收已 detached ArrayBuffer 的根因。completed
2. 补充静态回归测试，要求传给 pdf.js worker 前克隆 PDF 字节。completed
3. 实现最小修复，避免缓存字节被 worker transfer 后失效。completed
4. 运行 DCC 预览静态测试、类型检查和回归证据校验。completed
5. 收尾清理并提交本任务改动。completed

## Expected Verification

- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`
- `validate_bug_regression.py`

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 已读取 `docs/powershell-memory.md`，PowerShell 中文与文件读写使用 UTF-8。
- 已读取 bug-regression-fix-loop 契约，按 RED/GREEN 记录回归证据。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，根因是 pdf.js worker transfer 会 detach 传入 ArrayBuffer，因此必须传入工作副本而非缓存源字节。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。

## Final Verification Result

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> `PASS`。
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> `PASS`。
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`。
