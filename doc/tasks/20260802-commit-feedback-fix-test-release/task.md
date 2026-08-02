# 提交第三方报工修复并重新发布测试服

## Task Goal

按用户要求先提交 `E:\IntRuoyi` 当前前后端代码，确保第三方报工修复进入下一次 clean release worktree 的发布源，然后构建新的 releaseTag 并仅发布到测试服务器 `172.30.30.58`，最后用真实运行态和 `李萍.xlsx` 导入路径确认测试服已加载修复。

## Milestones

1. 建立任务记录并读取提交、worktree、发布、服务器、后端、前端、E2E、PowerShell 门禁。`completed`
2. 提交当前脏工作区基线，记录 commit hash、文件清单、敏感信息/大文件扫描和 push 结果。`completed`
3. 创建下一次发布专用 clean worktree，确认发布源包含 `DirectWorkstationResolution` / `resolveDirectFeedbackWorkstation` 修复。`completed`
4. 在 clean worktree 中执行必要构建/回归，构建新的测试发布包并验证 manifest / release-info / sourceRepos。`in_progress`
5. 仅发布测试服务器，禁止正式服、备份服、mark-tested、promote-prod、promote-backup。`pending`
6. 发布后验证测试服真实运行态和第三方报工真实导入路径。`pending`
7. 记录问题、沉淀经验、清理本任务 worktree，最终标记 completed。`pending`

## Expected Verification

- 当前脏工作区形成独立可追溯提交，并推送到 `origin/int_main`；不得丢弃、回滚或静默遗漏既有改动。
- 新发布 worktree 的 `HEAD` 包含第三方报工修复符号和对应提交历史，且 `git status --short` clean。
- 发布包 manifest / release-info 的 sourceRepos commit 指向包含修复的提交，`dirty=false`。
- 测试服 `.env IMAGE_TAG`、backend/frontend 镜像 tag、容器状态、backend health、frontend HTTP 200、release-info、release lock、migration 状态均匹配新 releaseTag。
- 真实前端路径导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后，正式报工列表和排产工单进度在测试服更新。

## Current Status

in_progress

## Applicable Experience Gates

- 脏工作区基线门禁：当前 `E:\IntRuoyi` 有大量 tracked/untracked 改动；提交前必须记录 dirty 范围、扫描大文件和敏感信息、确认当前任务文件不混入基线提交。
- 第三方报工直报正式链路门禁：导入成功必须创建/提交正式报工并参与排产进度汇总，禁止用导入记录直接进度、前端假新增、默认成功或空列表刷新替代。
- 发布源一致性门禁：本机源码通过不等于测试服通过；必须验证 clean release worktree、manifest sourceRepos、远端 release-info 和实际镜像 tag 都包含修复提交。
- 测试服发布门禁：只允许测试服发布；缺少 operation SUCCESS、manifest、远端 `.env`、实际镜像、health、HTTP、release-info、release lock 或 migration 任一项不得判定完成。
- PowerShell / SSH / UTF-8 门禁：中文、Excel 路径、SSH/MySQL stdin、release-info 和任务文档均使用显式 UTF-8；不记录凭据。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是让修复进入正式发布源并由测试服真实运行态验证。
- `是否存在临时补丁或绕过`：否。
