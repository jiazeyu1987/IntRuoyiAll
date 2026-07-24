# 20260524 DCC 任意文件上传与常见文件预览 - 前端

## 任务目标

- DCC 上传页面明确支持上传任意单个文件，不使用前端扩展名白名单阻断。
- DCC 受控预览组件支持 PDF、图片、文本/代码、Office、音频、视频。
- Office 配置缺失时展示后端返回的明确不可预览原因，不伪造预览、不静默降级。
- 未知类型显示“仅支持下载”的明确状态。

## 前序任务检查

- 前端前序 DCC 任务：
  - `doc/tasks/20260523-dcc-nas-transfer-large-folder-frontend/task.md`
  - 状态：`Completed on 2026-05-23`
- 结论：前序任务已完成，不阻塞本任务。

## BDD 场景

- BDD: 上传任意单文件 -> Given 用户进入 DCC 上传页面 / When 选择任意类型单文件 / Then 前端不因 MIME 或扩展名阻止选择，并调用后端预览上传。
- BDD: 媒体文件在线预览 -> Given 后端返回 `VIDEO` 或 `AUDIO` 预览类型 / When 受控预览组件加载文件 / Then 页面使用浏览器原生只读播放器展示，并叠加受控水印。
- BDD: 未知类型明确不可预览 -> Given 后端返回 `DOWNLOAD_ONLY` / When 预览组件渲染 / Then 页面明确提示当前类型仅支持下载，不尝试伪造预览。

## 里程碑

- [x] M1：建立任务文档和 BDD/TDD 证据框架。
- [x] M2：补充前端失败验证脚本。
- [x] M3：扩展前端预览类型、上传文案和类型声明。
- [x] M4：运行前端定向验证、ESLint 和真实浏览器冒烟。
- [x] M5：运行 evidence 校验、closeout 预览；全量类型检查通过后提交。

## 预期验证

- `node tests\e2e\dcc-common-file-preview-source.spec.js`
- `pnpm exec eslint src/api/dcc/controlledFile/workflow.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/upload/index.vue tests/e2e/dcc-common-file-preview-source.spec.js`
- Playwright CLI 真实路径：登录 `芋道源码 / admin / admin123`，进入 `http://127.0.0.1:8082/dcc/controlled-file/upload`，上传文本文件与未知二进制文件。
- `pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260524-dcc-any-file-common-preview\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\yudao-ui-admin-vue3 --task-id 20260524-dcc-any-file-common-preview --mode preview --worktree-closeout off`

## 当前状态

- 状态：completed
- 当前阶段：DCC 定向验证、真实浏览器冒烟、全量 `pnpm ts:check`、提交、rebase、快进合并和合并后验证均已完成。
- 收尾：按 closeout 清理规则已移除临时 evidence 文件，验证结果保留在 `execution-log.md`。
- Worktree：`D:\ProjectPackage\Int\IntRuoyi\worktrees\dcc-test\yudao-ui-admin-vue3` 已删除。

## Current Status

completed
