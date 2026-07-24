# 任务：DCC 预览控制栏滚动容器修复

- Task ID: 20260702-dcc-preview-control-scroll-container-fix
- Created: 2026-07-02
- Current Status: completed

## Task Goal

修复 DCC 受控文件 PDF / 图片预览控制栏仍随页面滚走的问题：滚动到第 2 页及后续页面时，缩放旋转控制栏必须仍在预览区内可见并可操作。

## Milestones

1. 复现并记录 sticky 仍随页面滚走的根因。completed
2. 补充失败契约，要求 PDF / 图片预览使用有高度约束的内部滚动容器。completed
3. 将可变换预览框改为内部滚动视口，控制栏在该视口内吸顶。completed
4. 运行 DCC 预览静态测试、类型检查与收尾清理。completed
5. 提交本次修复，保留无关脏改不动。completed

## Expected Verification

- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`
- `git diff --check`

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 已读取 `docs/powershell-memory.md`，PowerShell 中文与文件读写使用 UTF-8。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，控制栏继续保持 IntPP 蓝白运营台紧凑样式。
- 已读取 `bug-regression-fix-loop` 与 bug evidence contract，本次按回归修复记录 RED/GREEN。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，根因是预览框未成为实际滚动容器，导致 sticky 绑定到不滚动的祖先后仍随页面滚走；修复为 PDF / 图片预览框提供内部滚动视口。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。

## Final Verification Result

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> `PASS`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> `PASS`
- `git diff --check` scoped to DCC files -> `PASS`
- `http://localhost:8081/src/views/dcc/controlled-file/view/index.vue` -> served source contains `protected-viewer-frame--transformable`
- Vite style module -> served CSS contains `max-height: calc(100vh - 180px)` and `overscroll-behavior: contain`
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`

## Current Status

completed
