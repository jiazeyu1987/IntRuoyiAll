# MES 排产目标 7 闭环回归证据

## Scope

目标 7 要验证目标 1-6 在测试租户中形成真实闭环：

## Matrix

| Requirement | Verification | Evidence |
| --- | --- | --- |
| 目标 1 ERP 生产订单同步 | 生产工单列表存在 ERP 工单编码样本，唯一键为工单编码 | Playwright 打开生产订单并搜索工单编码 |
| 目标 2 排产工单池 | 排产工单数量等于生产工单数量，存在承诺交期，不拆分 | Playwright 打开排产工单池并搜索 ERP 工单 |
| 目标 3 路线版本与资源快照 | 排产工单存在 `ROUTE-` 版本、工序快照、产能来源、班次小时、班次产能 | Playwright 点击工序快照并读取接口响应 |
| 目标 4 报工 Excel 导入归属 | MES 导入记录已人工归属到排产工单和工序 | Playwright 打开生产报工待归属页，最终校验已归属记录 |
| 目标 5 夜间自动重排 | 工作台展示夜间重排说明；后端回归验证 Job 与保护规则 | Playwright 工作台 + 后端回归命令 |
| 目标 6 排产员工作台增强 | 工作台展示今日产能、瓶颈建议、报工偏差、快捷入口 | Playwright 工作台真实导航 |

## Test

- 租户：`测试租户`
- 账号：`aoteman`
- ERP 工单编码：`CODexERP20260610E`
- 报工任务编码：`TASK-CODEX-20260610-E-B010`

## Execution

- RED: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence docs\qa\mes-scheduler-e2e-closure-evidence.md` -> FAIL，缺少标准 QA 证据结构与 RED/GREEN/Verification/Blockers 标记。
- RED: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` -> FAIL，`归属状态` 文本多匹配，E2E 定位不唯一。
- RED: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` -> FAIL，同登录态最终校验缺少前端鉴权 header。
- RED: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` -> FAIL，token 缓存值需去除包裹引号。
- RED: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` -> FAIL，工作台瓶颈字段名应为 `bottlenecks`。
- GREEN: `python C:\Users\BJB110\.codex\skills\quality-assurance-test-suite\scripts\validate_quality_assurance.py --evidence docs\qa\mes-scheduler-e2e-closure-evidence.md` -> PASS。
- GREEN: `node --check tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` -> PASS。
- GREEN: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on `http://127.0.0.1:8095` -> PASS。
- RED: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on merged `int_main` `http://127.0.0.1:8081` -> FAIL，登录页没有显式租户输入时脚本仍尝试处理租户下拉，登录请求未触发。
- GREEN: `node tests\e2e\mes-scheduler-target7-closed-loop-real-flow.e2e.js` on merged `int_main` `http://127.0.0.1:8081` -> PASS。

## Verification

- Worktree 后端回归：33 个 MES 排产相关测试通过。
- Worktree SQL 回归：5 个闭环 SQL 契约测试通过。
- Worktree 前端静态契约：排产工单池和排产员工作台测试通过。
- Worktree 前端真实 E2E：`CODexERP20260610E` 样本通过，排产单 `12`，路线版本 `ROUTE-ROUTE-XLSX-00001-20260610-0003`，工序快照 `24` 条，已归属导入记录 `135`，今日可用产能 `586711.950243`，瓶颈项 `10`。
- 融合后 `int_main` 回归：后端 33 个测试、SQL 5 个测试、前端静态契约、QA 证据校验和真实 E2E 均通过。

## Blockers

- 当前无阻塞；若测试租户样本 `CODexERP20260610E` 或 `TASK-CODEX-20260610-E-B010` 被删除，目标 7 必须失败并重新补测试租户真实样本。
