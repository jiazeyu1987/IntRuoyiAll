# 目标 3+4：工艺路线版本与资源快照、报工 Excel 导入归属

## 任务目标

在新的 worktree 中完成排产闭环第二阶段：

- 排产工单生成时固化当时使用的工艺路线、工序、设备、人工、班次小时和产能，后续工艺路线或资源变更不影响已生成排产工单。
- 班组长导入 MES Excel 报工后，必须人工选择可归属的排产工单工序，系统不自动猜测归属；归属后更新排产工单工序进度。
- 验证成功后融合进 `int_main`。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。以排产工单工序快照作为排程事实来源，避免继续依赖可变工艺路线。
- `是否存在临时补丁或绕过`：否。

## 里程碑

1. 建立 worktree 与任务文档。
2. 扩展排产工单生成快照：路线版本自动编号、工序资源快照、产能快照。
3. 收紧报工导入归属：只返回未完成/可归属排产工单工序，归属后更新进度，不做自动匹配成功。
4. 补齐数据库迁移、后端单元测试、SQL 合约测试。
5. 补齐前端展示/操作入口必要字段与静态契约测试。
6. 在测试租户执行真实 E2E。
7. 融合进 `int_main` 并在融合结果上回归验证。

## BDD 场景

- Given 排产员从 ERP 生产工单生成排产工单，When 当前产品已配置工艺路线与设备/人工资源，Then 排产工单必须保存路线版本、工序列表、设备/人工来源、班次小时、小时产能与班次产能快照。
- Given 已生成排产工单，When 后续修改工艺路线或资源配置，Then 已生成排产工单的快照不应随之变化。
- Given 班组长导入 MES Excel 报工，When 查看归属候选，Then 只能看到未完成且剩余数量足够的排产工单工序。
- Given 班组长选择一个候选排产工单工序，When 确认归属，Then 系统创建正式报工并增加该工序已报工数量、减少剩余数量。
- Given 导入报工没有足够剩余数量或没有已生成任务，When 班组长确认归属，Then 系统必须失败并给出明确错误，不自动兜底到其他工序。

## 预期验证

- Maven 目标测试通过。
- SQL 合约测试通过。
- 前端静态契约测试通过。
- 测试租户真实 E2E 通过，不修改 `芋道源码/admin` 数据。

## 验证证据

- 后端目标测试：`mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleOrderServiceImplTest,MesProFeedbackImportRecordServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS，7 tests。
- SQL 合约测试：`python -m pytest script\tests\test_mes_scheduling_closed_loop_sql.py` -> PASS，4 tests。
- 前端静态契约：`node tests\e2e\mes-pro-schedule-order-pool-static.spec.js` -> PASS。
- 测试租户目标 3 真实 E2E：`CODexERP20260610D` 生成排产工单 `id=11`，承诺交期 `2026-07-03`，数量 `127`，路线版本 `ROUTE-ROUTE-XLSX-00001-20260610-0002`，工序快照包含 `B010`、产能来源、班次小时与班次产能。
- 测试租户目标 4 真实 E2E：导入 Excel 生成待归属记录 `id=134`，手动选择任务 `TASK-CODEX-20260610-D-B010` 对应排产工序 `id=241`，创建正式报工 `id=134`。
- 只读 SQL 证据：`mes_pro_feedback_import_record.id=134` 为 `ATTRIBUTED`；`mes_pro_feedback.id=134` 关联 `schedule_order_id=11`、`schedule_order_process_id=241`、`feedback_quantity=5.00`；`mes_pro_schedule_order_process.id=241` 从 `planned=127` 更新为 `reported=5`、`remaining=122`。
- 融合后目标测试：后端目标 Maven 测试 PASS，SQL 合约 PASS，前端静态契约 PASS。
- 融合后主运行时真实 E2E：`8081/48081` 测试租户 `CODexERP20260610E` 生成排产工单 `id=12`，承诺交期 `2026-07-04`，数量 `128`。
- 融合后报工归属 E2E：导入记录 `id=135` 手动归属到排产工单 `12` 工序 `265`，创建报工 `id=135`；只读 SQL 确认 `reported=6`、`remaining=122`。

## 当前状态

已完成。目标 3+4 已提交、融合进 `int_main`，并在融合后的主运行时使用测试租户完成真实 E2E 回归。
