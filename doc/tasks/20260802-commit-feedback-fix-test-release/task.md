# 提交第三方报工修复并重新发布测试服

## Task Goal

按用户要求先提交 `E:\IntRuoyi` 当前前后端代码，确保第三方报工修复进入下一次 clean release worktree 的发布源，然后构建新的 releaseTag 并仅发布到测试服务器 `172.30.30.58`，最后用真实运行态和 `李萍.xlsx` 导入路径确认测试服已加载修复。

## Milestones

1. 建立任务记录并读取提交、worktree、发布、服务器、后端、前端、E2E、PowerShell 门禁。`completed`
2. 提交当前脏工作区基线，记录 commit hash、文件清单、敏感信息/大文件扫描和 push 结果。`completed`
3. 创建下一次发布专用 clean worktree，确认发布源包含 `DirectWorkstationResolution` / `resolveDirectFeedbackWorkstation` 修复。`completed`
4. 在 clean worktree 中执行必要构建/回归，构建新的测试发布包并验证 manifest / release-info / sourceRepos。`completed`
5. 仅发布测试服务器，禁止正式服、备份服、mark-tested、promote-prod、promote-backup。`completed`
6. 发布后验证测试服真实运行态、运行控制台版本号和变更说明。`completed`
7. 验证第三方报工真实导入路径并区分发布问题与业务数据前置问题。`blocked_by_data_precondition`
8. 记录问题、沉淀经验、清理本任务 worktree，最终标记 completed。`completed`

## Expected Verification

- 当前脏工作区形成独立可追溯提交，并推送到 `origin/int_main`；不得丢弃、回滚或静默遗漏既有改动。
- 新发布 worktree 的 `HEAD` 包含第三方报工修复符号和对应提交历史，且 `git status --short` clean。
- 发布包 manifest / release-info 的 sourceRepos commit 指向包含修复的提交，`dirty=false`。
- 测试服 `.env IMAGE_TAG`、backend/frontend 镜像 tag、容器状态、backend health、frontend HTTP 200、release-info、release lock、migration 状态均匹配新 releaseTag。
- 真实前端路径导入 `C:\Users\BJB110\Desktop\文档\李萍.xlsx` 后，如目标租户缺少工单、活动任务、工序、工位或报工用户等业务前置数据，必须记录为业务数据阻塞，不得误判为发布失败或旧代码未加载。

## Current Status

completed

## Release Result

- 发布范围：仅测试服务器 `172.30.30.58`；未执行 `mark-tested`、`promote-prod`、`promote-backup`、正式服或备份服发布。
- 目标提交：`f0c34dfed910f52f9c03b401e976cbd2d0424e00`，已位于 `origin/int_main`。
- 构建 worktree：`D:\ProjectPackage\Int\IntRuoyiWorktrees\r260802-feedback-fix-test-v5\app`，detached HEAD 为目标提交，`git status --short --branch --untracked-files=no` clean。
- 发布包：`E:\Int\CacheData\IntRuoyi\publish-int-ruoyi\release-20260802-feedback-fix-test-r260802h-r1`。
- releaseTag：`release-20260802-feedback-fix-test-r260802h-r1`。
- 测试服真实运行态：`.env IMAGE_TAG`、backend/frontend 实际镜像、容器 running、backend health `UP`、frontend HTTP `200`、远端 `/release-info.json`、release lock `APPLIED`、migration `FAILED/RUNNING=0` 均匹配该 releaseTag。
- 运行控制台版本说明：真实浏览器验证 PASS，页面显示 `release-20260802-feedback-fix-test-r260802h-r1` 和变更说明，console errors 为 0。
- 业务导入验证：测试服已加载新发布包；`李萍.xlsx` 在测试服导入仍受业务数据前置条件阻塞，测试租户缺少 Excel 工单，芋道源码租户还缺活动任务、工序、工位或报工用户等数据，不作为本次发布失败。
- 收尾清理：`r260802-feedback-fix-test`、`v2`、`v3`、`v4`、`v5` release worktree Git 注册和物理目录均已删除；临时分支 `codex/release-info-change-notes-20260802` 已确认合入 `int_main` 后删除；无进程引用已删除路径。
- 运行控制台收尾：重启后本机 `127.0.0.1:48181` 未运行，且无任务 release worktree 进程引用；本任务未保留需要恢复的运行控制台实例。

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

## Cleanup Keep

- `doc/tasks/20260802-commit-feedback-fix-test-release/runtime-version-ui-evidence.json`
