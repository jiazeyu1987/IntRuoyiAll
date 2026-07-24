# 执行日志：DCC PDF 预览 ArrayBuffer detached 修复

- BDD: PDF 预览首次加载不因 worker transfer 失败 -> Given 用户打开 DCC 受控 PDF 预览 / When pdf.js worker 加载 PDF 字节 / Then worker 收到的是独立字节副本，不会报 ArrayBuffer 已 detached。
- BDD: PDF 缩放重渲染复用源字节但不复用 detached buffer -> Given 用户在 PDF 预览中点击放大或缩小 / When 组件重新调用 pdf.js 渲染 / Then 每次传给 worker 的都是新克隆字节，缓存源字节仍可继续使用。
- GREEN: experience-preflight -> PASS，已读取 docs/experience-index.md、docs/powershell-memory.md 与 bug-regression-fix-loop 契约。
- RED: node tests/e2e/dcc-common-file-preview-source.spec.js -> FAIL, Protected viewer transform controls contract missing: clonePdfBytesForWorker。

- GREEN: node tests/e2e/dcc-common-file-preview-source.spec.js -> PASS, PDF worker byte clone and preview source wiring contract passed。
- GREEN: node tests/e2e/dcc-controlled-file-protection.contract.test.js -> PASS。
- GREEN: NODE_OPTIONS=--max-old-space-size=8192 pnpm ts:check -> PASS。
- GREEN: node tests/e2e/dcc-common-file-preview-source.spec.js; node tests/e2e/dcc-controlled-file-protection.contract.test.js; pnpm ts:check -> PASS，PDF worker 字节克隆契约与类型检查通过。
- BLOCKER: task-closeout-cleanup apply -> FAIL，task.md 状态使用列表项 `- Current Status: completed`，清理脚本只识别 `## Current Status` 章节。
- GREEN: task.md status format -> PASS，已改为 `## Current Status` 独立章节，便于清理脚本识别 completed。
