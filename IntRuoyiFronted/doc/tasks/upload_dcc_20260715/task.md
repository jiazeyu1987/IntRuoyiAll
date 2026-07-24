# upload_dcc_20260715 前端开发任务

## Task Goal

在 `upload_dcc_20260715` 前端 worktree 中完成电子文控系统受控文件上传流程前端交互开发与验证，最终随后端合并进 `int_main`，并支持合并后的真实数据 E2E 验证。

## Milestones

- [completed] M1 任务记录、经验门禁和前端 RED 测试
- [completed] M2 上传页现行版本面板、变更方式和编号联动
- [completed] M3 文控归档前部门勾选交互与 payload
- [in_progress] M4 前端 GREEN/REGRESSION、提交、合并与合并后验证

## Expected Verification

- `node tests/e2e/dcc-controlled-file-upload-current-version-static.spec.js`
- `node tests/e2e/dcc-controlled-file-upload-distribution-scope-static.spec.js`
- `pnpm ts:check`
- 合并后真实 Playwright E2E 走本机前端页面路径，测试租户写入、admin 只读复验。

## Current Status

in_progress

## Previous Task Check

- 当前前端 worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\upload_dcc_20260715\yudao-ui-admin-vue3`
- 当前分支：`codex/upload_dcc_20260715`
- 启动时前端工作区无未提交差异。

## 经验门禁

- PowerShell：中文读写和命令输出显式 UTF-8；不使用 Bash heredoc；不使用 `&&`。
- worktree：实现必须在 `upload_dcc_20260715` worktree 内完成；合并后验证通过才删除 worktree。
- 登录/E2E：真实 E2E 前先跑官方登录前置；写入只用测试租户，admin 只做最终只读验证。
- 前端样式：遵循 IntPP operations-console 风格，表单和状态展示保持紧凑、可扫描、明确错误反馈；禁止空 catch 或静默失败。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，通过正式 API 类型、页面状态和静态/E2E 测试覆盖。
- 是否存在临时补丁或绕过：否。

## Runtime Ownership Plan

- Worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\upload_dcc_20260715`
- 前端端口：优先使用非主工作区端口，E2E 前记录实际 URL。
- 后端端口：使用本任务后端 runtime，E2E 前记录健康检查和首个业务请求目标。

## Progress Log

- 2026-07-16：完成上传页现行版本面板、变更方式联动、提交 payload、文控第 4 节点部门树多选和 `selectedDistributionDepartmentIds` payload。
- 2026-07-16：`pnpm ts:check`、现行版本静态契约、文控部门下发静态契约均通过。
- 2026-07-16：worktree 真实 E2E 已通过：`CODEX-DCC-DEPT-20260716010719` 第 4 节点审批请求包含 `stampedPdfUploadTicket`、`sessionId` 和部门下发范围；随后现行版本真实 E2E 验证同编号 ACTIVE 版本自动展示并切换为升版。
- 2026-07-16：当前前端分支已满足提交前验证，待提交并合并到 `int_main` 后复验。
