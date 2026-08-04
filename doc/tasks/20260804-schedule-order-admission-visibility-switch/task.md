# 任务：同步工单已入池显示开关

## Task Goal

在排产工单页面“同步工单”页签红框位置增加一个开关，用于显示或隐藏已经加入排产工单池的生产工单；默认隐藏已入池工单，避免排产员重复处理。

## Current Status

ready_for_closeout

## Milestones

- [x] M1：创建任务文档并记录 BDD/TDD 验收口径。
- [x] M2：补充 RED 静态契约，锁定同步工单页签必须存在已入池显示开关及查询参数绑定。
- [x] M3：实现最小前端开关、请求参数和重置逻辑，不改后端契约、不引入 fallback。
- [x] M4：运行定向验证与证据校验，记录 GREEN/REGRESSION 结果。

## Expected Verification

- `node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260804-schedule-order-admission-visibility-switch/frontend-feature-evidence.md`
- `pnpm ts:check:schedule`
- `pnpm ts:check` 若被无关历史问题阻塞，记录首个阻塞点。

## BDD Scenarios

- `BDD: 同步工单默认隐藏已入池订单 -> Given 排产员打开排产工单页面并切换到同步工单页签 / When 页面首次加载同步工单列表 / Then 查询参数默认不包含已加入排产工单池的生产工单，列表聚焦可入池或需处理订单。`
- `BDD: 开关显示已入池订单 -> Given 排产员停留在同步工单页签 / When 打开“显示已入池订单”开关 / Then 页面重新查询第一页，并把已加入排产工单池的生产工单纳入列表展示。`
- `BDD: 重置恢复隐藏已入池订单 -> Given 排产员已打开显示已入池订单开关 / When 点击同步工单页签的重置按钮 / Then 开关恢复关闭状态并重新查询隐藏已入池订单的列表。`

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；开关只改变正式查询参数，不吞接口错误。
- `是否从根因和长期维护角度解决`：是；将显示口径建模为同步工单查询状态和静态契约，而不是用前端当前页临时过滤。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- `前端静态契约隔离门禁`：本任务新增专用最小静态契约，只验证同步工单已入池显示开关，不修改历史大契约来绕过旧失败。
- `Element Plus 选择框显示门禁`：Switch 文案必须完整可见，不把主标签和状态提示挤入会裁切的窄列。
- `E2E 脚本入口存在性门禁`：本任务仅声明静态契约验证；不把静态测试冒充真实 Playwright 用户路径。

## Verification Result

- RED：`node tests/e2e/mes-schedule-order-admission-show-admitted-switch-static.spec.js` 先失败于同步工单 actions 工具栏缺少“显示已入池订单”开关。
- GREEN：专用静态契约通过，确认 Switch、默认关闭、查询参数切换、重置恢复和禁止本地过滤。
- REGRESSION：相邻同步工单默认状态、工具栏布局、批量入池和原因选项静态契约通过。
- TYPECHECK：`pnpm ts:check:schedule` 通过；全量 `pnpm ts:check` 被无关 `src/views/mes/qc/template/index.vue` 导出缺失阻塞。
- EVIDENCE：`frontend-feature-delivery` evidence validator 通过；RED/GREEN 摘要已复制到 `execution-log.md` 和 `verification-report.md`，允许 cleanup 删除临时 evidence 文件。
- CLEANUP：`task-closeout-cleanup` preview/apply 通过，仅删除本任务临时 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md` 和 `verification-report.md`。

## Remaining Closeout Blockers

- 全量 `pnpm ts:check` 存在无关 QC 模板 API 导出缺失，不能作为本任务通过证据。
- 当前 `int_main` 仍存在其它任务的未提交/已暂存改动和本地 ahead 状态；本任务不能在不混入无关改动的情况下完成独立提交/推送收尾。
