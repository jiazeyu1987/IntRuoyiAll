# Task: eDHR 右侧红框元信息隐藏

## Task Goal

修复 eDHR 批次执行详情页右侧栏中额外的“填写人 / 提交时间”元信息块显示问题；用户红框标注区域不应显示，单据卡片自身仍保留填写人和打开填写入口。

## Milestones

- [x] M1: 定位红框对应组件与现有静态契约测试。
- [x] M2: 记录 BDD 场景并补充 RED 静态测试。
- [x] M3: 最小化移除红框元信息渲染和无用计算逻辑。
- [x] M4: 运行目标回归验证并记录 GREEN 证据。

## Expected Verification

- RED: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> FAIL，当前组件仍渲染 `edhr-batch-detail__primary-fill-meta`。
- GREEN: `node tests/e2e/edhr-batch-detail-hide-red-box-static.spec.js` -> PASS。
- GREEN: `node tests/e2e/edhr-batch-process-form-card-fillers-static.spec.js` -> PASS，确认每张单据卡片填写人展示不受影响。
- GREEN: `node tests/e2e/edhr-batch-companion-forms-right-panel-static.spec.js` -> PASS，确认右侧表单列表结构不回退。

## Current Status

completed

## Experience Gate

- `docs/experience-index.md`：缺失。
- Gate decision: 本次为低风险前端局部展示修复，不涉及发布、生产数据、远程服务器、数据库写入、权限放宽或破坏性操作；记录缺失但不阻塞实施。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接移除不应显示的右侧栏独立元信息块，保留单据卡片内真实填写人展示。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260724-hide-edhr-primary-fill-meta/bug-regression-evidence.md`
- `doc/tasks/20260724-hide-edhr-primary-fill-meta/frontend-feature-evidence.md`

## Closeout Evidence

- Cleanup preview: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-hide-edhr-primary-fill-meta --mode preview` -> PASS, delete `<none>`, blocked `<none>`。
- Cleanup apply: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260724-hide-edhr-primary-fill-meta --mode apply` -> PASS, deleted_paths `<none>`。
- Worktree: current repo is not a linked worktree; merge and worktree removal skipped。
- Experience consolidation: searched `docs/*memory*.md` and obvious long-term docs (`docs/engineering/technology-stack-routing.md`, `docs/worktree-restrictions.md`, `docs/login-access.md`); no suitable existing destination for this one-off UI cleanup lesson, and no new long-term experience document was created without explicit user authorization。
