# 任务：DCC 预览控制栏 2x2 按钮布局

- Task ID: 20260702-dcc-preview-transform-controls-grid
- Created: 2026-07-02
- Current Status: completed

## Task Goal

将 DCC 受控文件 PDF / 图片预览的缩放旋转控制栏改为 2 行 4 个按钮：放大、缩小、旋转、复原；其中旋转固定为右旋 90 度，复原恢复 100% 缩放与 0 度旋转。

## Milestones

1. 确认现有控制栏按钮与契约。completed
2. 补充 2x2 控制栏 RED 静态契约。completed
3. 实现 PDF / 图片控制栏 2x2 布局与复原按钮。completed
4. 运行 DCC 静态测试、类型检查与本地运行态确认。pending
5. 收尾清理并提交本任务改动。pending

## Expected Verification

- `node tests/e2e/dcc-common-file-preview-source.spec.js`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js`
- `pnpm ts:check`
- `git diff --check`

## 经验门禁

- 已读取 `docs/experience-index.md`。
- 已读取 `docs/powershell-memory.md`，PowerShell 中文与文件读写使用 UTF-8。
- 已读取 `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`，按钮保持蓝白运营台紧凑样式。
- 已读取 `frontend-feature-delivery` 与前端证据契约，本次按前端行为切片记录 RED/GREEN。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，直接调整受控预览组件按钮结构与状态操作，不添加临时兼容分支。
- 是否存在临时补丁或绕过：否。

## Current Blockers

- 暂无。

## Current Status

in_progress


## Final Verification Result

- `node tests/e2e/dcc-common-file-preview-source.spec.js` -> `PASS`
- `node tests/e2e/dcc-controlled-file-protection.contract.test.js` -> `PASS`
- `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192` -> `PASS`
- `validate_frontend_feature.py` -> `PASS`
- Local Vite served source/style markers -> `PASS`

## Current Status

completed
