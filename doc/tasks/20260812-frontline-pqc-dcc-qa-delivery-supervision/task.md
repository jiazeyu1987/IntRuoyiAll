# 一线PQC DCC-QA 14任务主管交付

## 任务目标

在设计包取得最终独立评审PASS后，严格按Wave 0至Wave 9调度C00、DF01至DF11、INT12和VAL13；每个实现任务使用独立分支和 `D:\IntRuoyiWorktree\` 下的独立worktree，经BDD、严格TDD、主管评审、独立验证和fast-forward合并后逐步进入 `int_main`。禁止push、部署、远程服务器操作和共享业务数据修改。

## 里程碑

- [x] M0：设计包最终独立评审PASS。
- [ ] M1：完成Git、int_main、并发修改、worktree与端口启动门禁。
- [ ] M2：完成Wave 0至Wave 7的C00和DF01至DF11。
- [ ] M3：完成Wave 8 INT12全链路集成。
- [ ] M4：完成Wave 9 VAL13独立验收。
- [ ] M5：确认14任务全部合并、清理worktree并完成最终报告。

## 预期验证

- 每个任务都有BDD、真实RED、GREEN、回归和独立验证证据。
- 每个实现提交只包含该任务归属文件，且主管复核完整diff。
- 每个分支在合并前吸收最新 `int_main` 并重跑验证，最终仅fast-forward合并。
- INT12通过真实全链路验证，VAL13由未参与实现的独立Agent完成。
- 最终报告包含任务、分支、worktree、提交号、验证和合并状态。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是；以独立评审通过的正式合同和逐波次验证为唯一开发基线。
- 是否存在临时补丁或绕过：否。

## 适用经验门禁

- 一线PQC DCC-QA：正式规则key必须区分 `FIRST/PATROL_AM/PATROL_PM/FINAL`；同任务提交必须严格串行化；历史锁定QA读取不得受DCC当前启用状态影响。
- 跨分支集成：各任务单测不能代替合并后的真实Bean注入和接口组合回归；共享文件必须按波次串行移交。
- 脏主工作区融合：创建任务分支以已提交 `int_main` 为基线；合并前计算任务真实增量与主工作区未提交文件交集，存在无法归属的重叠时立即停止。
- 状态文件：主管独占 `task-state.json`，所有状态写入串行执行并在写后复读；子Agent不得修改。
- 暂存和提交：只使用明确任务路径，禁止 `git add -A`、回滚或清理并发任务改动；不执行push。

## Current Status

in_progress：M0/M1完成；Wave 0 C00 已完成主管复核、独立验证、本地提交和 fast-forward 合入 int_main，提交号 a1c032581。Wave 1 的 DF01、DF02、DF03、DF05 均为 ready；DF01/DF02/DF03 worktree 已创建并登记端口，但首轮执行 Agent 因越界/无可见产出已暂停，后续需重派或由主管接手。主工作区仍有大量既有并发改动，后续每个任务合并前必须继续复核实际交集。

## Cleanup Keep

- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/task.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/request-analysis.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/prd.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/dev-plan.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/test-plan.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/task-state.json
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/execution-log.md
- doc/tasks/20260812-frontline-pqc-dcc-qa-delivery-supervision/test-report.md
