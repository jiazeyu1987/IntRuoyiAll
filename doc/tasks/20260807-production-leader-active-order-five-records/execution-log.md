# 执行日志

## User Intent And Scope

- User request: 给生产组长的活跃订单池增加 5 条符合条件的数据。
- Interpretation: 仅操作本机 `int_main` 数据；创建 5 条可追踪且真实满足当前候选资格的订单数据，并通过生产组长页面加入活跃订单池。
- Boundary: 不访问远端，不修改产品代码，不直接写 `mes_pro_process_pool_active_order`、工序快照或 PQC 任务表，不修改共享正式路线/QA 规程。

## BDD

- BDD: 生产组长获得 5 条合格活跃订单 -> Given 本机确认的业务租户和生产组长账号已有完整正式产品/路线/ACTIVE 版本/工序/QA 规程组合，且 5 个任务订单均为已确认、正数 ERP 数量、唯一有效排产、启用工序、计划数量一致并包含计划日期，When 生产组长通过活跃订单池远程候选下拉逐条选择并加入，Then 页面新增 5 条 ACTIVE 订单，后端为每条订单生成正式工序快照和 PQC 任务，候选资格与写入结果一致。
- BDD: 任一正式前置缺失时不写入 -> Given 任一任务订单缺少唯一排产、路线/版本、启用工序、数量因子、计划日期、唯一已发布 QA 规程或完整首检/巡检/末检规则，When 执行候选预检，Then 该订单明确显示不可加入原因，任务停止且不直接写活跃订单或相关子表。

## RED / GREEN Evidence

- RED: pending - 写入前任务前缀对应的合格候选和 ACTIVE 活跃订单必须为 0。
- GREEN: pending - 5 个任务订单候选均 `eligible=true`，真实页面 5 次加入均成功，最终 5 条 ACTIVE 活跃订单及关联快照/PQC 任务通过只读复核。

## Command Intent

- READ: 已读取 `docs/task-closeout-rules.md`、`docs/database-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/login-access.md`、`docs/local-runtime.md`、`docs/e2e-rules.md`、`docs/server-access.md`、`docs/release-backup-restore.md`。
- SKILL: 已读取 `database-schema-delivery` 及 database evidence contract；已读取 `playwright` 技能并采用真实页面写入路径。
- SAFETY: 本任务默认仅操作本机，不访问 `172.30.30.57/58/59`，不输出数据库密码、登录密码或 token。

## Milestone Status

- M1 completed：资格合同、运行环境边界、禁止直接写活跃订单的门禁已确认。
- M2 in_progress：正在核对 schema、当前生产组长身份与完整 QA 规程组合。

## Blockers

- 当前共享 `int_main` 分支存在其它并发任务提交和未提交文件；本任务暂不执行宽泛暂存、基线提交或推送，提交前必须重新复核归属与并发状态。

