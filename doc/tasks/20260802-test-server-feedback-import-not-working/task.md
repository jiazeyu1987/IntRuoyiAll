# 测试服第三方报工导入不生效原因排查

## Task Goal

分析用户反馈的“本机第三方报工导入已通过，但发布到测试服务器后仍报不上”的原因，区分发布包未包含修复、测试服运行态未切换到目标 releaseTag、以及测试服数据条件导致导入跳过。

## Milestones

1. 核对本机 Git/修复代码/发布输入状态。`completed`
2. 核对测试服发布包与运行态版本。`completed`
3. 核对测试服导入结果、跳过原因或正式报工落库情况。`not_required`
4. 输出根因、影响和下一步修复路径。`completed`

## Expected Verification

- 本地修复代码是否已进入可发布提交。
- 测试服当前 releaseTag / manifest sourceRepos 是否包含该提交。
- 测试服 backend/frontend 实际镜像 tag、health、release-info 是否一致。
- 若运行态包含修复，再核对导入记录 skip/success、正式报工反馈记录、排产工单进度。

## Applicable Experience Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`：导入成功必须落到正式报工，不得用导入记录直接进度或前端假新增替代。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\test-release-preflight.md`：测试服发布只认 clean release worktree、manifest sourceRepos dirty=false、远端实际镜像和 releaseTag 一致。
- `D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md`：本机源码通过不等于发布通过，必须验证发布产物和目标环境真实运行态。

## Current Status

ready_for_closeout

根因已定位：测试服当前 releaseTag 使用的 app commit 为 `b99246f58`，该提交不包含第三方报工直报工作站解析修复；修复提交 `b8533d59a` 只在 `codex/third-party-feedback-import-20260802` 分支，尚未进入 `int_main` / `origin/int_main` / 测试服发布包。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，当前根因归属为发布输入未包含修复提交；下一步需要把修复正式合入发布源并重新构建发布。
- `是否存在临时补丁或绕过`：否。
