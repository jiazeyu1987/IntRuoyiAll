# 20260727-commit-frontend-backend-code

## Task Goal

提交当前 `E:\IntRuoyi` 根仓库中前端、后端及关联任务文档的所有待提交代码，并按项目规则完成提交、推送和证据记录。

## Milestones

- [x] 创建本次提交任务文档。
- [x] 完成提交前 Git、分支、remote、运行端口规则和敏感/大文件预检。
- [x] 将既有脏工作区改动保存为独立基线提交。
- [x] 完成本次提交任务记录和收尾提交。
- [ ] 推送当前分支到 `origin` 并确认不再领先远端。

## Expected Verification

- `git status --short --branch`
- `git branch --show-current`
- `git remote -v`
- `git diff --check`
- `scripts\preflight\branch-runtime-port-guard.ps1`
- 推送后再次执行 `git status --short --branch`

## 经验门禁

### Git 提交与推送门禁

- Trigger: 提交、推送、处理脏工作区或保存基线提交。
- Preflight check: 执行 `git status --short --branch`、`git branch --show-current`、`git remote -v`、staged 文件清单检查、`git diff --check`。
- Blocker: 缺少 Git 仓库、缺少可用 `origin`、发现无法归属的敏感文件、超大文件、冲突或推送失败。
- Verification: 记录提交 hash、文件清单、推送结果和推送后 `git status --short --branch`。
- Forbidden action: 禁止 force push、历史重写、destructive reset、跳过 push 或把当前任务文档混入既有脏工作区基线。
- Evidence: `docs\powershell-memory.md` 与 `docs\task-closeout-rules.md`。

### Branch Runtime Port Guard

- Trigger: 提交或推送涉及本地运行脚本、端口规则或分支运行态文件。
- Preflight check: 执行 `scripts\preflight\branch-runtime-port-guard.ps1`。
- Blocker: int_main 默认端口偏离 `8081/48081`、端口矩阵冲突或守卫脚本失败。
- Verification: 守卫脚本输出 `Branch runtime port guard passed for int_main/int_main: frontend 8081, backend 48081.`。
- Forbidden action: 禁止随机换端口、跳过守卫或冒充其它 runtime profile。
- Evidence: `docs\branch-runtime-ports.md`。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务只负责提交与留痕，不改变业务实现。
- `是否存在临时补丁或绕过`：否。

## Cleanup Candidates
