# Acceptance Criteria: IntRuoyi 自动排产

## Purpose and Scope

本文定义 IntRuoyi 自动排产第一版本的可验收条件和拒收条件。验收必须使用真实业务数据路径，不得以 mock 成功、静默跳过或降级替代真实验证。

## Evidence Reviewed

- `docs/product/prd.md`
- `docs/product/user-flows.md`
- `docs/changes/20260512-intpp-auto-schedule-migration.md`
- `doc/tasks/20260512-intpp-auto-schedule-migration-assessment/task.md`

## Acceptance Criteria

### AC1 自动排产入口

Given 用户拥有自动排产权限，且存在可排产工单
When 用户在生产排产页面选择排产范围并发起自动排产
Then 系统应生成当前排产结果或返回明确失败原因。

### AC2 单版本当前排产

Given 自动排产应用成功
When 用户查看生产任务和 Gantt
Then 系统只展示当前有效排产结果，不要求用户选择排产版本、草稿或快照。

### AC3 当前系统数据复用

Given 工单、路线、BOM、库存、工作站和日历存在于 IntRuoyi
When 系统执行自动排产
Then 系统应使用 IntRuoyi 数据作为输入和结果来源，不要求创建 IntPP 同名业务表。

### AC4 缺少路线失败

Given 排产范围内存在缺少可用工艺路线的工单
When 用户发起自动排产
Then 系统失败，并提示缺少路线的工单。

### AC5 产能约束

Given 排产范围内任务需要占用同一资源同一班次
When 可用产能不足
Then 系统不得静默超产能排产，应失败或按明确风险规则提示。

### AC6 实际产能模式

Given 用户选择实际产能模式
When 排产日期缺少必需实际产能数据
Then 系统必须失败，并提示缺少实际产能。

### AC7 物料齐套

Given 工单 BOM 和库存数据存在
When 物料不足
Then 系统应展示缺料物料、缺口数量、影响工单和影响工序。

### AC8 工序依赖

Given 工艺路线包含多个工序
When 自动排产应用成功
Then 后置工序不得早于前置工序完成，Gantt 应展示依赖关系。

### AC9 重排保护

Given 重排范围内存在已完工任务
When 用户执行重排
Then 已完工任务不得被覆盖、删除或移动。

### AC10 手工/锁定任务保护

Given 任务被标记为锁定或手工保留
When 用户执行重排
Then 系统应按策略保留该任务，或在覆盖前明确提示并要求确认。

### AC11 数量回写

Given 自动排产应用、修改、删除或重排成功
When 用户查看工单
Then 工单 `quantityScheduled` 应等于当前有效生产任务数量汇总。

### AC12 超工单数量拦截

Given 自动排产结果会导致工单已排产数量超过工单数量
When 系统准备应用排产
Then 系统必须失败，并提示对应工单和数量差异。

### AC13 权限控制

Given 用户没有自动排产或重排权限
When 用户尝试执行对应操作
Then 系统拒绝操作，且当前生产任务不变化。

### AC14 可测试覆盖

Given 自动排产功能进入验收
When 执行后端单测、API 测试和 Playwright E2E
Then 主流程、缺路线、缺产能、缺料、重排保护和数量回写路径均有真实数据验证证据。

## Rejection Criteria

- 系统需要创建或依赖 IntPP 排产版本、草稿或快照才能完成第一版本排产。
- 自动排产结果不写回 IntRuoyi 当前生产任务。
- 缺少必要路线、BOM、产能、日历或库存时仍返回成功。
- 重排覆盖已完工任务。
- 已排产数量可能超过工单数量。
- 前端只能通过隐藏接口验证，没有真实用户入口。
- E2E 使用 mock 数据或为测试额外增加临时控件。
- 权限不足时仍可执行自动排产或重排。

## Product Blockers

- 产能维度和数据来源未确定。
- 物料可用量口径未确定。
- 缺料阻塞或风险排产规则未确定。
- 手工任务、锁定任务和已开始任务保护规则未确定。
- 自动排产应用前是否必须人工确认未确定。
