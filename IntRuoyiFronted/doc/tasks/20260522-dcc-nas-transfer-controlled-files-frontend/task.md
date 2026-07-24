# 任务：DCC NAS目录转移前端实现

## Goal

在当前 `NAS管理` 与 `DCC受控文件上传/预览` 前端中完成 NAS 转移入口、模板类别选择、生效日期输入、批量结果展示，以及非 PDF 上传/预览分流。

本任务必须满足：

- `NAS管理` 页新增 `转移` 按钮与转移对话框
- 仅在已进入选择模式且至少选中一个 NAS 目录时激活 `转移`
- 转移对话框支持模板类别选择、统一生效日期与固定规则说明
- 提交后显示批量结果汇总与失败明细
- `DCC受控文件上传` 页支持非 PDF 文件选择
- `DCC` 预览/详情页支持 `PDF / IMAGE / TEXT / OFFICE / DOWNLOAD_ONLY` 预览分流

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\upload\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\view\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\system\nas\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\dcc\controlledFile\**`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\system-nas-management.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-dcc-nas-transfer-controlled-files-frontend\**`

## Non-Scope

- 不修改 showroom 前端
- 不修改系统管理其他业务页面结构
- 不新增与本任务无关的菜单项

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-system-nas-directory-selection-frontend\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 当前 NAS 管理页已有选择模式与导出占位，本任务在其基础上补真实转移入口，不回退已有功能。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在多处与 showroom 相关的用户改动
- Impact: 本任务仅修改 NAS 管理、DCC 上传预览、相关 API 与本任务文档，避免混入无关文件

## Milestones

- [x] M1: 创建任务文档并确认上一同仓 NAS 任务完成状态
- [x] M2: 记录 BDD 与 RED，锁定 NAS 转移入口、结果展示、非 PDF 上传与预览分流契约
- [x] M3: 实现 NAS 管理页转移对话框与结果展示
- [x] M4: 实现 DCC 上传与预览页面的非 PDF 分流
- [x] M5: 跑前端定向验证、证据校验和 closeout preview

## Expected Verification

- `node --test scripts\system-nas-management.test.mjs scripts\dcc-controlled-file-nonpdf-preview.test.mjs`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm exec eslint src/views/system/nas/index.vue src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue src/api/dcc/controlledFile/workflow.ts scripts/system-nas-management.test.mjs scripts/dcc-controlled-file-nonpdf-preview.test.mjs --format stylish`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260522-dcc-nas-transfer-controlled-files-frontend/frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-dcc-nas-transfer-controlled-files-frontend --mode preview`

## Current Status

Completed on 2026-05-23. 前端转移入口、批量结果展示、非 PDF 预览分流、定向测试、证据校验和 closeout preview 已完成；当前仅剩仓外 `OnlyOffice` 运行环境不可访问这一联调缺口，不阻塞当前任务代码提交。

## Final Verification Result

- `node --test scripts/system-nas-management.test.mjs scripts/dcc-controlled-file-nonpdf-preview.test.mjs` -> PASS
- `pnpm exec eslint src/views/system/nas/index.vue src/views/dcc/controlled-file/upload/index.vue src/views/dcc/controlled-file/upload/submitter.ts src/views/dcc/controlled-file/view/index.vue src/views/dcc/controlled-file/view/OnlyOfficeReadOnlyViewer.vue src/api/dcc/controlledFile/workflow.ts scripts/system-nas-management.test.mjs scripts/dcc-controlled-file-nonpdf-preview.test.mjs --format stylish` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260522-dcc-nas-transfer-controlled-files-frontend/frontend-feature-evidence.md` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-dcc-nas-transfer-controlled-files-frontend --mode preview` -> READY

## Residual External Gap

- 本地无可访问的 OnlyOffice 服务，因此 `OFFICE` 只读预览组件暂时无法完成浏览器侧实机渲染验证。
- `vue-tsc` 仍报 showroom 既有文件错误：`src/views/showroom-admin/narration/NarrationWorkspace.vue`、`src/views/showroom-frontstage/shared/payload.ts`；当前任务自有 NAS / DCC 文件未新增类型错误。
