# 任务：DCC 受控预览 PDF Worker HMR 报错修复

## 目标

修复 DCC 受控文件 PDF 预览在本地 Vite 开发态触发 `@vite/client` `ReferenceError: document is not defined` 的问题，确保 PDF.js worker 不再走会被 HMR 注入的源模块链路。

## 前置任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-controlled-file-download-auth-fix\task.md`
- 启动前状态：`completed`
- 影响：前一个前端任务已完成，不阻塞本次受控预览 worker 缺陷修复。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\view\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\public\pdfjs\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\dcc-protected-viewer-pdf-worker-hmr.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-protected-viewer-pdf-worker-hmr-fix\**`

## 非范围

- 不修改 DCC 受控预览后端接口。
- 不关闭 Vite HMR overlay，不新增降级分支或静默忽略错误。
- 不顺带改动 PDF 预览页面的视觉样式、交互或权限逻辑。

## 里程碑

- [x] M1：记录 BDD 场景并确认 worker 报错根因链路。
- [x] M2：先补 RED 回归测试，锁定 PDF worker 必须走静态资源链路。
- [x] M3：以最小改动调整 worker 资源位置与 `workerSrc` 配置。
- [x] M4：完成 GREEN 验证、任务证据与收尾检查。

## 预期验证

- `node --test scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs`
- 如前端本地服务可用，补充验证 `http://localhost:8081` 对应受控预览路径不再出现该控制台错误。
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-dcc-protected-viewer-pdf-worker-hmr-fix --mode preview`

## 当前状态

completed

## Root Cause

- `src/views/dcc/controlled-file/view/index.vue` 之前把 `GlobalWorkerOptions.workerSrc` 指向了 `src/.../vendor/pdf.worker.mjs`。
- 在 Vite 开发态，这条链路会把 PDF.js worker 当成源模块处理，worker 侧收到 HMR 更新时会执行 `@vite/client`。
- 当前仓库使用的 Vite client 在 `hasErrorOverlay()` 中直接访问 `document`，而 worker 上下文不存在 `document`，因此抛出 `ReferenceError: document is not defined`。

## Final Verification Result

- PASS：`node --test scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs`
- PASS：`pnpm exec eslint src/views/dcc/controlled-file/view/index.vue scripts/dcc-protected-viewer-pdf-worker-hmr.test.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260520-dcc-protected-viewer-pdf-worker-hmr-fix\bug-regression-evidence.md`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260520-dcc-protected-viewer-pdf-worker-hmr-fix --mode preview`
- PASS：任务级 bug 证据已记录到 `doc/tasks/20260520-dcc-protected-viewer-pdf-worker-hmr-fix/bug-regression-evidence.md`
- SKIP：未执行 `task_closeout.py --mode apply`，因为 preview 仅把 `bug-regression-evidence.md` 识别为删除候选，而该文件需要保留为本次缺陷回归证据。
- PENDING：真实页面路径回放未执行；当前未提供可稳定进入受控 PDF 预览页的具体测试文件与页面入口参数。

## 阻塞与影响

- 阻塞：缺少一个可直接打开到 DCC 受控 PDF 预览页的稳定真实数据入口，当前无法在 `http://localhost:8081` 上做同路径复现回放。
- 影响：本次以源码级 RED/GREEN 回归和静态资源服务验证作为完成依据；如需再补浏览器实跑，需要用户提供可用的预览记录或入口参数。
