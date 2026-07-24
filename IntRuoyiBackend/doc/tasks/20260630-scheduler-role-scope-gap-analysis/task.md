# 任务：排产员正式角色范围 SQL 漏项分析（后端/SQL）

- Task ID: `20260630-scheduler-role-scope-gap-analysis`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-30`
- Current Status: `in_progress`

## Task Goal

只读审计 `20260629_mes_smart_scheduling_role_scope.sql` 中 `scheduler` 白名单，判断排产员正式角色范围是否仍缺少菜单/权限码，并明确每个差异对应的后端接口和业务影响。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260630-test-server-zhaojie-replan-preview-permission-fix\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成 `900180/900181/900182` 漏项修复；本次进入新的只读审计任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - SQL、任务文档与审计输出统一显式 UTF-8。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务为只读分析。
- `是否从根因和长期维护角度解决`：是。直接核对 SQL 白名单、控制器鉴权和历史正式修复记录。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 排产员白名单与接口鉴权一一对应 -> Given 智能排产相关控制器声明了具体权限码 / When 对照 scheduler 白名单 / Then 可以识别还未覆盖的权限项。`
- `BDD: 非排产员职责的权限不误判为漏项 -> Given 某些高风险权限本就不应交给排产员 / When 对照历史角色收敛目标 / Then 这些项应被标记为设计上故意排除。`

## Milestones

1. M1：建立后端分析任务文档。`completed`
2. M2：读取 SQL、菜单与控制器鉴权。`completed`
3. M3：形成漏项/非漏项清单。`completed`
4. M4：回填最终结论。`completed`

## Expected Verification

- `Get-Content -Encoding utf8` / `rg -n` 交叉读取上述 SQL、测试和控制器文件。

## Final Verification Result

- `scheduler` 正式白名单当前为：
  - `5100,5101,5160,5161,5170,5171,5300,5310,5311,5320,5321,5700,5720,5721,5530,5531,900120,5590,900170,900180,900181,900182,5580,5581,5582,5584,5585,5587,5550,5262,900121,5540,5985`
- 已确认此前真实故障对应权限已全部进入白名单，不再属于漏项：
  - `900170`
  - `900180/900181/900182`
  - `5581/5582/5584/5585/5587`
- 当前最明确漏项：
  - `5541 = mes:pro-task:query`
  - 原因：白名单保留了 `5540 = 生产排产` 页面，但未保留其查询子权限；而前端 `task/index.vue` 会调用 `/mes/pro/task/gantt-list`，后端 `MesProTaskController` 对 `page/get/gantt-list` 都要求 `mes:pro-task:query`。
- 高概率漏项：
  - `5583 = mes:pro-schedule-order:update`
  - 原因：`scheduleorder/index.vue` 的冻结、解冻、调整、同步进度按钮及 `MesProAutoScheduleController` 的若干 issue 接口都依赖 `mes:pro-schedule-order:update`；但历史正式修复尚未把它像 `5581/5582/5584/5585/5587` 那样明确纳入排产员职责闭环。
- 当前更像故意排除、而非漏项：
  - `900171 = mes:pro-scheduler-workbench:smoke-test`
  - `5586 = mes:pro-schedule-order:delete`
  - `mes:pro-schedule-order:revoke-complete`
  - `5532/5533/5535 = mes:pro-work-order:create/update/export`
  - `5722/5723/5724/5725 = mes:pro-route:create/update/delete/export`
  - `900122 = mes:pro-schedule-route:update`
  - 这些项要么已有历史任务明确不下放给排产员，要么当前更符合“配置维护/管理员”职责，而不是排产员基础操作权限。

## Current Blockers

- 无。

## Current Status

- `completed`
