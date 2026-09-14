# 20260729 已链接单元格点击与绿色联动边框

## Task Goal

修复填写配置里的辅助表单映射交互：已经建立链接的单元格仍可点击；点击后当前选中的原表单单元格和辅助表单中被链接的单元格同时显示绿色边框。

## Milestones

1. 建立任务记录、读取前端与 worktree 门禁，并确认适用经验。
2. 用静态合同复现已链接单元格不可点击、绿色联动边框缺失的问题。
3. 修改前端交互与样式，保持未映射、已映射、当前选中状态语义清晰。
4. 运行目标静态合同和相关回归验证。
5. 合并回 `int_main` 并完成任务收尾记录。

## Expected Verification

- `node tests/e2e/edhr-fill-config-linked-cell-click-green-border-static.spec.js`
- `node tests/e2e/edhr-visual-fill-config-static.spec.js`
- `node tests/e2e/edhr-fill-config-redbox-hide-static.spec.js`
- `pnpm ts:check` 如遇无关历史失败，记录 blocker 并保留聚焦合同结果。

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；直接修正已链接单元格的点击状态和选中态样式。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- Worktree 已创建在 `D:\IntRuoyiWorktree\linked-cell-click-green-border`，分支 `codex/20260729-linked-cell-click-green-border`。
- Worktree runtime slot 已登记：`slot=13`，frontend `8094`，backend `48094`。
- 适用经验门禁：`docs/frontend-development.md` 的前端静态合同隔离门禁、`docs/e2e-rules.md` 的静态合同与真实 E2E 同步门禁；本次为窄范围点击/选中态修复，使用聚焦静态合同验证。

## Closeout Result

- 任务实现已推送到 `origin/codex/20260729-linked-cell-click-green-border`。
- 已通过 `git push origin HEAD:int_main` 快进融合到 `origin/int_main`，融合后远端 HEAD 为 `3db1af3cec387a44b95e4d7acb1cf5c3bf225395`。
- `task_closeout.py --mode preview --worktree-closeout off` -> ready，无阻塞。
- `task_closeout.py --mode apply --worktree-closeout off` -> applied；保留核心任务记录，清理附属证据文件。
