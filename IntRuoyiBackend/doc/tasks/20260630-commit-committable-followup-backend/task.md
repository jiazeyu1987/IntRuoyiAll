# 任务：提交当前可闭环的后端代码补充批次

- Task ID: `20260630-commit-committable-followup-backend`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在后端仓库中仅提交当前仍留在工作区、但已经具备 completed 状态与验证证据的代码，当前目标只包含：

- `20260629-srm-nas-locator-production-share-scope`
- `20260629-scheduler-workbench-full-config-package`

其余 `in_progress` / `blocked` / 同文件混入未完成 hunk 的改动继续保留在工作区。

## Previous Task Check

- 上一个后端提交任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-commit-backend-code\task.md`
- 状态：`completed`
- 处理说明：上一批后端提交已完成；本次为新的补充提交批次，不复用旧提交结论。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md` 与 `docs\worktree-memory.md`；提交边界以后端 Git 仓库为准，同文件混入未完成 hunk 不得整文件强提。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文任务文档与执行日志显式 UTF-8；PowerShell 不可用时改用等价安全执行通道，但不改变提交边界规则。
- `D:\ProjectPackage\Int\IntRuoyi\docs\worktree-memory.md`
  - 收口前必须核对 `git diff --stat` 与 staged 文件列表，避免把进行中任务一并带入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；只提交已闭环、已验证且边界清晰的正式代码。
- `是否存在临时补丁或绕过`：否；不回滚用户改动，不用破坏性 Git 命令，不把 blocked 任务伪装为可提交。

## BDD 场景

- `BDD: 已完成后端任务可独立提交 -> Given 后端工作区存在多个主题改动 / When 本次补充提交收口 / Then 只提交具备 completed 状态与 GREEN 证据的后端文件组。`
- `BDD: 未完成任务混入共享文件时不得强提 -> Given 某些 showroom/mes/dcc 共享文件同时混有 blocked 或 in_progress hunk / When 评估提交范围 / Then 这些文件整体留在工作区，不为了提交而一并带入。`

## Milestones

1. M1：建立本轮后端补充提交任务并锁定候选任务。`completed`
2. M2：补跑候选任务定向验证。`completed`
3. M3：按任务边界提交后端代码。`completed`
4. M4：记录剩余未提交范围并完成收尾预览。`completed`

## Expected Verification

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am -Dtest=SrmNasLocatorServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.testIncludes=**/MesProSchedulerWorkbenchFullConfigPackageServiceTest.java,**/MesProSchedulerWorkbenchControllerPermissionContractTest.java -Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --name-only`
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro diff --cached --check`

## Final Verification Result

- `cmd /c mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-srm -am -Dtest=SrmNasLocatorServiceTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `cmd /c mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes -Dmaven.compiler.testIncludes=**/MesProSchedulerWorkbenchFullConfigPackageServiceTest.java,**/MesProSchedulerWorkbenchControllerPermissionContractTest.java -Dtest=MesProSchedulerWorkbenchFullConfigPackageServiceTest,MesProSchedulerWorkbenchControllerPermissionContractTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS
- `git -C D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro commit -m "任务: 提交全量数据包与NAS共享范围补充批次"` -> PASS，创建 commit `2b04736a28`

## Current Blockers

- 无新的提交阻塞；剩余改动属于进行中/阻塞任务或边界不清文件，继续保留在工作区。
