# 验证报告

## 结论

PASS - 本机 tenant 1 的生产组长活跃订单池已新增 5 条符合正式候选条件的数据，真实页面写入、最终页面展示和数据库关联完整性均通过。

## 交付数据

| 工单号 | 工单 ID | 活跃订单 ID | 资格 | 页面写入 |
| --- | ---: | ---: | --- | --- |
| `CODX-AO5-20260807-01` | 980022 | 35 | `eligible=true` | HTTP 200 / 业务码 0 |
| `CODX-AO5-20260807-02` | 980023 | 36 | `eligible=true` | HTTP 200 / 业务码 0 |
| `CODX-AO5-20260807-03` | 980024 | 37 | `eligible=true` | HTTP 200 / 业务码 0 |
| `CODX-AO5-20260807-04` | 980025 | 38 | `eligible=true` | HTTP 200 / 业务码 0 |
| `CODX-AO5-20260807-05` | 980026 | 39 | `eligible=true` | HTTP 200 / 业务码 0 |

## 验证证据

- 真实入口：`http://127.0.0.1:8081/mes/pro/process-pool/production-leader`。
- 页面身份：tenant `芋道源码`，用户 `admin`，对应 leader_user_id `1`。
- 页面行为：通过生产组长“新增活跃订单”远程候选逐条选择和加入；未直接写活跃订单、快照或 PQC 任务表。
- 页面结果：活跃订单池显示 ID `35..39`，工单 ID `980022..980026`，路线 `980091`、版本 `622`、数量 `10.000`、状态均为“活跃”。
- 请求结果：5 次加入均为 HTTP 200、业务码 0；`targetFailures=[] / consoleErrors=[] / pageErrors=[]`。
- DB 结果：5 条活跃订单均为 tenant 1、leader_user_id 1、状态 `ACTIVE`；每条均有 1 条工序快照和 4 条 PENDING PQC 任务，类型分布为 FIRST 1、PATROL 2、FINAL 1。
- 审计结果：`ADD_ACTIVE_ORDER / ACTIVE_ORDER / SUCCESS` 共 5 条。
- 视觉检查：截图中 5 条记录完整可见，无文本遮挡、错位或失败提示。

## 安全与范围

- 仅操作本机 `int_main`，未访问远程服务器。
- 仅新增任务前缀 `CODX-AO5-20260807-` 对应的数据及正式服务生成的关联记录。
- 首轮 fixture 因排产快照超过 `TEXT` 容量而完整回滚；未截断数据或保留半成品。
- 未修改产品代码、共享正式路线、路线版本、工序或产品主数据。

## 当前状态

completed - 交付和验证已完成；database evidence validator、经验检查、task-closeout-cleanup preview/apply 均通过。临时执行产物已清理，业务数据和三份正式任务记录已保留。
