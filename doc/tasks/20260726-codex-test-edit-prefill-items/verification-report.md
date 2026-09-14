# Verification Report

## Scope

- 修复测试管理页点击“修改”时的方法项和目标项回显。
- 保持后端 API 和保存 payload 契约不变。

## Implementation

- 修改按钮从 `openEdit(row.id)` 改为 `openEdit(row)`。
- 新增 `applyCaseFormForEdit(data)` 集中回显表单数据。
- 编辑时通过 `parseMethodItems(data.methodText)` 将方法文本拆成逐条方法项。
- 编辑时通过 `normalizeCheckpointItems(data.checkpoints)` 将目标项排序、拆分并逐条回显。
- 历史 `expectedText` 多行内容会被拆成多条目标项，便于按当前测试项逐条修改。

## Verification

- RED: `pnpm e2e:system:codex-test-management:static` -> FAIL，失败在修改按钮未传当前行、缺少回显归一化函数。
- GREEN: `pnpm e2e:system:codex-test-management:static` -> PASS。
- GREEN: `pnpm ts:check` -> PASS。
- GREEN: `git diff --check -- IntRuoyiFronted/src/views/system/codex-test-management/index.vue IntRuoyiFronted/tests/e2e/system-codex-test-management-static.spec.js doc/tasks/20260726-codex-test-edit-prefill-items/task.md doc/tasks/20260726-codex-test-edit-prefill-items/execution-log.md` -> PASS，仅有 CRLF 工作区提示。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260726-codex-test-edit-prefill-items/frontend-feature-evidence.md` -> PASS。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-codex-test-edit-prefill-items --mode preview` -> PASS，无删除项。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260726-codex-test-edit-prefill-items --mode apply` -> PASS，未删除任何文件。

## Risks

- 未启动真实前后端服务，未执行写入型 Runner E2E；本次为窄范围前端编辑回显修复，使用静态合同和类型检查验证。
- 工作区存在大量非本任务脏改动，且本任务涉及文件已有同文件并行改动；为避免混入其它任务，本任务不提交/推送，状态保留为 `ready_for_closeout`。
