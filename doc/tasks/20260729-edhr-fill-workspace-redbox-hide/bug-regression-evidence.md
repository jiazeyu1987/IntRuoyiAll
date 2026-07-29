# Bug Regression Evidence

## Bug Summary And Expected Behavior

- Bug: eDHR 填写工作区点击最大化后，左侧保存草稿成功结果弹框、提交执行签名弹框和提交结果弹框会被浏览器全屏层遮挡。
- Expected: 最大化时这些弹框仍显示在当前填写工作区上方，用户可完成电子签名、确认保存结果或确认提交结果。

## Reproduction Command Or Path

- Static reproduction: `node tests/e2e/edhr-fill-workspace-static.spec.js`
- User path: 打开 eDHR 执行填写页，点击左侧“最大化”，再点击“保存草稿”或“提交执行”。

## Root Cause

- `ExecutionPage.vue` 使用浏览器 `requestFullscreen()` 让 `.edhr-fill-workspace` 进入全屏。
- 保存/提交相关 `el-dialog` 原先渲染在全屏元素外部，浏览器全屏只显示全屏元素及其子树，导致弹框在最大化状态下被遮挡。

## Regression Test Added Or Updated

- Updated: `IntRuoyiFronted/tests/e2e/edhr-fill-workspace-static.spec.js`
- Coverage: 断言提交签名弹框和保存/提交结果弹框位于 `.edhr-fill-workspace` 内部，并显式使用 `:append-to-body="false"`，避免 teleport 到 body。

## RED

- RED: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> FAIL，预期失败；提交执行签名弹框仍在全屏填写工作区外部。

## GREEN

- GREEN: `node tests/e2e/edhr-fill-workspace-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-fill-workspace-hide-side-panels-static.spec.js` -> PASS
- GREEN: `node tests/e2e/edhr-assist-fill-mode-static.spec.js` -> PASS
- GREEN: `pnpm ts:check` -> PASS

## Risk And Regression Scope

- Risk: 仅改变保存/提交弹框挂载位置，不改变保存草稿、提交执行、密码校验、结果展示或 API 调用逻辑。
- Regression scope: eDHR 填写页左侧控制栏、辅助模式切换弹框、全屏按钮、保存结果弹框和提交签名弹框。

## Blockers And Follow-Up Actions

- Real E2E: 本轮未启动本地服务或真实登录验证；静态合同锁定 DOM 挂载根因，`pnpm ts:check` 已通过。
- Follow-up: 若需要截图级验收，可在已确认本地 8081/48081 运行态后追加 Playwright 最大化点击路径。
