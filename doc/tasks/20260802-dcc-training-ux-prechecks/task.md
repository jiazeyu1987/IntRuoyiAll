# DCC 培训阅读确认前端操作优化

## Task Goal

修复 DCC 文控培训/阅读确认链路中的前端操作痛点：让培训计时状态和确认按钮禁用原因可见，让管理详情页更容易看到完成率/未完成名单，并在正式下发权限不足时给出明确业务提示，而不是让用户误判流程卡住。

## Milestones

1. 梳理培训任务页、管理详情页、正式下发按钮和现有静态契约。
2. 记录 BDD 场景并新增任务专用 RED 静态契约。
3. 最小范围实现培训计时提示、管理汇总提示和正式下发权限缺口提示。
4. 运行目标静态契约与相邻前端检查，记录 GREEN/REGRESSION。
5. 更新验证报告并标记 ready_for_closeout。

## Expected Verification

- 静态契约证明培训任务页显示计时状态、按钮禁用原因、聚焦/预览/时长不足提示。
- 静态契约证明 DCC 详情页显示培训完成汇总、未完成名单，并在不可正式下发但流程待下发时显示权限提示。
- 不改变后端接口契约，不新增 fallback，不吞异常，不扩大到其它 DCC 场景。
- 若现有全量检查被无关历史失败阻塞，记录无关 blocker，并以任务专用静态契约作为当前行为 GREEN 证据。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过前端显式状态和前置提示降低真实业务阻塞定位成本，不用 API-only 或隐藏错误替代。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 前端静态契约隔离门禁：若全量 `pnpm ts:check` 或既有大契约先失败在无关历史问题，新增任务专用最小静态契约，记录 RED/GREEN 和无关 blocker。
- 前端权限页签/权限提示门禁：权限不足不得用空白、隐藏错误或吞 403 冒充成功；本任务只提示 DCC 正式下发权限缺口，不扩大角色授权。
- DCC 文控审批处理入口门禁：涉及发布/下发入口时必须区分真实处理态和只读 viewer；本任务只改前端提示，不改变发布写入路径。
- Playwright/目标链路归因门禁：如运行真实页面验证，必须区分目标 DCC 链路错误与外部资源异常。

## Cleanup Keep

- doc/tasks/20260802-dcc-training-ux-prechecks/task.md
- doc/tasks/20260802-dcc-training-ux-prechecks/execution-log.md
- doc/tasks/20260802-dcc-training-ux-prechecks/verification-report.md
- doc/tasks/20260802-dcc-training-ux-prechecks/frontend-feature-evidence.md
