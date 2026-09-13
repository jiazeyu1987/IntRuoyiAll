# Execution Log

## BDD / TDD

- BDD: 正式订单快照冻结输出物料 -> Given 路线发布快照中同一工序冻结输出物料 A、B 且目标数量为 100 When 生产组长将正式工单加入活跃订单 Then 工序生产配置快照必须包含全体 `outputMaterialIds`，供后续进度算法按 A、B 累计后的最小完成口径计算。
- BDD: 拆分提交不虚增进度 -> Given 同工序输出物料 A、B 各累计 50 When A 与 B 分开提交并形成各自生产事实 Then 列表、工序完成量和完工门禁不得把两种物料相加成 100；只有 A、B 均达到目标时才视为完成。

## Evidence

- Bug: EDHR-STATIC-013 重新打开项指出正式活跃订单工序快照缺少 `outputMaterialIds`，多输出物料拆分提交时进度判断可能回到旧 allocation 累加口径。
- Expected: 正式活跃订单加入链路必须从发布路线快照冻结全体 `outputMaterialIds`，列表进度、工序完成量和完工门禁必须按各输出物料累计后的最小完成口径计算。
- Reproduction: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs`。
- PRECHECK: 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/backend-development.md`、`docs/powershell-encoding.md`、`docs/request-command-log.md`、`docs/worktree-restrictions.md`。
- PRECHECK: 已读取 bug-regression-fix-loop 技能及 `references/bug-contract.md`。
- PRECHECK: 当前 worktree 缺少指定缺陷文档与独立复核报告；已只读从 `E:\IntRuoyi\docs\bugs\20260912-edhr-90-step-static-audit.md` 和 `E:\IntRuoyi\doc\tasks\20260913-edhr-fix-independent-audit\verification-report.md` 获取 EDHR-STATIC-013 / R013 证据。
- RED: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` -> FAIL，expected reason: 缺少 `MesOutputMaterialProgressCalculator.java`，且当前正式快照生产方/消费方尚未统一锁定 `outputMaterialIds`。
- Root Cause: 正式活跃订单工序配置快照未从发布路线快照冻结 `outputMaterialIds`，而列表进度、完工门禁和工序完成记录仍可按旧 allocation 累加口径判断进度，导致多输出物料分次提交时真实最小完成口径不可达。
- Regression test added: `IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` 锁定生产快照写入、路线发布校验、共享输出物料进度计算器、列表进度、完工门禁和工序完成记录合同。
- Implementation: `MesTeamLeaderActiveOrderServiceImpl` 生成生产配置快照时写入全体 `outputMaterialIds`，并在读取正式路线生产配置时 fail fast 校验字段存在；`MesProRouteCandidateConfigServiceImpl` 发布前要求非空、正数、去重输出物料集合。
- Implementation: 新增 `MesOutputMaterialProgressCalculator`，统一按活跃订单快照冻结的 `outputMaterialIds`、当前 allocation event IDs 和生产事件 `materialDetails` 聚合每个输出物料数量，并取最小值作为保守工序进度；缺少来源、事件、物料或数量时 fail fast，不回退到 allocation 累加。
- Implementation: 列表剩余量、完工门禁和工序完成记录均改用同一个保守输出物料进度；`MesProProcessPoolEventMapper` 新增正式生产提交事件批量读取，避免列表进度只看分配数量。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` -> PASS，输出 `PASS: EDHR-STATIC-013 output-material snapshot and progress contract`。
- GREEN: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile` -> PASS，Reactor 25/25 SUCCESS，`yudao-module-mes` 编译通过。
- Verification: `git diff --check` -> PASS，仅报告 Windows CRLF 提示，无 whitespace error。
- Verification: `rg -n "MesOutputMaterialProgressCalculator|resolveConservativeProcessProgressQuantities|requireOutputMaterialIds|selectProductionSubmitsByWorkOrderIdsAndRouteIds|validateOutputMaterialIds" ...` -> PASS，命中范围限定为本任务快照生产方、保守进度消费方、事件 Mapper、路线校验和定向静态合同。
- Verification: `python -X utf8 C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc\tasks\20260913-edhr-static-013-reopened-output-material-snapshot\execution-log.md` -> PASS。
- Experience: 已按 `project-experience-consolidation` 合并到已有 `docs\backend-development.md` 和 `docs\experience-index.md`，记录多输出物料活跃订单快照与保守进度门禁。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-013-reopened-output-material-snapshot --mode preview` -> BLOCKED，keep 仅包含 `task.md`、`execution-log.md`、`verification-report.md`，delete 为 none；blocked 原因是当前 linked worktree 无法解析分支 `current_branch=None`。
- MERGE BLOCKED: 用户要求融合进 `int_main` 后，复核 `E:\IntRuoyi` 主工作区发现当前 `int_main` 已处于脏状态且包含未解决冲突：`UU IntRuoyiBackend/yudao-module-dcc/src/main/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceImpl.java`、`UU IntRuoyiBackend/yudao-module-dcc/src/test/java/cn/iocoder/yudao/module/dcc/service/file/DccTrainingTaskServiceTest.java`；同时存在多项非本任务 DCC/前端/resource 改动。为避免混入或覆盖并行任务，未执行 merge、checkout、commit、push 或冲突处理。
- SUBMIT BLOCKED: 用户要求“先提交,然后融合int_main”后，已创建 `codex/edhr-static-013-output-material-snapshot` 并暂存本任务代码、静态合同、任务证据和经验文档；提交前 `scripts\preflight\branch-runtime-port-guard.ps1` 返回失败：当前 workspace profile 为 `int_main`，但当前分支为 `codex/edhr-static-013-output-material-snapshot`，脚本要求切换到 `int_main`。
- MERGE BLOCKED: 同轮复核 `E:\IntRuoyi` 的 `int_main` 已 ahead 1 且存在未解决 MES 冲突：`UU IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/dal/mysql/pro/processpool/team/MesProcessPoolReportAllocationMapper.java`、`UU IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/enums/ErrorCodeConstants.java`、`UU IntRuoyiBackend/yudao-module-mes/src/main/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandService.java`、`UU IntRuoyiBackend/yudao-module-mes/src/test/java/cn/iocoder/yudao/module/mes/service/pro/processpool/team/MesReportAllocationCommandServiceTest.java`；未处理并行冲突，未执行 commit、merge、push。
- RECOVERY: 用户确认 4 个冲突已解决后，复核 `E:\IntRuoyi` 的 `int_main` 无 `UU` 冲突，状态为 ahead 4；随后按用户要求执行 `git push origin int_main` -> PASS，将既有主干提交推送到 `origin/int_main`。
- FUSION: 在 `E:\IntRuoyi` 的 `int_main` 上应用本任务补丁；`MesTeamLeaderActiveOrderCompletionProgressPortImpl.java` 与 `MesTeamLeaderActiveOrderServiceImpl.java` 出现融合冲突，已保留主干已有 `inputMaterialIds` 快照逻辑，并采用本任务共享 `MesOutputMaterialProgressCalculator` 的 fail-fast 保守进度口径。
- RESOLUTION: 本轮用户确认两个 `UU` 冲突文件已修复；`rg -n "<<<<<<<|=======|>>>>>>>" ...` 无命中，移除 `MesTeamLeaderActiveOrderServiceImpl.java` 首行 UTF-8 BOM 后 `git add` 两个文件。
- GREEN: `git ls-files -u` -> PASS，索引无未解决冲突。
- GREEN: `scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`int_main/int_main` 使用 frontend `8081`、backend `48081`。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` 于 `E:\IntRuoyi` `int_main` -> PASS。
- GREEN: `git diff --check --cached` -> PASS。
- GREEN: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile` 于 `E:\IntRuoyi` `int_main` -> PASS，Reactor 25/25 SUCCESS，`BUILD SUCCESS`。
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-013-reopened-output-material-snapshot --mode preview` -> PASS，keep 为 `task.md`、`execution-log.md`、`verification-report.md`；delete/blocked/warnings 均为 `<none>`。
- CLOSEOUT: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260913-edhr-static-013-reopened-output-material-snapshot --mode apply` -> PASS，deleted_paths 为 `<none>`。
- SUBMIT: 本地实现提交 `2618a4a3f` 已落在 `E:\IntRuoyi` 的 `int_main`；随后本地文档修正提交 `16997a9c7`。
- FUSION: `git merge origin/int_main` -> PASS，merge commit `362929947`，本地 `int_main` 已包含最新 `origin/int_main` 与 EDHR-STATIC-013 实现。
- GREEN: 融合后复核 `scripts\preflight\branch-runtime-port-guard.ps1`、`node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs`、`git diff --check`、`mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -am -DskipTests compile`、bug evidence validator、cleanup preview/apply 均 PASS。
- Blockers: 当前无代码或融合冲突阻塞；本轮仍未执行 E2E、未启动服务、未写数据库。新本地提交尚未获明确 Git push 授权，因此保持 `ready_for_closeout`，不标记 `completed`。

## Current Notes

- 用户要求只做静态代码逻辑检查；本任务不做 E2E、不使用 Playwright、不启动服务、不写数据库。本轮已授权本地提交和融合 `int_main`；本任务新提交的远端推送需单独明确授权。
