# 提交并推送前后端代码

## Task Goal

按用户要求提交并推送当前工作区中 IntRuoyiBackend 与 IntRuoyiFronted 下的前后端代码、SQL、脚本和测试改动；不主动提交根目录规则、doc/、docs/ 等非前后端代码改动，仅保留本任务必需的任务记录。

## Milestones

- [x] 读取提交、PowerShell、worktree 与编码规则。
- [x] 创建本次提交任务记录。
- [x] 核对 Git 分支、remote、staged 文件清单和提交范围。
- [x] 暂存仅前后端路径并执行提交前检查。
- [x] 创建前后端代码提交并复扫残余改动。
- [ ] 推送 `int_main` 并确认本地不再领先 `origin/int_main`。
- [ ] 执行任务收尾检查并记录最终结果。

## Expected Verification

- git status --short --branch
- git branch --show-current
- git remote -v
- scripts\\preflight\\branch-runtime-port-guard.ps1
- git diff --cached --check
- git diff --cached --name-status
- git show --name-status --oneline -1
- git push origin int_main
- git status --short --branch（推送后确认 ahead 为 0）

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；本任务只执行用户明确要求的 Git 提交，不修改业务实现。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 任务提交推送前置门禁：提交前必须检查分支、remote、工作区状态和 staged 文件清单；发现不应提交的敏感文件或无法归属冲突时阻塞。
- 显式路径暂存门禁：工作区存在大量并行改动时，使用明确路径暂存前后端范围，禁止 git add -A 混入无关文档或临时产物。
- 提交后残余改动复扫门禁：提交后必须复扫 git status --short --branch 与最近提交文件清单，确认未提交改动是否仍为本次范围外内容。
