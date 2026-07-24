# 任务：MES 工单清空全部排产阻断 后端实现

## 任务目标

- 继续排查工单 `TESTERPA9ED2D417434` 的剩余排产阻断。
- 按真实前置逐项修复，直到该工单不再被后端排产逻辑判为阻断。
- 所有修改仅限本机范围并保持可追溯。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-mes-process-line-blocker-fix\task.md`
- 状态：`COMPLETED`
- 处理说明：已补齐首批缺失产线绑定，本次继续清空剩余阻断。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
- 适用强制门禁：
  - 真实库查询与日志写入必须显式 UTF-8。
  - 真实库写入前必须记录 `GREEN: experience-preflight -> PASS`。
  - 真实登录与自动排产预览前必须先执行官方 `login-preflight.mjs`。
  - 每次修复后必须做只读回查或真实预览复验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。按真实阻断项继续补齐前置。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工单所有排产前置齐备后不再阻断 -> Given 工单涉及的工作站、产线、排班计划与产能前置均已满足 / When 执行排产预览 / Then 系统不再返回该工单的阻断项。`
- `BDD: 仍有缺失时继续暴露真实阻断 -> Given 工单还存在其它缺失前置 / When 执行排产预览 / Then 系统继续返回下一条真实阻断，而不是静默通过。`

## 里程碑

1. M1：建立后端任务文档并继承上一轮修复结果。`COMPLETED`
2. M2：查询当前剩余阻断。`COMPLETED`
3. M3：逐项修复并回查。`COMPLETED`
4. M4：确认该工单已无阻断。`COMPLETED`

## 预期验证

- 真实库核对该工单涉及的工作站、产线、排班计划与产能数据。
- 必要时通过后端真实预览口径复验该工单阻断是否清空。

## 完成结论

- 已通过本机后端真实口径复验工单 `TESTERPA9ED2D417434`：
  - `workOrderId=925553`
  - `scheduleOrderId=48`
  - `route_id=900026`
- 当前 `/admin-api/mes/pro/auto-schedule/preview` 返回 `blockingIssueCount = 0`，说明该工单已无自动排产阻断。
- 当前剩余提示均为 warning，不属于阻断：
  - `MATERIAL_DEMAND`
  - `LATEST_START`
- 之前导致阻断的 `route_id=900026` 后续 9 个工序工作站已全部绑定到启用产线 `900040`，并通过真实预览复验生效。
