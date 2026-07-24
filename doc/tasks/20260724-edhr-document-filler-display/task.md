# Task: eDHR 右侧单据填写人显示

## Task Goal

在 eDHR 批次执行详情页右侧单据列表中，为每个主生产表和动态表单卡片显示该单据的填写人，帮助用户直接判断每个单据由谁负责或已填写。

## Milestones

- [x] M1: 定位右侧单据卡片组件、数据来源和现有测试入口。
- [x] M2: 记录 BDD 场景并补充 RED 静态契约测试。
- [x] M3: 最小化修改前端组件和类型展示填写人。
- [x] M4: 运行目标测试和结构校验，记录 GREEN 证据。

## Expected Verification

- RED: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> FAIL，右侧每张单据卡片缺少填写人元信息。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-admin-filler-visibility-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-review-summary-right-rail-static.spec.js` -> PASS。
- BROADER CHECK: `pnpm ts:check` -> FAIL in unrelated `src/views/dcc/controlled-file/browser/index.vue` existing ID type mismatch; no eDHR errors reported before failure.
- GREEN: 前端特性证据文件通过 `frontend-feature-delivery` 校验脚本。

## Current Status

completed

## Experience Gate

- `docs/experience-index.md`：缺失。
- Gate decision: 本次为低风险前端局部展示修复，不涉及发布、生产数据、远程服务器、数据库写入、权限放宽或破坏性操作；记录缺失但不阻塞实施。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，优先复用后端返回的真实单据责任/填写字段，不通过创建人或默认当前用户推断。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260724-edhr-document-filler-display/frontend-feature-evidence.md`

## Closeout Evidence

- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-edhr-document-filler-display --mode preview` -> PASS, delete `<none>`, blocked `<none>`。
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-edhr-document-filler-display --mode apply` -> PASS, deleted_paths `<none>`。
- Worktree: current repo is not a linked worktree; merge and worktree removal skipped。
- Experience consolidation: searched `docs/*memory*.md` and obvious `docs/` long-term docs; no suitable existing memory document found, and no new long-term experience file was created without explicit user authorization。
