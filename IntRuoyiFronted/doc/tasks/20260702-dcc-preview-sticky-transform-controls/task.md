# 任务：DCC 预览缩放旋转控制栏吸顶

- Task ID: 20260702-dcc-preview-sticky-transform-controls
- Created: 2026-07-02
- Current Status: completed

## Task Goal

将 DCC 受控文件 PDF / 图片预览的缩放旋转控制栏从页面顶部移动到预览框内部，并在滚动到后续页面时保持预览区内吸顶可见。

## Milestones

1. 确认现有缩放旋转控制栏位置与测试契约。completed
2. 记录 BDD 场景并补静态契约。completed
3. 实现 PDF / 图片预览框内 sticky 控制栏。completed
4. 运行 DCC 预览静态测试与前端类型检查。completed
5. 收尾清理并提交本任务改动。completed

## Expected Verification

- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 已读取 `docs/powershell-memory.md`，PowerShell 中文与文件读写使用 UTF-8。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，本次控制栏保持蓝白运营台紧凑样式。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，将控制栏绑定到预览滚动容器而非页面顶部。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。

## Final Verification Result

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> `PASS`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> `PASS`
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`

## Current Status

completed


## Closeout Cleanup

- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-sticky-transform-controls --mode preview` -> PASS
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260702-dcc-preview-sticky-transform-controls --mode apply` -> PASS
