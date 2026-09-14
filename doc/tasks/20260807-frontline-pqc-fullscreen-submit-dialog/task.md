# 一线PQC全屏提交签名弹框覆盖验证

## Task Goal

确认一线PQC进入浏览器全屏后，点击“提交”打开的电子签名弹框仍位于全屏根节点内部、覆盖填写内容且不会被浏览器全屏层或页面其它层遮挡；补充稳定回归合同和真实只读页面验证证据。

## Milestones

- [x] 记录全屏提交弹框的BDD场景与验证边界
- [x] 补充全屏根节点与签名弹框层级的静态回归合同
- [x] 通过真实PQC页面完成只读全屏弹框可见性验证
- [x] 更新验证报告并完成任务收尾

## Expected Verification

- `node tests\e2e\frontline-pqc-fullscreen-submit-dialog-static.spec.cjs`
- `node tests\e2e\frontline-pqc-formal-submit-static.spec.js`
- `node tests\e2e\edhr-frontline-pqc-fullscreen-toggle-static.spec.cjs`
- `node tests\e2e\mes-frontline-pqc-fullscreen-preload-static.spec.js`
- `pnpm ts:check`
- 真实 Playwright 只读路径：登录 -> 打开一线PQC -> 最大化 -> 点击提交 -> 检查签名弹框
- `git diff --check`

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；只增加结构化定位标识和验证合同，不改变错误处理或弹框行为。
- `是否从根因和长期维护角度解决`：是；通过全屏根节点后代关系和层级样式建立可持续回归门禁。
- `是否存在临时补丁或绕过`：否。

## Verification Boundary

- 只读验证不调用正式PQC提交接口，不填写或提交签名密码，不写入业务数据。
- 浏览器全屏必须由真实页面的最大化按钮触发，不能通过脚本直接伪造 `document.fullscreenElement`。
- 缺少正式待检工单或正式填写上下文时，记录为真实E2E前置阻塞，不使用mock数据替代。

## Cleanup Keep

- doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/pqc-fullscreen-submit-dialog-real-check.cjs
- doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/evidence/pqc-fullscreen-submit-dialog-real-check.json
- doc/tasks/20260807-frontline-pqc-fullscreen-submit-dialog/evidence/pqc-fullscreen-submit-dialog-real-check.png
