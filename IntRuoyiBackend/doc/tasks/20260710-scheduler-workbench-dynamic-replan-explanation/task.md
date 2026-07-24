# 排产员工作台动态重排说明

## 任务目标

- 为排产员工作台提供最近一次成功重排的权威说明数据。
- 在重排成功应用时保存订单顺序、工序、班次产能、受保护任务、物料计算、问题和任务变更快照。
- 提供当前租户最近一次成功重排说明查询接口；预览或失败重排不得覆盖快照。

## 上一任务检查

- 后端上一任务 `doc/tasks/20260710-edhr-process-companion-forms/task.md` 状态为 `completed`，不阻塞本任务。

## 经验门禁

- PowerShell / UTF-8：已读取 `docs/powershell-memory.md`；中文文件显式按 UTF-8 处理，PowerShell 不使用 `&&`。
- 智能排产：已读取 `docs/agent-memory/project-error-prevention.md`；重排说明必须保存本次应用时的权威计算结果，不能用页面打开时的当前库存或当前配置重新计算。
- 数据库：新增表和迁移必须同步初始 schema、迁移脚本及发布契约测试；写 SQL 前以当前真实 schema 和现有迁移模式为准。
- BDD + 严格 TDD：先记录 Given/When/Then，再写失败测试，最后最小实现和回归。
- 无 fallback：快照保存失败必须让重排事务失败，不得静默应用排产结果。
- Worktree：后端工作目录为 `D:\ProjectPackage\Int\IntRuoyiWorktrees\20260710-scheduler-workbench-dynamic-replan-explanation\ruoyi-vue-pro`，分支为 `codex/20260710-scheduler-workbench-dynamic-replan-explanation`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；使用一次成功重排一条快照的正式数据模型，避免在操作日志中重复存储大体量明细。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- BDD: 成功应用人工重排后生成说明 -> Given 用户完成一次可应用的人工重排 / When 重排事务提交 / Then 保存一条包含本次完整计算数据且来源为人工的说明快照。
- BDD: 夜间重排生成相同口径说明 -> Given 夜间任务成功应用重排 / When 重排事务提交 / Then 保存相同结构且来源为夜间自动的说明快照。
- BDD: 物料计算包含充足与短缺物料 -> Given 重排订单需要多种物料 / When 系统计算库存 / Then 快照记录每种物料的需求量、可用量、短缺量及订单贡献。
- BDD: 工序产能保留实际计算值 -> Given 重排包含多道工序 / When 系统完成产能计算 / Then 快照记录班次、工作站、设备、人员、每小时产能、预计时长和瓶颈标记。
- BDD: 失败重排不覆盖说明 -> Given 已存在成功重排快照 / When 新重排预览或应用失败 / Then 最近成功快照保持不变。
- BDD: 快照写入失败回滚重排 -> Given 排产任务已准备写入但快照持久化失败 / When 应用重排 / Then 整个事务失败且不留下部分排产结果。
- BDD: 查询仅返回当前租户最近记录 -> Given 多个租户存在多次成功重排 / When 当前租户查询重排说明 / Then 只返回本租户最新一条记录。

## 里程碑

1. [完成] 建立任务文档、BDD、数据库和后端证据骨架。
2. [完成] 新增快照 schema、迁移契约及失败测试。
3. [完成] 扩展重排计算结果并在成功应用事务中保存快照。
4. [完成] 新增当前租户最近一次重排说明查询接口。
5. [完成] 运行定向测试、构建回归和证据校验。
6. [进行中] 完成真实链路验证、提交、融合和收尾。

## 预期验证

- 定向服务测试覆盖人工/夜间、物料汇总、工序产能、失败不覆盖和事务回滚。
- Mapper/集成测试覆盖租户隔离、唯一请求编号和最新记录查询。
- SQL 契约测试覆盖初始 schema、迁移脚本、索引和唯一约束。
- 后端证据与数据库证据校验脚本通过。
- 真实测试租户重排后，查询接口返回与本次应用一致的快照。

## 已完成工作

- 新增 `mes_pro_replan_explanation_snapshot`，按租户和重排请求保存一次成功应用的完整结构化快照。
- 人工重排和夜间自动重排共用快照生成逻辑；快照写入位于重排事务中，写入失败会直接回滚。
- 新增当前租户最近一次成功重排说明查询接口，空数据返回明确无数据状态，损坏快照直接报错。
- 快照包含订单顺序、工单工序、实际产能、保护任务、完整物料计算、问题及最终任务变更。

## 验证结果

- 后端定向测试：63 个测试通过。
- SQL 契约脚本：通过迁移脚本、初始 schema、H2 测试 schema、索引和唯一约束校验。
- 后端完整打包：`mvn.cmd -pl yudao-server -am -DskipTests package` 通过。
- 真实 E2E：测试租户通过前端连续应用两次 `TESTERP62AF41D87EFA` 重排，生成快照 `id=1` 和 `id=2`；第二条记录成为最新说明。
- 收尾：`task-closeout-cleanup` preview/apply 已清理临时证据并快进融合到 `int_main`；脚本删除空 worktree 目录时遇到 Windows 权限占用，目录已确认位于本任务 worktree 且为空后手动删除。
- 融合后验证：`python -X utf8 script/tests/test_mes_replan_explanation_snapshot_sql.py`、后端 63 个定向测试和 `git diff --check` 均通过。

## Cleanup Candidates

- `output/e2e-runtime/`

## Current Status

completed
