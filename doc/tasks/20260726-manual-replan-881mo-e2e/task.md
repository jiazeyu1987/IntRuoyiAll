# 排产工单手动重排 E2E 验证

## Task Goal

- 使用 Playwright 真实浏览器访问 `http://127.0.0.1:8081`，在租户 `1` 下验证来源生产工单号 `881MO093613`、`881MO093615` 的排产工单手动重排流程。

## Milestones

- [x] 完成任务规则、E2E、登录、运行端口和经验门禁预检。
- [x] 通过真实页面筛选并选择两个目标排产工单。
- [x] 执行“手动重排 -> 开始重排 -> 确认应用重排”。
- [x] 验证四个检查点并记录真实观察结果。

## Expected Verification

- 页面提示“应用重排成功”，后端 apply 请求业务码为 `0`。
- 仅来源生产工单号为 `881MO093613`、`881MO093615` 的两个排产工单产品编号具有橙色已排产样式。
- 最近一次成功排产时间更新为本次手动重排执行时间，`operationType=REPLAN_APPLY`。
- 生产排产甘特图接口和折叠后 UI 有且仅有 `881MO093613`、`881MO093615` 两个工单。

## Current Status

- ready_for_closeout

## Verification Evidence

- 2026-07-26 11:21:44：`node doc\tasks\20260726-manual-replan-881mo-e2e\manual-replan-881mo-current.e2e.cjs` 执行完成，四个检查点全部 PASS。
- 证据文件：`output\playwright\20260726-manual-replan-881mo-e2e\e2e-evidence.json`。

## 经验门禁

- Element Plus 表格选择门禁：写入动作前必须按页面可见业务唯一文本定位目标行，断言已选集合仅包含目标来源生产工单号，不得使用表头全选、数组下标、API-only 或坐标猜测。
- 真实 E2E 门禁：必须使用 Playwright 操作真实前端页面；API 仅可用于最终只读核验，不得替代真实用户路径。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，本任务为独立 E2E 验证，不修改产品实现。
- `是否存在临时补丁或绕过`：否。
