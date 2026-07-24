# Task: DCC 目录管理导入按钮文案改为 NAS同步

## Goal

将 `DCC目录管理` 页面现有“从 IntAuth 导入目录树”按钮的前端显示文案改为“NAS同步”，只改文字描述，不改现有点击行为、接口调用和权限控制；同时只读盘点 `IntAuth` 仓库中是否已有与 NAS 交互的实现代码。

## Scope

- 修改 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\dcc\controlled-file\directories\index.vue` 中导入按钮的显示文案。
- 为该按钮文案补充任务范围内的最小回归校验脚本。
- 只读检索 `D:\ProjectPackage\Int\IntAuth` 中与 NAS 交互、目录同步相关的前后端代码位置并记录结果。

## Out Of Scope

- 不修改导入按钮绑定的处理函数、接口路径、权限点、禁用条件或成功提示。
- 不改动 `IntAuth` 代码。
- 不新增 fallback、mock、兼容分支或临时 UI。

## Previous Task Check

- Previous frontend task: `doc/tasks/20260520-login-remove-secondary-sections/task.md`
- Status before this task: blocked.
- Blocker summary: 上一任务已在文档中明确记录为“等待删除范围确认”，属于已显式阻塞状态。
- Impact: 当前任务为独立的 DCC 文案调整，不依赖上一任务继续推进，可在不触碰其作用域的前提下开始。

## Milestones

- [x] M1: 定位按钮实现位置，确认同仓前置任务状态，并创建当前任务文档与执行日志。
- [x] M2: 先写按钮文案失败校验并记录 RED 证据。
- [x] M3: 将按钮显示文案改为 `NAS同步`，保持原交互逻辑不变。
- [x] M4: 运行验证并整理 `IntAuth` 中 NAS 交互代码位置。
- [x] M5: 更新任务文档与证据，执行 closeout cleanup preview，准备任务范围内提交。

## Expected Verification

- `node --test D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\scripts\\dcc-directory-nas-sync-label.test.mjs`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm.cmd run ts:check -- --pretty false`
- `python C:\\Users\\BJB110\\.codex\\skills\\frontend-feature-delivery\\scripts\\validate_frontend_feature.py --evidence D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3\\doc\\tasks\\20260520-dcc-directory-nas-sync-label\\frontend-feature-evidence.md`
- `python C:\\Users\\BJB110\\.codex\\skills\\task-closeout-cleanup\\scripts\\task_closeout.py --workspace D:\\ProjectPackage\\Int\\IntRuoyi\\yudao-ui-admin-vue3 --task-id 20260520-dcc-directory-nas-sync-label --mode preview`

## Current Status

Completed. 文案修改、回归测试、类型检查、`IntAuth` NAS 代码盘点、证据校验与 cleanup preview 已完成。

## IntAuth NAS Inventory

- 前端知识目录页提供 NAS 目录导入入口：`fronted/src/pages/KnowledgeBases.js`
- 前端知识目录 API 提供按路径导入与结构文件导入：`fronted/src/features/knowledge/api.js`
- 后端公开接口提供 `POST /api/knowledge/directories/import-from-path` 与 `POST /api/knowledge/directories/import-from-structure-file`：`backend/app/modules/kb_config/router.py`
- 后端服务会校验 `source_path` 非空、存在且必须是目录，并使用 `Path(clean_source_path)` 读取目录树：`backend/services/intdcc_kb_read_service.py`
- `IntDCC` 与 `IntAuth` 之间还存在内部目录同步接口 `POST /internal/dcc/knowledge/directories/sync`：`backend/app/modules/internal_dcc/router.py`

## Final Verification Result

- `node --test .\\scripts\\dcc-directory-nas-sync-label.test.mjs`：PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; npm.cmd run ts:check -- --pretty false`：PASS
- `validate_frontend_feature.py`：PASS
- `task_closeout.py --mode preview`：PASS，preview 仅识别 `frontend-feature-evidence.md` 为可清理候选
