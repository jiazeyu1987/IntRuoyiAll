# 提交推送前后端代码

## Task Goal
提交并推送当前 int_main 分支上已存在的前后端代码、SQL、测试和相关文档改动到 origin/int_main。

## Milestones
- [x] 读取并核对提交、前端、后端、数据库和收尾规则。
- [x] 冻结当前工作区改动范围并完成提交前验证。
- [x] 按用户授权提交当前脏工作区基线。
- [x] 推送当前分支到 origin 并验证分支不再 ahead。
- [x] 执行收尾清理、记录证据并标记完成。

## Expected Verification
- git status --short --branch
- git diff --check
- git ls-remote --heads origin int_main
- git push origin int_main
- git status --short --branch

## Design Constraints Check
- 禁止 fallback、降级、吞异常、模拟成功。
- 仅执行用户当轮授权的本地 Git 提交与推送；不发布、不重启服务、不写数据库、不操作远程服务器。
- 不回滚或删除未明确归属的既有改动；当前脏工作区按提交规则作为用户授权基线提交。

## Current Status
completed - 前后端当前改动已提交并推送；cleanup preview/apply 通过，收尾记录已完成。

