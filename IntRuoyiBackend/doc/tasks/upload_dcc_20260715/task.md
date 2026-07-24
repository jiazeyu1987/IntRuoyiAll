# upload_dcc_20260715 后端开发任务

## Task Goal

在 `upload_dcc_20260715` 后端 worktree 中完成电子文控系统受控文件上传流程后端开发与验证，最终随前端合并进 `int_main`，并支持合并后的真实数据 E2E 验证。

## Milestones

- [completed] M1 任务记录、经验门禁和 RED 测试
- [completed] M2 文件编号现行版本查询与变更方式提交校验
- [completed] M3 文控最终部门下发范围确认与培训/归档联动
- [in_progress] M4 后端 GREEN/REGRESSION、提交、合并与合并后验证

## Expected Verification

- `mvn.cmd -pl yudao-module-dcc -am "-Dtest=DccControlledFileCurrentVersionLookupServiceTest,DccControlledFileChangeTypeSubmissionServiceTest,DccControlledFileDistributionScopeServiceTest" test`
- 受影响 DCC workflow/finalization 回归测试。
- 合并后真实前端 E2E 只通过页面路径创建或处理测试租户数据，API/DB 仅用于最终只读核验。

## Current Status

in_progress

## Previous Task Check

- 当前后端 worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\upload_dcc_20260715\ruoyi-vue-pro`
- 当前分支：`codex/upload_dcc_20260715`
- 启动时仅发现本任务未跟踪文件 `DccControlledFileChangeTypeEnum.java`，未发现已提交实现。

## 经验门禁

- PowerShell：所有中文读写和命令输出显式 UTF-8；不使用 Bash heredoc；PowerShell 5.1 不使用 `&&`。
- worktree：实现必须在 `upload_dcc_20260715` 前后端 worktree 内完成；合并前提交 scoped 变更；合并后在 `int_main` 上重新验证；成功后才删除 worktree。
- 登录/E2E：真实 E2E 前必须先用官方 `login-preflight.mjs` 跑通本机登录；写入型 E2E 只用测试租户 `测试租户/aoteman`；`芋道源码/admin` 仅做最终只读验证，除非用户明确覆盖。
- 前端/后端联动：新增 SQL、菜单、脚本或发布契约时必须补契约验证；本轮如修改 schema，必须同步 SQL/DO/测试。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，后端统一负责版本链、变更方式和下发部门最终校验。
- 是否存在临时补丁或绕过：否。

## Runtime Ownership Plan

- Worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\upload_dcc_20260715`
- 后端端口：优先使用非主工作区端口，启动前记录实际端口、数据库、Redis、文件服务。
- 前端端口：优先使用非主工作区端口，真实 E2E 前记录实际 URL 和首个业务请求目标。

## Progress Log

- 2026-07-16：完成后端实现与回归：DCC workflow 75 tests、外来文件评审 6 tests、Mapper XML 1 test、MES 懒加载依赖契约 2 tests、SQL 幂等契约均通过。
- 2026-07-16：worktree 真实 E2E 已通过：`测试租户/aoteman` 在 `http://127.0.0.1:8086` 提交 `CODEX-DCC-DEPT-20260716010719`，第 4 节点携带 `stampedPdfUploadTicket`、`sessionId` 和部门下发范围，最终文件状态 `ACTIVE`，生成部门下发记录。
- 2026-07-16：当前后端分支已满足提交前验证，待提交并合并到 `int_main` 后复验。
