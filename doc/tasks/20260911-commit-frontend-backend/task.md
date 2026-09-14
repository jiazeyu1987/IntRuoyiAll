# 提交前后端代码

## Task Goal

核对当前工作区中的前端、后端改动及其既有任务验证证据，只提交用户本轮授权的前后端代码，不夹带无关产物或其他任务文件。

## Milestones

- [x] 核对 Git 分支、工作区改动和文件归属
- [x] 核对相关任务的验证状态与提交门禁
- [x] 运行提交前必需检查并确认 staged 文件清单
- [x] 修复提交前验证阻断点：DCC 回归失败与 SQL release-migration 元数据失败
- [x] 提交并推送前端、后端代码，记录 commit hash 和 push 结果

## Expected Verification

- `git status --short --branch`
- `git diff --check`
- `mvn -pl yudao-module-dcc -am -Dtest=DccControlledFileQueryServiceTest#getControlledFile_requesterOutsideAssignmentHardScopeIsDenied,DccPublicationFollowupQueryServiceTest#getFileFollowup_linkRevisionTimelineUsesAuditLinkedVersionAfterTaskReopened test`
- `python -X utf8 IntRuoyiBackend\script\release\run-release-migration-policy-gate.py --sql-root IntRuoyiBackend\sql\mysql --sql-file ...20260908_gxp_audit_trail_core.sql --output ...`
- `git diff --cached --name-only`
- `scripts/preflight/branch-runtime-port-guard.ps1`
- 复用各代码改动所属任务已记录的定向测试证据；任何未验证改动均阻塞提交

## Current Status

completed

用户已要求“直接修”。当前提交阻断点已修复并通过定向验证；前后端代码已提交并推送到 `origin/int_main`，最终提交为 `860496e330a8b47bcb421b8b65ece03c06df33d8`。远端分歧已通过合并远端文档提交后重写为单个前后端提交解决；`resource/通用检验规程` 下新增 Office 资源文件因不属于本轮“前后端代码”范围且触发 HTTPS 大对象上传超时，已保留在本地磁盘、未纳入远端提交。

## Design Constraints Check

- 本任务不修改产品行为，仅负责核对并提交现有前后端代码。
- 本轮追加允许在提交门禁范围内做最小后端/SQL 修复，但不得引入 fallback、吞异常或默认成功。
- 不提交临时日志、截图、构建产物、凭据或无法确认归属的文件。
- 已收到本轮 Git 提交/推送授权；仍必须先满足验证门禁。
- 不启用子 Agent，不执行 E2E，不操作数据库、服务器或运行中的 `int_main` 服务。
