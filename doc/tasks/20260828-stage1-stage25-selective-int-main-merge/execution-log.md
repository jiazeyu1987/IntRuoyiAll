# Execution Log

## 2026-08-28

- User intent: 将当前 worktree 中与 stage1、stage2.5 相关的代码融合进 `int_main`，其它内容不融合。
- Preflight: 已读取 `docs/backend-development.md`、`docs/powershell-memory.md`、`docs/worktree-restrictions.md`、`docs/task-closeout-rules.md`、`docs/powershell-encoding.md` 和 `docs/experience-index.md`。
- BDD: Stage2.5 消费 Stage1 正式来源 -> Given stage1 生成可清理的真实活跃订单、正式领料/出库来源和 100% 进度事实；When stage2.5 执行模拟完工；Then stage2.5 必须调用正式活跃订单完成节点、生成统一回填 receipt，并把该 receipt 交给 Flow6 创建/复用真实批次执行。
- BDD: 选择性融合边界 -> Given 源 worktree 同时包含 stage4/5/6 及其它模块差异；When 融合到 `int_main`；Then 只允许 stage1/stage2.5 目标路径进入主干工作区。
- Preflight result: `E:\IntRuoyi` 当前在 `int_main`，存在大量既有 dirty/untracked，但 stage1、stage2.5 后端目标路径当前无同文件脏改动。
- Merge scope: 只复制 5 个文件：stage1 simulation service、活跃订单 simulation service、对应 JUnit、stage2.5 handoff JUnit、stage2.5 static contract。未复制 stage4/5/6、DCC、BPM、系统登录、配置文件和 SQL 差异。
- Stage2.5 implementation check: `MesStage2_5BackfillBatchExecutionSimulationServiceImpl.java` 与源 worktree 仅为换行哈希差异，`git diff --no-index --ignore-space-at-eol` 无内容差异，因此未重复复制。
- RED: 本轮没有重新制造失败版本；本次任务是已验证 worktree 切片的选择性融合，RED 证据沿用源 worktree stage2.5 缺少正式出库来源导致真实完成回填不可用的问题。
- GREEN: `node IntRuoyiBackend\yudao-module-mes\src\test\js\mes-active-order-stage2-5-static.spec.cjs` -> PASS，输出 `mes-active-order-stage2-5-static: PASS`。
- GREEN: `mvn -pl yudao-module-mes -am "-Dtest=MesStage2_5ReceiptHandoffContractTest,MesTeamLeaderActiveOrderSimulationServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，MES reactor 24/24 SUCCESS，Tests run: 6, Failures: 0, Errors: 0, Skipped: 0。
- Boundary verification: `git diff --check -- <selected paths>` -> PASS。
- Remaining context: 主干仍有大量既有 unrelated dirty/untracked 改动，本任务未清理、未暂存、未覆盖这些文件。
- Commit: `17b48e9bb fix(mes): merge stage1 stage2.5 simulation handoff`，提交 5 个 stage1/stage2.5 代码/测试文件和任务记录。
- Cleanup preview/apply: `task_closeout.py --task-id 20260828-stage1-stage25-selective-int-main-merge --mode preview/apply` -> PASS；删除临时 `backend-api-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
- Final status: completed。
