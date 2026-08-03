# Bug Regression Evidence

## Bug

受控文件提交页选择“文件分类”后，继续选择文件类别或触发目录加载时可能报错 `Controlled file category does not exist`。

## Expected

前端必须区分“文件分类”`fileTypeTaxonomyId` 和 DCC 正式“文件类别”`categoryId`。文件类别下拉只能使用当前 taxonomy 分支下可上传且已绑定目录的正式类别；文件分类切换后必须清空旧类别和目录上下文，不得把 stale 或跨分类 ID 发送给后端。

## Reproduction

静态复现命令：`node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js`。修复前契约缺少 taxonomy 分支过滤和切换清理逻辑，能够证明旧页面存在跨链路类别 ID 风险。

## Root Cause

上传页同时维护 taxonomy 分类和 DCC 类别。后端 `getUploadDirectoryTree(categoryId)` 只接受 DCC 正式类别 ID，并会 fail fast 抛出 `Controlled file category does not exist`。旧前端未把类别下拉严格收敛到当前 taxonomy 分支，也未在 taxonomy 切换时统一清理旧 `categoryId` 和目录上下文，导致可能提交不再有效的类别 ID。

## RED

- RED: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> FAIL, expected reason: 缺少 `selectedFileTypeTaxonomyCategoryIds`、`availableCategories` taxonomy 过滤、以及 `handleFileTypeTaxonomyChange()` 的类别/目录/预览清理。

## GREEN

- GREEN: `node tests/e2e/dcc-upload-category-taxonomy-binding-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-category-permission-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-project-taxonomy-revision-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/dcc-upload-name-version-autofill-static.spec.js` -> PASS。
- GREEN: `pnpm ts:check` -> PASS，进程 `21862` 退出码 0。

## Verification

定向静态契约覆盖 taxonomy-bound 类别过滤、文件分类切换清理、上传类别权限投影、项目 taxonomy 修订联动和上传文件名/版本自动填充相邻回归。`git diff --check` 对本任务路径通过。

## Blockers

Real Playwright E2E 未运行，因为未确认本地前后端运行态、登录账号、测试租户和可写测试数据。当前主工作区存在非本任务脏改动且分支已领先 `origin/int_main`，无法安全完成独立提交、推送和 `completed` 状态。
