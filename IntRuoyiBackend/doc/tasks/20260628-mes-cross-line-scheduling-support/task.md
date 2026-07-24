# 任务：MES 排产支持跨产线 后端实现

## 任务目标

- 修改 MES 自动排产后端，使同一工单不同工序可以落在不同产线。
- 去除“没有共同可用单产线”这一整单阻断约束。
- 保留单工序无可用工作站/产线、无排班计划、无产能等真实阻断。

## 当前状态

COMPLETED

## 上一任务检查

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260628-smart-scheduling-four-route-defaults\task.md`
- 状态：`COMPLETED`
- 处理说明：上一后端任务已完成，本次为新的排产核心行为变更。

## 经验门禁

- 来源：`D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
- 命中文档：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- 适用强制门禁：
  - 必须先写 RED 测试再改实现。
  - 仅做本机代码与单测改动，不做真实数据写入。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。去掉固定单产线算法假设，改成按工序真实资源求解。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 跨产线工序可继续排产 -> Given 两道工序分别只在不同启用产线上存在可用工作站 / When 预览自动排产 / Then 不再返回“没有共同可用单产线”阻断。`
- `BDD: 无可用工作站的工序仍阻断 -> Given 某道工序找不到任何可用工作站 / When 预览自动排产 / Then 仍返回明确的工序级阻断。`
- `BDD: 正式排程工单分析支持多产线展示 -> Given 同一工单在正式排程中不同工序位于不同产线 / When 查看工单产线分析 / Then 系统应展示多产线结果，而不是把它视为冲突异常。`

## 里程碑

1. M1：建立后端任务文档与变更基线。`COMPLETED`
2. M2：写 RED 测试，锁定新行为。`COMPLETED`
3. M3：实现算法变更并回归。`COMPLETED`
4. M4：完成测试与结论回填。`COMPLETED`

## 预期验证

- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest#preview_shouldAllowCrossLineSchedulingWhenEachProcessHasOwnEnabledLine,MesProScheduleCalendarServiceImplTest#getWorkOrderAnalysis_shouldAllowCrossLineTasksAndReturnCombinedLineSummary" test`
- `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" test`

## 完成结论

- 自动排产从“整单单产线”改为“按工序选线”，允许同一工单的不同工序落在不同启用产线。
- 工序级真实阻断仍保留：无工作站、无排班计划、无产能等情况继续明确报错。
- 正式排程工单分析不再把跨线工单标为冲突，而是输出多产线汇总结果。
