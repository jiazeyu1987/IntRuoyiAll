# 任务：MES 排产物料校验统一为工单物料需求口径

## Goal

将 `自动排产 preview/apply` 与 `排程日历月视图/日详情` 的物料短缺口径统一到工单详情页 `物料需求` 同一套规则：

- 不再直接使用工单 BOM 原始行做库存校验
- 不引入任何工序级 BOM 校验
- 物料消耗来源统一为工单物料需求（叶子物料展开后合并）
- 若工单缺少可用物料需求，仅返回 warning，不阻塞排产发布

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\workorder\MesProWorkOrderBomService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\workorder\MesProWorkOrderBomServiceImpl.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\controller\admin\pro\workorder\MesProWorkOrderBomController.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProAutoScheduleServiceImpl.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProScheduleCalendarServiceImpl.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\main\java\cn\iocoder\yudao\module\mes\enums\ErrorCodeConstants.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProAutoScheduleServiceImplTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-mes\src\test\java\cn\iocoder\yudao\module\mes\service\pro\schedule\MesProScheduleCalendarServiceImplTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-mes-auto-schedule-material-demand-unification\**`

## Non-Scope

- 不修改前端页面或前端问题弹框结构。
- 不修改工单 BOM 生成入口与 ERP 同步入口。
- 不改变自动排产请求/响应字段结构。
- 不把“缺少物料需求”警告扩展到日历接口展示。
- 不引入 fallback、mock 成功或静默降级。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-product-narration-restart-and-monitor\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已完成，不阻塞本次继续在 `yudao-module-mes` 侧修改排产物料校验逻辑。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务只提交 `yudao-module-mes` 相关后端代码和本任务文档，不夹带其他在途改动。

## Milestones

1. 建立任务文档并锁定物料需求口径与 warning 策略。
2. 先写 RED，覆盖自动排产、排程日历与工单物料需求服务化场景。
3. 实现服务层工单物料需求展开能力，并让控制器复用。
4. 实现自动排产与排程日历统一按工单物料需求校验库存。
5. 跑 GREEN 验证，回写证据并执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-mes "-Dtest=MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-mes-auto-schedule-material-demand-unification\backend-api-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-mes-auto-schedule-material-demand-unification --mode preview`

## Current Status

Completed.

## Completed Work

- 在 `MesProWorkOrderBomService` 增加了工单物料需求展开查询能力：
  - `getWorkOrderMaterialDemandByWorkOrderId`
  - `getWorkOrderMaterialDemandMapByWorkOrderIds`
- 将原本留在 `MesProWorkOrderBomController` 里的叶子物料递归展开逻辑迁入 `MesProWorkOrderBomServiceImpl`，控制器改为直接复用服务结果。
- 将 `MesProAutoScheduleServiceImpl` 的物料校验口径从“工单 BOM 原始行”切换为“工单物料需求（叶子物料展开后合并）”。
- 删除了自动排产里“工单未生成BOM”的 blocking 分支；当工单没有可展开的物料需求时，改为返回 `MATERIAL_DEMAND` warning，文案为 `工单缺少物料需求`。
- 保持真实缺料仍为 `MATERIAL` warning，`shortageCount` 只统计真实缺料，不统计 `MATERIAL_DEMAND`。
- 将 `MesProScheduleCalendarServiceImpl` 的月视图与日详情短缺统计同样切到工单物料需求口径，继续按工单首道开工日归集。
- 新增后端测试：
  - `MesProWorkOrderBomServiceImplTest`
  - `MesProWorkOrderBomControllerTest`
- 扩展既有测试：
  - `MesProAutoScheduleServiceImplTest`
  - `MesProScheduleCalendarServiceImplTest`

## Verification Result

- PASS: `mvn -pl yudao-module-mes "-Dtest=MesProWorkOrderBomServiceImplTest,MesProWorkOrderBomControllerTest,MesProAutoScheduleServiceImplTest,MesProScheduleCalendarServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-mes-auto-schedule-material-demand-unification\backend-api-evidence.md`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-mes-auto-schedule-material-demand-unification --mode preview`

## Remaining Blockers

- 当前无已知 blocker；仅仓库中存在与本任务无关的并行改动，需要提交时严格按文件范围暂存。
