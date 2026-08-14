# 20260808 一线生产全屏提交确认弹框修复

## Task Goal

修复一线生产页面进入全屏后点击“正式提交”时确认弹框可能被浏览器全屏层覆盖的问题：生产正式提交确认必须在当前全屏容器内部渲染，保留原正式提交校验、不可修改提示和单次写接口，不引入 fallback、mock 或静默降级。

## Milestones

- [x] M1 定位生产全屏根节点、提交按钮和确认链路。
- [x] M2 新增任务专用静态回归 RED，证明生产全屏提交不能依赖全局 MessageBox。
- [x] M3 实现组件内全屏安全确认层，并保持提交接口和错误暴露不变。
- [x] M4 执行目标静态合同、相邻提交/全屏合同、类型检查和 diff 检查。
- [x] M5 归档 evidence，执行 cleanup preview/apply 后完成收尾。

## Expected Verification

- `node tests/e2e/frontline-production-fullscreen-submit-confirm-static.spec.cjs`
- `node tests/e2e/frontline-formal-submit-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `pnpm ts:check`
- `git diff --check -- <本任务文件>`

## Applicable Gates

- 前端静态契约隔离门禁：新增任务专用静态合同，先 RED 再 GREEN，不用无关历史失败替代当前行为验证。
- 前端命令按钮失败必须终止在可见错误边界门禁：提交前置失败仍由页面显示真实原因，不得抛到原生事件处理器或继续发起写请求。
- 前端写入成功与列表刷新失败分层门禁：正式提交仍只在用户明确确认后调用单次正式写接口，不因确认层修复增加重复提交风险。
- 全屏用户路径门禁：全屏态可见弹层必须挂载在当前 fullscreen 元素内部，不能依赖 `body` 上的全局 overlay 或 z-index。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；确认层只是替换渲染位置，提交校验和正式接口失败仍显式暴露。
- `是否从根因和长期维护角度解决`：是；把生产正式提交确认收敛到一线组件内，避免浏览器 fullscreen top layer 遮挡。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

实现、验证、evidence validator 和 task-closeout-cleanup preview/apply 均已完成。真实提交写入 E2E 未在本轮执行，避免在缺少任务自有正式提交数据和清理闭环时误触写接口。
