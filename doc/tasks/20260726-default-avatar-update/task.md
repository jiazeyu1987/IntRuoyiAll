# 20260726-default-avatar-update

## Task Goal

将前端用户默认头像替换为用户提供的新 PNG 图标，并确保所有默认头像兜底引用一致指向新资源。

## Milestones

- [x] 建立默认头像静态契约，先验证旧实现会失败。
- [x] 添加新默认头像资源并更新默认头像引用。
- [x] 运行最小静态验证并记录结果。

## Expected Verification

- `node IntRuoyiFronted/tests/e2e/default-avatar-asset-static.spec.js`
- 受影响文件仅限默认头像资源、默认头像引用、任务文档和对应静态契约。

## Current Status

ready_for_closeout

## Closeout Notes

- `task-closeout-cleanup` preview/apply 已通过，未删除文件。
- 按项目提交规则，当前工作区存在大量本任务外脏改动；本任务未执行提交或推送，避免将无关任务内容混入本次变更。
- 经验沉淀检查未发现需要新增的长期经验；现有 `docs/frontend-development.md#前端静态契约隔离门禁` 已覆盖本任务采用的验证方式。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，统一默认头像资源引用，避免局部替换导致默认头像不一致。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 命中 `docs/frontend-development.md#前端静态契约隔离门禁`：本任务使用任务专用最小静态契约验证默认头像资源和引用，避免被无关历史前端检查阻塞。

## Cleanup Keep

- doc/tasks/20260726-default-avatar-update/frontend-feature-evidence.md
