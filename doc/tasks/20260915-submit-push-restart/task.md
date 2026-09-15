# Task: 提交推送并重启前后端

## Task Goal
提交当前工作区前后端代码，推送当前分支到 origin，并按运行端口矩阵重启前后端。

## Milestones
1. 记录并核对工作区、分支、远端与运行态。
2. 执行必要验证并提交代码。
3. 推送当前分支。
4. 重启前后端并验证端口、健康检查与前端入口。
5. 收尾记录与清理。

## Expected Verification
- git status 与 staged 文件核对
- 前后端构建/测试可执行性检查
- git push origin 当前分支成功且不再 ahead
- 前端 8081、后端 48081 运行并通过检查

## Current Status
in_progress

## 设计约束检查
- 遵守 int_main 端口矩阵，禁止静默换端口。
- 仅处理当前工作区已有改动；不回滚无关改动。
- 用户已明确授权提交、推送及前后端重启。
