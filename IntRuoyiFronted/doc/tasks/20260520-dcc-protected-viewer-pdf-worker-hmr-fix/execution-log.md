# 执行日志：DCC 受控预览 PDF Worker HMR 报错修复

BDD: protected viewer pdf worker avoids vite hmr client -> Given DCC 受控预览页面通过 PDF.js worker 渲染 PDF 且前端运行在 Vite 开发态，When worker 资源被浏览器加载并参与 HMR 更新，Then worker 不应执行 `@vite/client`，也不应再抛出 `ReferenceError: document is not defined`。

RED: node --test scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs -> FAIL, `workerSrc` 仍指向 `./vendor/pdf.worker.mjs` 且 `public/pdfjs/pdf.worker.mjs` 不存在

GREEN: node --test scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs -> PASS

GREEN: pnpm exec eslint src/views/dcc/controlled-file/view/index.vue scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs -> PASS

GREEN: python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-protected-viewer-pdf-worker-hmr-fix\bug-regression-evidence.md -> PASS

GREEN: python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-dcc-protected-viewer-pdf-worker-hmr-fix --mode preview -> PASS, preview keep=`task.md`/`execution-log.md`, delete candidate=`bug-regression-evidence.md`, apply skipped to retain regression evidence

Root Cause:

- 旧实现把 PDF.js worker 指向了 `src/views/dcc/controlled-file/view/vendor/pdf.worker.mjs`。
- Vite 开发态会把该 worker 作为源模块处理，导致 worker 内部也被拉入 `@vite/client`。
- 当前 Vite client 的 overlay 检测逻辑直接访问 `document`，在 worker 上下文中触发 `ReferenceError: document is not defined`。

Fix:

- 将 `GlobalWorkerOptions.workerSrc` 改为基于 `import.meta.env.BASE_URL` 的 `public/pdfjs/pdf.worker.mjs` 静态资源路径。
- 将现有 vendored `pdf.worker.mjs` 复制到 `public/pdfjs/`，让浏览器加载不带 HMR 注入的静态 worker 文件。
