# 20260808 DCC 受控浏览问题确认

## Task Goal

确认用户报告的 3 类受控浏览问题是否在当前本机运行态存在：

- 会话失效边界筛选标签与表格数据不同步。
- `预览` 与文件名称按钮点击无反馈。
- 分页 `前往` 输入框 Enter 不跳转且保留错误页码。

只读范围：文控中心 > 受控浏览；不确认下载、不保存、不提交、不删除、不修改权限或业务数据。

## Milestones

- [x] 创建验证任务记录并读取适用门禁。
- [x] 确认本机前端、后端与登录前置。
- [x] 用真实页面只读路径复核三类问题。
- [x] 输出 verification-report.md，记录 PASS/FAIL/BLOCKED 结论与证据。

## Expected Verification

- Playwright 真实页面路径，入口为 `http://127.0.0.1:8081` 或 `http://localhost:8081`。
- 记录租户/账号标签、目标菜单、只读筛选范围、目标请求、页面状态、弹窗/提示、URL 与表格数据。
- 若登录、账号、运行态或目标菜单不可用，按项目规则记录 BLOCKED，不使用 API-only 或 mock 替代真实页面确认。

## Applicable Gates

- `docs/e2e-rules.md#DCC 受控浏览当前有效版与权限隔离门禁`：真实页面验收必须记录请求范围、目录、状态、类别、当前有效文件与页面可见行。
- `docs/e2e-rules.md#Playwright 目标链路与外部资源异常归因门禁`：区分目标业务请求、非目标请求、console/pageerror 和写请求数量。
- `docs/e2e-rules.md#Playwright 浏览器可执行文件门禁`：Playwright 浏览器缺失时优先使用本机 Chrome/Edge，并记录来源。
- `docs/e2e-rules.md#Playwright 快照与 daemon 收尾门禁`：不得提交或回显含凭据的 Playwright 快照，任务结束清理本任务自有敏感 artifact。
- `docs/login-access.md`：登录失败必须报告目标入口、租户/用户标签和接口响应，不得静默切换账号/租户/后端。
- `docs/local-runtime.md` 与 `docs/worktree-restrictions.md`：本机 int_main 固定使用前端 8081、后端 48081。

## Current Status

completed - 已完成只读真实页面确认：会话失效筛选标签与旧表格混合状态已复现；用户指定 0 QM 首行预览/文件名无反馈未复现；全域分页 jumper Enter 不跳转未复现，且当前本机总数为 15,917、末页 796，并非报告中的 31,370、末页 1,569。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务为只读确认，不用 API-only/mock 替代页面复核。
- `是否从根因和长期维护角度解决`：否。本轮只确认问题是否存在，不做修复。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- doc/tasks/20260808-dcc-browser-issues-confirmation/session-result.json
- doc/tasks/20260808-dcc-browser-issues-confirmation/preview-result.json
- doc/tasks/20260808-dcc-browser-issues-confirmation/pagination-preview-result.json
