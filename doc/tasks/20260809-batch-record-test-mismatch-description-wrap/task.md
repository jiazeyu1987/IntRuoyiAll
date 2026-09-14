# 批记录测试不符合描述完整显示修复

## Task Goal

修复“批记录测试”测试结果弹框中“不符合描述”列在窄桌面视口下被右侧边界裁切的问题，确保长文本在弹框内自然换行并完整显示。

## Milestones

- [x] M1：根据用户截图定位结果弹框、目标列和现有布局约束。
- [x] M2：补充 BDD 与稳定失败的静态回归契约。
- [x] M3：实现最小布局修复并完成定向回归、类型检查和真实页面尺寸验证。
- [x] M4：完成任务清理、经验沉淀检查与最终记录。

## Expected Verification

- `node tests/e2e/batch-record-test-result-mismatch-wrap-static.spec.cjs`：结果弹框必须具备响应式宽度，结果表格列必须在可用宽度内分配，“不符合描述”使用专用换行样式且不继承单行溢出。
- `node tests/e2e/batch-record-test-codex-cli-response-static.spec.cjs`：既有 Codex CLI 回复结构与正式执行链路保持不变。
- `pnpm ts:check`：前端 TypeScript 检查通过。
- Playwright 真实页面验证：在用户截图对应的窄桌面视口检查“不符合描述”单元格 `scrollWidth <= clientWidth`、`scrollHeight <= clientHeight`，右边界不超过弹框内容区，并保存截图；若登录、运行态或目标历史结果缺失则明确阻塞，不用 mock 替代。
- `git diff --check`：本任务改动无空白错误。

## Applicable Experience Gates

- Element Plus 表格长文本换行与固定列边界：表级溢出配置不能让目标列退化为单行省略；目标列需显式关闭 tooltip，使用专用 `white-space: normal`、`overflow-wrap: anywhere` 和 `word-break`，并通过真实 DOM 的 scroll/client 尺寸与边界验证完整显示。
- 前端截图样式块静态契约：静态合同只抽取目标结果表格和目标样式块，避免跨到其它描述列或无关样式。
- Codex Runner 真实回复：保留正式 execution、检查点实际回复与不符合描述数据链路，不新增 mock、默认成功或 API-only 代替页面验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从结果弹框的响应式宽度、表格列宽分配和长文本换行约束解决裁切。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- `output/playwright/20260809-batch-record-test-mismatch-description-wrap/`

## Current Status

completed

全部实现与验证通过。closeout cleanup 已按预览结果删除本任务临时 E2E 脚本、技能证据文件、JSON 和截图，保留 `task.md`、`execution-log.md` 与 `verification-report.md`；当前为主 worktree，无合并或 worktree 删除步骤。
