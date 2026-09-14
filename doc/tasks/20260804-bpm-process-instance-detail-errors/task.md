# 20260804 BPM 流程实例详情页报错修复

## Task Goal

修复使用本机 `芋道源码/admin` 访问 `/bpm/process-instance/detail?id=c1cd2ae6-8fbf-11f1-a00f-00155d2984a0` 时出现的 3 个 BPMN 高亮错误，保持真实 BPM 详情页可打开、流程图高亮稳定、错误对用户可见且不吞异常。

## Milestones

- [x] 复现并记录 3 个报错的控制台、页面和网络证据。
- [x] 编写或更新最小回归测试，先证明现有缺陷 RED。
- [x] 实施最小根因修复，不引入 fallback、降级或吞异常。
- [x] 运行聚焦后端回归测试与前端 marker 守护静态验证，并记录 GREEN/REGRESSION。
- [ ] 完成真实 8081 页面复验、清理、提交和推送收尾。

## Expected Verification

- Playwright 真实页面登录并访问目标详情页，记录目标链路无新增 pageerror/console error。
- 聚焦静态合同覆盖 BPMN marker 高亮安全性：调用 `canvas.addMarker/removeMarker` 前必须用 `elementRegistry.get(id)` 校验节点存在。
- 后端回归测试覆盖 BPMN 模型视图响应只返回当前 BPMN XML 中真实存在的任务节点和连线 ID。
- 若源代码涉及 Vue/TypeScript，运行相关前端静态测试；可行时运行 `pnpm ts:check` 或记录无关阻塞。

## Applied Experience Gates

### 前端 BPMN marker 高亮完整性门禁

- Trigger: BPMN/BPM 流程图、审批流程图、`canvas.addMarker`、`canvas.removeMarker`、`elementRegistry.get`、`Cannot read properties of undefined (reading 'markers')`、节点高亮、节点缺失。
- Preflight check: 对后端或流程实例返回的每个高亮节点 ID，先通过 `elementRegistry.get(id)` 确认当前 BPMN XML 中存在该元素，再调用 `canvas.addMarker/removeMarker`。
- Blocker: 任一 marker 操作直接对未经校验的 ID 调用、缺失节点被静默忽略、页面只在控制台报错但用户不可见、或静态契约无法证明缺失节点不会触发 `markers` pageerror。
- Verification: 聚焦静态契约必须断言安全 marker helper、`elementRegistry.get` 校验、可见 warning `data-testid`、无直接未校验 `canvas.addMarker/removeMarker`。
- Forbidden action: 禁止用 try/catch 吞掉 `markers` 异常、隐藏流程图、禁用全部高亮冒充修复，或把 BPMN XML 与审批任务节点不一致解释为前端无责任而不提示用户。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，后端 BPMN 模型视图响应按当前 BPMN XML 过滤不存在的 marker 节点 ID，前端保留可见 warning 和安全 marker helper。
- `是否存在临时补丁或绕过`：否。

## Current Status

blocked_on_environment_closeout

代码修复和聚焦验证已完成。真实 8081 页面复验被本地运行态前置条件阻塞：8081/48081 当前未监听，且主工作区存在大量非本任务脏改动，按本地运行态规则不得从脏主工作区重打运行 Jar 冒充本任务验证。提交/推送同样被当前分支已有非本任务 ahead/dirty 状态阻塞。
