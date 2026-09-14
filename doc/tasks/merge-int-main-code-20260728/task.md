# 融合 int_main 代码

## Task Goal

将当前工作区与 `int_main` 代码状态对齐，明确是否需要从 `origin/int_main` 融合代码，并完成必要验证与任务记录。

## Milestones

- [x] 建立任务记录并读取适用经验门禁
- [x] 检查当前 Git 分支、remote、工作区状态和 `int_main` 差异
- [x] 执行 `int_main` 代码融合或确认无需融合
- [x] 运行必要验证并记录证据
- [x] 完成收尾记录、清理检查、提交并推送

## Expected Verification

- `git status --short --branch`
- `git fetch origin int_main`
- 按实际差异执行 `git merge` / `git pull --ff-only` / 确认 no-op
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 如融合产生代码变更，按影响范围补充后端、前端或 E2E 验证

## Current Status

completed

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是；本任务以真实 Git 分支状态和远端状态为准，不用临时替代路径。
- `是否存在临时补丁或绕过`：否

## Experience Gate Summary

## 经验门禁

### Git / Push / PowerShell 门禁

- Trigger: 当前任务涉及 `git fetch`、融合 `int_main`、提交、推送和 PowerShell 命令编排。
- Preflight check: 执行 `git status --short --branch`、`git branch --show-current`、`git remote -v`；推送前运行分支端口 guard，并扫描待推送历史中是否存在超过 100 MB 的 blob。
- Blocker: 当前目录不是 Git 仓库、当前分支或 remote 不符合预期、存在无法解释的脏改动或冲突、发现超 100 MB blob、推送被拒、缺少凭据或网络。
- Verification: 记录融合结果、guard 输出、大文件扫描结果、`git push origin <branch>` 退出码和推送后的 `git status --short --branch`。
- Forbidden action: 不使用 force push、历史重写、destructive reset、静默跳过 push、默认编码写中文、`&&` 串联命令或把失败验证当成功。
- Evidence: `docs\powershell-memory.md`、`docs\task-closeout-rules.md`、`docs\branch-runtime-ports.md`、`D:\ProjectPackage\Int\IntRuoyiMaintance\docs\release-build-preflight-lessons.md#2026-07-24-github-推送前历史大文件门禁`。
