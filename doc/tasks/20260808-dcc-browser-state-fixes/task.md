# 20260808 DCC 受控浏览状态一致性修复

## Task Goal

修复 DCC 受控浏览中三个用户可见问题：

- 鉴权失败时不得提交新筛选标签、URL 或伪装成新筛选结果，必须清空或标记旧数据。
- `预览` 与文件名称入口必须共用安全打开逻辑，打开失败时给出明确提示。
- 分页 `前往` 输入框 Enter 必须显式跳转；跳转失败或非法页码必须恢复真实当前页。

## Milestones

- [x] 创建修复任务记录并读取适用门禁。
- [x] 写入最小回归测试并完成 RED 证据。
- [x] 实现前端正式修复。
- [x] 完成 GREEN 与相关回归验证。
- [x] 输出 verification-report.md 并完成收尾清理。

## Expected Verification

- 聚焦静态/行为合同覆盖：鉴权失败不提交筛选状态且清空/标记旧表格；预览使用安全打开并提示失败；分页 jumper Enter 成功跳转或失败回滚。
- 前端类型/静态回归在当前任务范围内通过；若全量命令遇到无关历史 blocker，按门禁记录并保留聚焦验证结论。
- 可用本机运行态时，使用 Playwright 真实页面只读验证核心受控浏览路径，DCC 写请求数量为 0。

## Applicable Gates

- `docs/frontend-development.md`：请求失败必须通过 UI/测试明确暴露，不得吞异常或默认成功。
- `docs/e2e-rules.md#DCC 受控浏览当前有效版与权限隔离门禁`：受控浏览只读交互缺陷需区分运行态复现和源码风险，记录写请求为 0。
- `docs/e2e-rules.md#Element Plus 表单值断言门禁`：分页 jumper 等 Element Plus 输入值必须读取真实 input value。
- `docs/task-closeout-rules.md`：任务完成后保留 task/execution/verification，并运行 cleanup preview/apply。

## Current Status

completed - 用户追加的只读 Playwright E2E、证据归档和 cleanup apply 已完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。请求失败必须明确暴露，不用旧数据或默认成功掩盖。
- `是否从根因和长期维护角度解决`：是。按状态提交顺序、失败状态显示、安全预览和分页输入同步根因修复。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates

- doc/tasks/20260808-dcc-browser-state-fixes/readonly-real-regression.cjs
- doc/tasks/20260808-dcc-browser-state-fixes/readonly-real-regression-result.json
- doc/tasks/20260808-dcc-browser-state-fixes/bug-regression-evidence.md
- doc/tasks/20260808-dcc-browser-state-fixes/frontend-feature-evidence.md
- doc/tasks/20260808-dcc-browser-state-fixes/dcc-browser-state-real-e2e.cjs
- doc/tasks/20260808-dcc-browser-state-fixes/dcc-browser-state-real-e2e-result.json
