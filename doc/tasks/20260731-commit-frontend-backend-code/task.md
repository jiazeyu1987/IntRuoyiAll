# 提交前后端代码

## Task Goal

- 按项目规则提交当前 `int_main` 工作区中的前后端相关代码与既有任务证据改动，并推送到 `origin/int_main`。
- 先保存开始任务前已有脏工作区为独立基线提交，再单独提交本任务收尾记录。

## Milestones

1. `completed` - 完成提交/推送前置规则读取、Git 状态识别和任务文档初始化。
2. `completed` - 保存开始任务前已有脏改动基线提交，并记录 commit hash 与文件清单。
3. `in_progress` - 执行提交前门禁检查、推送当前分支到 `origin`。
4. `completed` - 完成收尾记录、清理预检和最终状态更新。

## Expected Verification

- `git status --short --branch`
- `git branch --show-current`
- `git remote -v`
- `git diff --cached --name-status`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- `git push origin int_main`
- 推送后 `git status --short --branch` 不再显示 ahead。

## Applicable Experience Gates

- `docs/task-closeout-rules.md`：提交前必须检查状态、staged 文件清单，脏工作区先做独立基线提交，任务收尾记录单独提交。
- `docs/powershell-memory.md`：PowerShell Git 编排不得使用 `&&`，提交后必须复扫状态，推送后确认不再 ahead。
- `docs/experience-index.md`：命中 Git 提交推送、脏工作区基线、提交后残余改动复扫、GitHub 推送大文件门禁。
- `docs/powershell-encoding.md`：中文任务文档使用 UTF-8 写入和读取验证。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按项目既有提交/推送门禁保存基线并记录证据。
- `是否存在临时补丁或绕过`：否。
