# Execution Log: 同步工单已入池显示开关

- User intent: 在截图红框位置增加 switch 开关，可显示或隐藏已经加入排产池的订单。
- Scope: 仅修改排产工单页面“同步工单”页签筛选/操作区、同步工单查询状态及本任务静态契约。
- Non-goals: 不修改后端 API、不改变入池提交接口、不改变排产工单主列表、不引入兼容 fallback。
- Dirty workspace note: 任务开始前仓库已有大量与本需求无关的脏改动和本地 ahead 状态，本任务不回滚、不覆盖这些改动。
- `BDD: 同步工单默认隐藏已入池订单 -> Given 排产员打开排产工单页面并切换到同步工单页签 / When 页面首次加载同步工单列表 / Then 查询参数默认不包含已加入排产工单池的生产工单，列表聚焦可入池或需处理订单。`
- `BDD: 开关显示已入池订单 -> Given 排产员停留在同步工单页签 / When 打开“显示已入池订单”开关 / Then 页面重新查询第一页，并把已加入排产工单池的生产工单纳入列表展示。`
- `BDD: 重置恢复隐藏已入池订单 -> Given 排产员已打开显示已入池订单开关 / When 点击同步工单页签的重置按钮 / Then 开关恢复关闭状态并重新查询隐藏已入池订单的列表。`

## Milestone Updates

- M1 completed: 已创建任务文档并记录 BDD/TDD 验收口径。
- Experience gate: 已读取 `docs/experience-index.md`，命中并采用 `docs/frontend-development.md#前端静态契约隔离门禁`、`docs/e2e-rules.md#Element Plus 选择框显示门禁`、`docs/e2e-rules.md#E2E 脚本入口存在性门禁`。

## Verification Evidence

- Pending RED/GREEN.

## Blockers

- 当前工作区已有大量非本任务改动；本任务验证会优先使用定向静态契约，提交/推送收尾需在不混入无关改动的前提下处理。
- 只读检索 `rg admission-diff... IntRuoyiBackend IntRuoyiFronted/...` 触发历史损坏目录 `IntRuoyiBackend/yudao-module-mes/target_corrupt_m4_20260802_1327/...` OS error 1392；已改用源码文件定向读取，不影响本任务前端实现。
