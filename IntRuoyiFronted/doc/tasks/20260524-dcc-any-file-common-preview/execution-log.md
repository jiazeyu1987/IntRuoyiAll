# 20260524 DCC 任意文件上传与常见文件预览 - 前端执行日志

## BDD

- BDD: 上传任意单文件 -> Given 用户进入 DCC 上传页面 / When 选择任意类型单文件 / Then 前端不因 MIME 或扩展名阻止选择，并调用后端预览上传。
- BDD: 媒体文件在线预览 -> Given 后端返回 `VIDEO` 或 `AUDIO` 预览类型 / When 受控预览组件加载文件 / Then 页面使用浏览器原生只读播放器展示，并叠加受控水印。
- BDD: 未知类型明确不可预览 -> Given 后端返回 `DOWNLOAD_ONLY` / When 预览组件渲染 / Then 页面明确提示当前类型仅支持下载，不尝试伪造预览。

## TDD / Verification Evidence

- RED: `node tests\e2e\dcc-common-file-preview-source.spec.js` -> FAIL, `ControlledFilePreviewKind must include VIDEO`。
- GREEN: `node tests\e2e\dcc-common-file-preview-source.spec.js` -> PASS, `PASS: DCC common file preview source wiring is present`。
- GREEN: `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/upload/index.vue tests/e2e/dcc-common-file-preview-source.spec.js` -> PASS。
- GREEN: Playwright CLI 真实路径 -> PASS，`芋道源码 / admin / admin123` 登录后进入 `http://127.0.0.1:8082/dcc/controlled-file/upload`；上传 `dcc-common-preview-sample.txt` 后显示文本内容 `DCC common preview smoke sample`；上传 `dcc-common-preview-sample.unknownbin` 后显示 `当前文件类型仅支持下载，不提供在线预览`。
- RED: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> FAIL, 既有非 DCC 文件 `src/api/showroom-admin/version-center.ts:108`、`:116` 报 `VersionCenterHistoryQuery` / `VersionCenterDetailQuery` 缺少 `Record<string, unknown>` 索引签名。
- GREEN: `pnpm exec eslint src/api/showroom-admin/version-center.ts` -> PASS。
- GREEN: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260524-dcc-any-file-common-preview\frontend-feature-evidence.md` -> PASS。
- GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\yudao-ui-admin-vue3 --task-id 20260524-dcc-any-file-common-preview --mode preview --worktree-closeout off` -> PASS, preview status ready。
- GREEN: `git rebase int_main` in `D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\yudao-ui-admin-vue3` -> PASS, rebased commit `8dff07ca`。
- GREEN: `git merge --ff-only task/dcc-test` in `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` -> PASS, `int_main` advanced to `8dff07ca`。
- GREEN: post-merge `node tests\e2e\dcc-common-file-preview-source.spec.js` -> PASS。
- GREEN: post-merge `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/upload/index.vue tests/e2e/dcc-common-file-preview-source.spec.js src/api/showroom-admin/version-center.ts` -> PASS。
- GREEN: post-merge `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=12288` -> PASS。
- GREEN: post-merge Playwright CLI 真实路径 on `http://127.0.0.1:8083/dcc/controlled-file/upload` -> PASS，文本文件在线预览，未知二进制显示仅下载。
- GREEN: worktree cleanup -> PASS, `D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\yudao-ui-admin-vue3` removed。

## 当前状态

- 状态：completed
- 下一步：无。
