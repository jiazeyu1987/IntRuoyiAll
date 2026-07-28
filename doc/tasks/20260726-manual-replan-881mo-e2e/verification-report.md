# Verification Report

## Scope

- 使用 Playwright 真实浏览器访问 `http://127.0.0.1:8081`。
- 目标租户 id：`1`。
- 目标来源生产工单号：`881MO093613`、`881MO093615`。
- 执行路径：排产工单页签筛选目标工单，勾选两个目标行，点击“手动重排”，在弹框中点击“开始重排”，确认应用重排。

## Result

- `checkpoint 1`：PASS；页面提示 `应用重排成功：正式排程已更新，新增任务 136 个，删除任务 136 个，保留任务 7 个。`，apply 请求 HTTP=200，业务码 `0`。
- `checkpoint 2`：PASS；仅 `881MO093613/YXN.069.001.1013` 与 `881MO093615/YXN.069.001.1002` 的产品编号具有橙色已排产样式 `rgb(212, 107, 8)`。
- `checkpoint 3`：PASS；最近一次成功排产记录 `operationType=REPLAN_APPLY`，`appliedAt=1785036095000`，页面显示 `2026-07-26 11:21:35`，位于本次执行窗口内。
- `checkpoint 4`：PASS；生产排产甘特图接口与折叠后 UI 的来源生产工单号集合均为 `881MO093613,881MO093615`。

## Evidence

- Command: `node doc\tasks\20260726-manual-replan-881mo-e2e\manual-replan-881mo-current.e2e.cjs`
- Evidence JSON: `output\playwright\20260726-manual-replan-881mo-e2e\e2e-evidence.json`
- Final status: PASS
