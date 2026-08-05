# AC-M18 更新生产订单进度修复执行日志

## User Intent

- 用户要求对 AC-M18“更新生产订单进度”的系统代码不符合项进行修复。
- 当前已知缺口：班组长确认分配只更新工序完成池表，未同步正式排产工单工序进度和工单汇总；正式进度同步存在超目标被截断为 100% 而非 fail-fast 的行为。

## Preconditions

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/backend-development.md`。
- 已读取 `bug-regression-fix-loop`、`backend-api-delivery` 技能及其 evidence contract。
- `docs/experience-index.md` 存在；本任务命中 Maven `-D` 参数引号、Maven Reactor 与 Windows Maven 增量卡住门禁，验证命令将按对应规则执行并记录真实结果。

## Milestone Log

- BDD: Confirmed allocation updates formal schedule progress -> Given a team leader confirmed allocation for a schedule order process with target quantity, When the allocation is applied, Then the process-level reported quantity and order-level summary are updated from the process target while ERP product quantity remains unchanged.
- BDD: Over-target schedule progress is blocked -> Given a schedule order process target quantity, When feedback or confirmed allocation would make reported quantity exceed the target, Then the update fails fast instead of capping progress to 100%.
- BDD: Concurrent remaining target consumption is blocked -> Given another allocation has consumed the remaining target before the current confirmation applies, When the current allocation is applied, Then the service rejects the update and does not over-report formal schedule progress.
