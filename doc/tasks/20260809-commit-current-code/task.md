# 20260809 提交当前代码

## Task Goal

按用户要求提交当前 `E:\IntRuoyi` 工作区内尚未提交的前端、后端源码与正式测试代码；不回滚现有改动，不提交运行态、编译产物、审查过程输出或其它明显临时文件。

## Milestones

- [x] M1: 读取 Git、任务收尾、编码、worktree 与经验门禁，确认仓库和当前分支。
- [x] M2: 盘点当前代码改动并执行相关定向验证；后端定向测试失败，形成提交阻塞。
- [x] M3: 修复 PQC 提交实现与旧回归契约不一致问题，并完成 GREEN/REGRESSION。
- [x] M4: 运行分支运行端口守卫，按显式路径暂存并检查提交内容。
- [x] M5: 创建 Git 提交并复扫残余代码改动。
- [x] M6: 执行任务清理收尾并记录最终结果。

## Expected Verification

- 当前修改涉及的后端定向 JUnit 测试通过。
- 当前修改涉及的前端静态合同测试通过。
- `scripts\preflight\branch-runtime-port-guard.ps1` 通过。
- `git diff --cached --check` 通过。
- staged 文件不包含 `target*`、`.review-fix-loop`、运行日志、PID、凭据或其它临时产物。
- 提交后 `git diff --name-status -- IntRuoyiBackend IntRuoyiFronted` 无正式代码残余；允许仅保留已识别的临时编译产物。

## Current Status

completed

已创建提交 `199836c5fc105033898c2df59fb8ca22ac005625`（`feat: update frontline PQC workflow`）；提交后即时、5 秒与 12 秒复扫均无前后端正式代码残余；任务清理 preview/apply 通过并仅删除本任务临时 bug evidence，任务完成。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；本任务只执行显式授权的 Git 提交，提交前验证并排除临时产物。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `docs/experience-index.md` 存在并已读取。
- 适用 `docs/powershell-memory.md#Git 提交与推送门禁`：提交前确认分支、remote、脏状态和 staged 清单。
- 适用 `docs/powershell-memory.md#批量暂存脚本被拦截时的显式路径门禁`：按明确路径暂存，禁止宽泛 `git add -A`。
- 适用 `docs/powershell-memory.md#提交后残余改动复扫门禁`：提交后复扫延迟或并行写入的代码改动。
- 适用 `docs/powershell-memory.md#共享分支并发基线提交门禁`：保留并发任务边界，不改写既有历史。

## Cleanup Candidates

无。本任务不拥有当前工作区内既有临时产物，不执行删除。
