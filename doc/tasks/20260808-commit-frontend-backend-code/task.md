# 20260808 提交前后端代码

## Task Goal

按用户要求提交当前 `E:\IntRuoyi` 工作区内前端与后端代码改动；只提交确认范围内的文件，不回滚、不清理、不混入无法归属的运行态或临时产物。

## Milestones

- [x] M1: 读取提交、编码、worktree 与 Git 前置规则，确认仓库、分支、remote 与脏改动范围。
- [x] M2: 盘点前后端代码、测试、SQL 与相关任务文档，排除运行态、临时审查输出和明显临时产物。
- [x] M3: 执行提交前检查，按显式路径暂存并创建提交。
- [x] M4: 复扫提交后状态，记录 commit hash、文件清单和剩余未提交项。

## Expected Verification

- `git status --short --branch`
- `git diff --cached --name-status`
- `git diff --cached --check`
- `git diff --name-status -- IntRuoyiBackend IntRuoyiFronted`
- `git show --name-status --oneline -1`

## Current Status

completed

已按显式路径提交已验证通过的 `IntRuoyiBackend/` 与 `IntRuoyiFronted/` 下源码、测试与 SQL 变更。最终提交为 `1410ee239 feat: add active order release workflow` 和 `c645aad69 fix: align batch record test tabs`。提交后 `git diff --name-status -- IntRuoyiBackend IntRuoyiFronted` 与 `git diff --cached --name-status` 均无输出；`git status --short --branch -- IntRuoyiBackend IntRuoyiFronted` 仅剩未提交的 `target-pqc-route-snapshot*` 临时验证产物。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务仅执行 Git 提交，并保留通过验证的正式前后端实现与测试。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/powershell-memory.md#Git 提交与推送门禁`：提交前确认分支、remote、脏状态和 staged 清单；禁止混入秘密文件、冲突或不可归属变更。
- `docs/powershell-memory.md#批量暂存脚本被拦截时的显式路径门禁`：不用宽泛 `git add -A`；按明确目录/文件暂存，复核 staged 清单与 `git diff --cached --check`。
- `docs/powershell-memory.md#提交后残余改动复扫门禁`：每次提交后立即运行 `git status --short --branch` 与 `git diff --name-status`，确认是否还有需提交或需保留的残余。
- `docs/powershell-memory.md#共享分支并发基线提交门禁`：共享 `int_main` 上如最近提交或工作区含多任务文件，必须记录边界，禁止把并行任务伪装成本任务提交。
