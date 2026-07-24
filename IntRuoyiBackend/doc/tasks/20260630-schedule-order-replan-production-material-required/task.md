# 任务：手动重排仍提示工单缺少生产用料清单（后端）

- Task ID: `20260630-schedule-order-replan-production-material-required`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `blocked`

## Task Goal

定位并修复 `SCH-881MO090863-20260612-0001` 在手动重排 / 应用重排时被 `PRO_AUTO_SCHEDULE_PRODUCTION_MATERIAL_REQUIRED` 阻断的问题。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-erp-material-list-missing-local-sync-analysis\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成生产用料清单手工历史回补修复与真实回补验证，本次继续闭环排产重排报错。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Java/SQL/Markdown 输出统一显式 UTF-8；PowerShell 5.1 不使用 `&&`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\integrations\kingdee-erp-official-docs.md`
  - ERP 同步差异必须以真实库字段、正式同步链路和真实单据口径为准，不凭页面猜测。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先查清当前租户工单为什么没有命中生产用料清单，再决定修同步映射还是修重排逻辑。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 手动重排命中已具备生产用料清单的工单时不再误报缺失 -> Given 排产单 SCH-881MO090863-20260612-0001 对应工单在本地已具备可识别的生产用料清单 / When 用户点击手动重排并应用重排 / Then 系统不应再以“工单缺少生产用料清单”阻断。`
- `BDD: 真实缺少生产用料清单的工单仍显式暴露原因 -> Given 某工单在当前租户下确实没有生产用料清单明细 / When 用户手动重排 / Then 系统仍需暴露真实缺口，不得静默放过。`

## Milestones

1. M1：建立后端任务文档与日志。`completed`
2. M2：复核真实库中排产单、工单、生产用料清单关联现状。`completed`
3. M3：定位根因并补 RED 回归测试。`completed`
4. M4：最小修复并验证 GREEN。`blocked`

## Expected Verification

- 真实库查询
- `MesProAutoScheduleServiceImplTest` 定向回归测试

## Current Blockers

- 本机后端定向回归测试已通过，且本机 48081 运行态已重启到最新代码；但真实前端 E2E 当前在页面筛选后找不到 `SCH-881MO090863-20260612-0001` 这一行，无法继续完成“手动重排不再报工单缺少生产用料清单”的页面级最终验证。当前阻塞属于真实样本/页面状态漂移，需先确认该排产单当前是否仍在本机测试租户可见范围内，或提供新的可复现样本。
