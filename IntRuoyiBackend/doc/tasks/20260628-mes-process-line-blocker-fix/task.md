# 任务：MES 工序产线阻断排查修复 后端实现

## 任务目标

- 排查工单 `TESTERPA9ED2D417434` 在排产中对工序 `棘突丝拉伸2` 的阻断原因。
- 确认是否为工作站缺失、工作站未绑定产线、产线未启用或其它后端前置导致。
- 在本机范围内完成最小修复并验证。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-mes-cross-line-scheduling-support\task.md`
- 状态：`COMPLETED`
- 处理说明：跨产线后端逻辑已完成，本次为具体工单阻断数据/行为跟进。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - PowerShell 5.1 下真实库查询与日志写入必须显式 UTF-8。
  - 涉及数据库真实写入前必须先记录 `GREEN: experience-preflight -> PASS` 或明确阻塞。
  - 先查清真实缺失点，再执行最小修复。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先修正真实配置缺失，必要时再修后端判断逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工序存在可用工作站后不再阻断 -> Given 工序棘突丝拉伸2具备已启用且已绑定产线的工作站 / When 执行排产预览 / Then 系统不再返回该工序阻断。`
- `BDD: 无工作站时继续阻断 -> Given 工序仍无任何可用工作站或产线绑定 / When 执行排产预览 / Then 系统继续明确阻断。`

## 里程碑

1. M1：建立后端任务文档并锁定阻断对象。`COMPLETED`
2. M2：只读查询真实库并定位根因。`COMPLETED`
3. M3：执行最小修复与验证。`COMPLETED`
4. M4：回填结论。`COMPLETED`

## 预期验证

- 真实库只读核对 `mes_pro_work_order`、工艺路线、工作站、产线、启用状态。
- 如发生写入，需复验该工序满足排产前置。

## 完成结论

- 工序 `棘突丝拉伸2` 的真实阻断原因为：对应工作站 `900113` 存在，但未绑定任何产线。
- 为避免该路线后续工序继续同类阻断，本次已将 `route_id=900026` 下同批缺失产线绑定的 9 个工作站统一绑定到 `AUTO-LINE-01(id=900040)`。
- 修复后这些工序已满足“工作站已绑定启用产线”的最基本排产前置。
