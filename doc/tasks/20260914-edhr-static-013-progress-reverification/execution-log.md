# Execution Log

## BDD / TDD

- BDD: 部分分配限制进度 -> Given 一线正式报工同工序输出物料 A 生产 100 件且只被生产组长正式分配 40 件 When 活跃订单列表、工序完成记录或完成门禁计算生产进度 Then 该订单该工序最多只能纳入 40 件，不得把未分配的 60 件计入进度。
- BDD: 多输出物料逐物料受分配约束 -> Given 同工序冻结输出物料 A、B 且目标 100，A 正式分配 80、B 正式分配 40 When 计算订单工序进度 Then 应按每个输出物料已正式分配的生产事实累计，并以全体输出物料的最小完成口径 40 判断进度。
- BDD: 拆分提交与合并提交一致 -> Given 同工序输出物料 A、B 最终各被正式分配 50 When A、B 在一次合并提交或两次拆分提交中形成正式生产事实 Then 活跃订单列表、工序完成记录和完成门禁都得到同一保守进度 50，不得因拆分或合并方式虚增。

## Precheck

- PRECHECK: 已读取 `AGENTS.md`、`docs/task-closeout-rules.md`、`docs/test-release-preflight.md`、`docs/powershell-encoding.md`。
- PRECHECK: 已读取 eDHR/活跃订单/报工/正式分配/输出物料快照/工序进度相关文档：`docs/bugs/20260912-edhr-90-step-static-audit.md` 的 EDHR-STATIC-013 节、`docs/backend-development.md` 相关门禁、`docs/product/frontline-process-material-batch-record-mvp-prd.md`、`docs/product/production-team-leader-daily-operations.md`、`docs/changes/20260831-frontline-material-progress-erp-sync-source.md`、`docs/changes/20260819-production-report-overage-conflict.md`。
- PRECHECK: 已读取 `bug-regression-fix-loop`、`behavior-driven-development`、`independent-verification-gate`、`task-closeout-cleanup` 和 `project-experience-consolidation` 技能要求；本轮用户禁止 Git commit/push，最终状态需按项目规则记录 blocker。
- PRECHECK: `git status --short --branch` 显示工作区已有大量非本任务脏改；本轮只修改 `doc/tasks/20260914-edhr-static-013-progress-reverification/` 及必要 013 范围文件，不回滚、不提交、不处理无关改动。

## Evidence

- Bug: EDHR-STATIC-013 需要重新验证生产进度计算；风险是生产事件数量大于正式分配数量时，未分配产出被纳入活跃订单进度，多输出物料或拆分提交进一步放大进度。
- Expected: 生产进度消费方必须读取正式分配实际数量，并将它与冻结输出物料全集及生产事件 `materialDetails` 对齐后计算；不能只凭 `allocation.eventId` 把整笔生产事件数量全额纳入。
- Reproduction: 先运行消费方定向 JUnit，`MesTeamLeaderActiveOrderCompletionProgressPortImplTest` 与 `MesTeamLeaderOrderProcessCompletionServiceTest` 在旧桩下分别因缺 `outputMaterialIds/eventId`、缺活跃订单工序快照/生产事件来源失败；随后补齐正式来源桩和新增计算器用例转 GREEN。
- Root Cause: EDHR-STATIC-013 生产代码已引入分配数量限制，但实际消费方测试仍停留在旧 allocation 累加/无快照桩，无法证明列表进度、完成门禁和工序完成记录均消费同一正式分配约束；本轮修复的是测试合同缺口，未发现生产代码新增缺口。
- RED: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -Dtest=MesTeamLeaderActiveOrderCompletionProgressPortImplTest test` -> FAIL, expected reason: 实际消费方测试仍使用缺 `outputMaterialIds`、缺 `eventId` 的旧桩，无法证明完成门禁读取正式输出物料快照和正式分配事件。
- RED: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -Dtest=MesTeamLeaderOrderProcessCompletionServiceTest test` -> FAIL, expected reason: 完工记录服务测试未提供活跃订单工序快照和生产事件来源，无法证明工序完成记录按正式分配数量与物料明细计算。
- Static Review: `MesOutputMaterialProgressCalculator` 已按 `outputMaterialIds` 解析冻结输出物料全集，按当前正式分配的 `eventId -> allocatedQuantity` 聚合，并用 `outputQuantity.min(allocatedQuantity)` 限制每个生产事件对物料进度的贡献；缺少快照、事件、物料明细或数量时抛正式业务异常。
- Static Review: `MesTeamLeaderActiveOrderServiceImpl.loadActiveOrderProgress`、`MesTeamLeaderActiveOrderCompletionProgressPortImpl.read`、`MesTeamLeaderOrderProcessCompletionService.reconcileAffectedAllocations` 均调用同一 `MesOutputMaterialProgressCalculator.calculateConservativeProcessProgress`，覆盖活跃订单列表进度、完成门禁和工序完成记录。
- Test Update: `MesOutputMaterialProgressCalculatorTest` 新增部分分配、多输出物料逐事件分配约束、拆分提交与合并提交一致性回归。
- Test Update: `MesTeamLeaderActiveOrderCompletionProgressPortImplTest` 补齐正式输出物料快照、正式分配 eventId 和生产事件 `materialDetails`，使完成门禁测试覆盖真实计算链路。
- Test Update: `MesTeamLeaderOrderProcessCompletionServiceTest` 补齐活跃订单工序快照和生产提交事件桩，保持原完工记录断言，同时证明确认数量来自正式分配约束后的保守进度。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` -> PASS，输出 `PASS: EDHR-STATIC-013 output-material snapshot and progress contract`。
- GREEN: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -Dtest=MesOutputMaterialProgressCalculatorTest test` -> PASS, 3 tests before本轮新增覆盖。
- GREEN: `mvn --% -f IntRuoyiBackend\pom.xml -pl yudao-module-mes -Dtest=MesOutputMaterialProgressCalculatorTest,MesTeamLeaderActiveOrderCompletionProgressPortImplTest,MesTeamLeaderOrderProcessCompletionServiceTest test` -> PASS, 24 tests, 0 failures, 0 errors, 0 skipped。
- Verification: `git diff --check -- IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesOutputMaterialProgressCalculatorTest.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesTeamLeaderActiveOrderCompletionProgressPortImplTest.java IntRuoyiBackend\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\processpool\team\MesTeamLeaderOrderProcessCompletionServiceTest.java` -> PASS；仅有 CRLF 规范化提示，无 whitespace error。
- Verification: `git check-ignore -v doc\tasks\20260914-edhr-static-013-progress-reverification\task.md doc\tasks\20260914-edhr-static-013-progress-reverification\execution-log.md` -> INFO，任务文档被 `E:/IntRuoyi/.git/info/exclude:17:/doc/tasks/*/` 忽略；文件已在工作区存在，但不会出现在普通 `git status` 中。
- CLOSEOUT PREVIEW: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260914-edhr-static-013-progress-reverification --mode preview` -> BLOCKED；keep 为 `task.md`、`execution-log.md`、`verification-report.md`，delete 为 `<none>`，blocked 为 `Current worktree branch could not be resolved.`。
- Blockers: 当前代码和定向验证无剩余 blocker；收尾 blocker 为当前 linked worktree 处于 detached HEAD 且 cleanup 无法解析分支，另有项目规则要求提交推送但用户本轮明确禁止 git commit/push。因此本轮不执行 cleanup apply、不提交、不推送、不标记 completed。

## Current Notes

- 本轮未执行 Playwright/E2E、数据库写入、服务启动/停止/重启、远程服务器操作、Git commit/push。
- 工作区开始时已有大量 DCC/配置/文档等非本任务脏改；本轮只编辑 EDHR-STATIC-013 定向测试和 `doc/tasks/20260914-edhr-static-013-progress-reverification/` 任务证据。

## 2026-09-14 Git Integration Resume

- Authorization: 用户明确要求“提交并融合进int_main”，本轮解除此前 Git commit / merge / push 阻塞。
- PRECHECK: 已复读 `docs/task-closeout-rules.md`、`docs/worktree-restrictions.md`、`docs/branch-runtime-ports.md`、`docs/powershell-encoding.md` 和 `docs/test-release-preflight.md`。
- PRECHECK: `E:\IntRuoyi` 的 `int_main` 在融合前为 clean，且 `git fetch origin int_main` 后本地/远端同步到 `bc640fd96d407d284aab7ab2501215f85ac5bf14`。
- Worktree Migration: 按 `docs/worktree-memory.md` 的 C 盘 detached 临时 worktree 门禁，不在 `C:\Users\BJB110\.codex\worktrees\5e94\IntRuoyi` 直接提交；已创建 `D:\IntRuoyiWorktree\20260914-edhr-static-013-progress-reverification`，分支 `codex/20260914-edhr-static-013-progress-reverification`，并迁移三份 EDHR-STATIC-013 测试和任务证据。
- Worktree Slot: `reserve-worktree-slot.ps1 -Profile int_main -WorktreeRoot D:\IntRuoyiWorktree -AsJson` -> PASS，分配 `slot=9`、前端 `8090`、后端 `48090`。
- Experience Consolidation: 已按 `project-experience-consolidation` 复核长期经验归宿；`docs/worktree-memory.md` 已有 “Codex 临时 Worktree 提交迁移门禁”“Detached HEAD linked worktree 收尾门禁”“流程任务主线程复验与收尾证据门禁”，本轮无新增长期经验条目，避免重复写入。
- GREEN: `pwsh -NoProfile -File scripts\preflight\branch-runtime-port-guard.ps1` -> PASS，`codex/20260914-edhr-static-013-progress-reverification/int_main` 使用前端 `8090`、后端 `48090`。
- GREEN: `git diff --check -- <EDHR-STATIC-013 三份测试与任务记录>` -> PASS；仅有 Java 测试文件 CRLF 规范化提示，无 whitespace error。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-edhr-static-013-output-material-snapshot-static.spec.cjs` -> PASS，输出 `PASS: EDHR-STATIC-013 output-material snapshot and progress contract`。
- GREEN: `mvn.cmd -f IntRuoyiBackend\pom.xml -pl yudao-module-mes "-Dtest=MesOutputMaterialProgressCalculatorTest,MesTeamLeaderActiveOrderCompletionProgressPortImplTest,MesTeamLeaderOrderProcessCompletionServiceTest" test` -> PASS，24 tests, 0 failures, 0 errors, 0 skipped，`BUILD SUCCESS`。
