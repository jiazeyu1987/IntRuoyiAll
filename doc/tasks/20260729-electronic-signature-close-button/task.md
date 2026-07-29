# 20260729 Electronic Signature Close Button

## Task Goal

在截图红框所示的电子签名弹窗右上角增加关闭按钮，点击后关闭当前弹窗，不改变确认签名链路。

## Milestones

- [x] 建立任务记录和验收口径。
- [ ] 定位电子签名弹窗组件和现有关闭事件契约。
- [ ] 先补充失败的前端静态合同。
- [ ] 实现右上角关闭按钮。
- [ ] 运行目标验证并记录结果。
- [ ] 收尾清理、经验沉淀、提交并推送。

## Expected Verification

- 静态合同先 RED 后 GREEN，证明弹窗右上角存在关闭按钮并绑定现有关闭事件。
- `pnpm ts:check` 通过或记录与本任务无关的既有阻塞。
- 前端功能证据通过 `frontend-feature-delivery` 校验脚本。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用弹窗既有关闭契约，不新增降级或绕过。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

### Element Plus 全屏弹框挂载门禁

- Trigger: eDHR 填写页最大化后出现签名框或结果弹框。
- Preflight check: 弹框必须作为 `.edhr-fill-workspace` 子树渲染，并保持 `:append-to-body="false"`。
- Blocker: 弹框 teleport 到 body、只靠 `z-index` 或退出全屏规避遮挡。
- Verification: 静态合同断言签名弹框位于全屏容器内部且未 teleport 到 body。
- Forbidden action: 禁止用随机提高 `z-index`、退出全屏后再弹框、隐藏错误提示或关闭全屏能力绕过。
- Evidence: `docs/frontend-development.md#element-plus-全屏弹框挂载门禁`。

## Baseline

- 既有脏工作区基线提交：`e5643370`，保存本任务开始前已有源码、测试和任务文档改动。
- 既有残余证据基线提交：`66777526`，保存提交后残留的 E2E 证据 JSON 差异。
