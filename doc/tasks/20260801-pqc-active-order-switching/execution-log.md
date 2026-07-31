# Execution Log

## User Intent

用户要求实现并验证 PQC 检验员切换来源：

- 切换订单来源是当前活跃订单。
- 切换工序来源是选择的活跃订单对应产品的工艺路线工序。
- 切换员工来源是所有 PQC 员工 + PQC 组长。
- PQC 组长可查看每个 PQC 检验员提交内容，列表内容与检验员填写内容一致，可判定正确性、修正错误内容，并记录提交和修改日志。
- 需确认该口径与生产组长任务不冲突。

## BDD Scenarios

BDD: PQC order selector uses active orders -> Given a PQC inspector opens the fixed template panel / When the order selector loads / Then only active orders are returned and all-order fallback is not allowed.

BDD: PQC process selector uses selected active order route -> Given a PQC inspector selected an active order with product route / When the process selector loads / Then processes come from that product route and missing route fails visibly.

BDD: PQC employee selector uses PQC personnel -> Given a PQC inspector opens the employee selector / When personnel options load / Then the options include all PQC employees and PQC leaders, not unrelated employees.

BDD: PQC leader review is consistent with inspector submissions -> Given PQC inspectors submitted inspection content / When a PQC leader opens the review list / Then list content matches submitted content and correction/submission logs are available.

## Commands And Evidence

- Read backend/frontend/database delivery skills and project trigger rules before implementation.
- Created task directory `doc/tasks/20260801-pqc-active-order-switching/`.

## RED

待补充。

## GREEN

待补充。

## Regression

待补充。

## Blockers

无。

