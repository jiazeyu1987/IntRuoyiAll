# 任务：排程日历日期产能覆盖窗口自动续期

- Task ID: `20260630-schedule-calendar-capacity-horizon-renewal`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `completed`
- User Request: `我希望可以从根本解决这个问题,不是临时方案,比如换了产线,换了日期,换了产品就又报错了`

## Task Goal

从机制上修复 MES 排产/重排对 `mes_pro_capacity_plan` 未来覆盖窗口耗尽后再次报 `产线班次产能缺失` 的问题。正式方案必须保持“排产只认明确日期产能、继续 fail fast”，但要把未来日期产能的续生成变成统一机制，避免用户换产线、换日期、换产品后再次撞到覆盖边界。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-zhaojie-scheduler-button-permission-fix\task.md`
- 状态：`completed`
- 处理说明：上一任务已完成排产员按钮权限正式修复，不阻塞本次后端机制级修复。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - PowerShell 读取/记录中文文档与命令证据必须显式 UTF-8；不使用 `&&`。
- 共享日志说明：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\request-command-log.md` 当前长期存在多任务混合追加风险；本任务继续按规则补记，但提交边界以本后端任务目录与后端代码为准，不把共享日志作为默认提交范围。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。排产/重排在缺失显式日期产能时仍需 fail fast，不允许在排产过程中偷偷按固定班次兜底。
- `是否从根因和长期维护角度解决`：是。修复目标是统一的“日期产能覆盖窗口自动续期机制”，而不是为某天某产线补数据。
- `是否存在临时补丁或绕过`：否。不引入单天补 SQL、页面提示掩盖或人工运维依赖作为正式解法。

## BDD 场景

- `BDD: 覆盖窗口到期前自动续生成未来产能 -> Given 某启用产线已绑定排班计划且当前日期产能仅覆盖到未来某一天 / When 排产日历或排产计算需要访问更远日期 / Then 系统先按现有排程规则补齐缺失未来日期产能，再继续原有校验链路。`
- `BDD: 已有人工维护的日期产能仍不覆盖 -> Given 某产线某天某班次已存在有效日期产能 / When 自动续生成机制运行 / Then 系统只补缺口，不覆盖已有 capacity_minutes、enabled 或 remark。`
- `BDD: 无排班计划或无有效班次时仍 fail fast -> Given 某产线未绑定排班计划或规则下当天无可用班次 / When 自动续生成机制尝试补齐未来日期产能 / Then 系统不制造假产能，后续仍返回明确阻断。`
- `BDD: 自动/手动重排共享同一续期机制 -> Given 排产预览、重排预览和排程日历都依赖日期产能 / When 未来覆盖窗口不足 / Then 三者都通过同一后端机制补齐正式日期产能，不再出现“某入口修了，另一个入口还报错”的分叉。`

## Milestones

1. M1：建立任务文档并确认当前长期方案缺口。`completed`
2. M2：梳理排程日历/自动排产/重排对日期产能的读取与校验链路。`completed`
3. M3：先写 RED 回归测试，证明覆盖窗口到期后仍会再次报缺失。`completed`
4. M4：实现统一续期机制并补相关测试。`completed`
5. M5：运行 GREEN / REGRESSION / evidence 校验并回填结论。`completed`

## Expected Verification

- RED：
  - `mvn -pl yudao-module-mes "-Dtest=MesProScheduleCalendarServiceImplTest#<new-red-test>+MesProAutoScheduleServiceImplTest#<new-red-test>" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- GREEN / REGRESSION：
  - `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleCalendarServiceImplTest#getMonth_shouldRenewFutureCapacityCoverageBeforeFailFastValidation,MesProAutoScheduleServiceImplTest#preview_shouldEnsurePlannedCapacityCoverageBeforeLoadingCapacityPlans,MesProScheduleCalendarServiceImplTest#getMonth_shouldFailFastWhenCapacityMissing,MesProScheduleCalendarServiceImplTest#generateCapacityPlans_shouldCreateMissingWorkingDayCapacityRows,MesProAutoScheduleServiceImplTest#replanPreview_shouldExposeMissingShiftRowsWhenNightShiftCapacityMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test`
  - 结果：PASS，新增续期回归与相关旧回归共 5 条全部通过。
- evidence：
  - `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\backend-api-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\bug-regression-evidence.md`
  - 结果：PASS，两个 validator 均通过。

## Current Blockers

- 无。本任务代码修复、测试与提交已完成。

## Final Verification Result

- `mvn -pl yudao-module-mes -am "-Dtest=MesProScheduleCalendarServiceImplTest#getMonth_shouldRenewFutureCapacityCoverageBeforeFailFastValidation,MesProAutoScheduleServiceImplTest#preview_shouldEnsurePlannedCapacityCoverageBeforeLoadingCapacityPlans,MesProScheduleCalendarServiceImplTest#getMonth_shouldFailFastWhenCapacityMissing,MesProScheduleCalendarServiceImplTest#generateCapacityPlans_shouldCreateMissingWorkingDayCapacityRows,MesProAutoScheduleServiceImplTest#replanPreview_shouldExposeMissingShiftRowsWhenNightShiftCapacityMissing" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\backend-api-evidence.md` -> PASS
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-schedule-calendar-capacity-horizon-renewal\bug-regression-evidence.md` -> PASS
- Git 提交：`b0d401af1eba3a71befc3c8b5d07552a3425ed85` `任务: 修复排程日期产能覆盖自动续期`
- 发布说明：`D:\ProjectPackage\Int\IntRuoyiMaintance\doc\tasks\20260701-schedule-calendar-capacity-horizon-release-test-server\` 首次 `publish-test` 失败已确认根因为独立 required SQL `20260630_mes_pro_work_order_erp_snapshot_fields.sql` 非幂等，与本任务代码逻辑无关。
