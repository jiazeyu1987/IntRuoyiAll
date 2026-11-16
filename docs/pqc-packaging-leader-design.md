# PQC包装组长设计与验证

更新：2026-11-09。代码交付分支：int_main；验证结果和剩余阻塞见下文。

## 业务范围

- 独立角色 `pqc_packaging_leader_permission`，沿用PQC组长工作台及人员、复核链路。
- 可查看当前租户全部在制活跃订单，包括尚无检验任务的订单；不按原生产组长过滤。
- 使用自己的正式PQC检验人员。仅可选有 `pqc_permission` 的启用用户；其他PQC组长已占用的检验员不可重复关联。
- 所属检验员通过一线PQC页面选择活跃订单，按正式冻结规程填写和签名提交。
- 审批责任仍来自提交时冻结的检验员所属PQC组长。全订单可见不扩大审批责任，不接管其他团队或历史冻结任务。
- 原抽样证据、签名、状态、提交组及租户校验保留；不新增整单生产完工权限。

## 实现与业务链路依据

| 业务节点 | 实现依据 |
| --- | --- |
| 全部订单入口 | `IntRuoyiFronted/src/views/mes/pro/processpool/PqcLeaderWorkbenchPage.vue` 根据显式角色显示标题和内部订单组件；普通PQC组长保留原工作台 |
| 全部订单查询 | `PqcPackagingActiveOrders.vue` → `MesPqcPackagingLeaderController` → `MesPqcPackagingLeaderOrderService` → `MesProcessPoolActiveOrderMapper.selectActiveList()`；查询不依赖检验任务，校验账号启用状态、当前租户、显式角色以及工单租户，不写业务数据 |
| 人员维护 | 现有 `MesPqcLeaderPersonnelServiceImpl`；自己的PQC员工范围、人员资格锁、归属占用及启用状态校验保留 |
| 员工选择订单及提交 | `MesFrontlineDeviceAccountController` → `MesFrontlinePqcContextServiceImpl`；新增包装角色进入既有PQC岗位白名单，员工仍按现有正式身份和规程校验提交 |
| 审批责任生成 | `MesActiveOrderHandoffOwnerResolver.pqcLeader` 按提交人的唯一PQC员工归属确定接收组长，责任快照保留；包装角色进入PQC正式岗位校验 |
| 审批列表及签名 | `MesTeamLeaderWorkbenchServiceImpl` 按本人冻结复核任务筛选；`MesTeamLeaderSubmissionReviewServiceImpl` 对整组逐条调用冻结责任校验，再签名和写入，不按订单全局可见性扩权 |
| 退回与更正 | 更正控制器的PQC角色白名单及巡检更正岗位校验支持新角色；自己的员工范围及冻结责任校验保留 |

## 角色迁移

`IntRuoyiBackend/sql/mysql/20261109_mes_pqc_packaging_leader_role.sql`：为已有PQC工作台的有效租户/套餐补充独立角色、PQC查询/人员维护/复核/更正和不合格评审查询创建权限，并补齐既有 `pqc_permission` 的一线读取及巡检权限。角色、账号、订单和检验员历史归属不自动绑定或转移。共享生产按钮权限不自动授予生产组长页面。

缺少所需菜单权限或角色分类时迁移失败。已关联 `config/menu-migrations.json`。本轮未直接执行共享迁移SQL；测试角色及人员数据全部通过管理员、组长真实页面操作。SQL执行结果不属于本轮已验证事项。

## 已通过验证

- 后端编译及256项集中测试全部通过，0失败、0错误、0跳过，日志 `output/pqc-packaging-tests.log`。包括新订单查询角色/租户边界、PQC人员归属、员工上下文、冻结责任、整组审批、签名及任务生命周期。修正旧测试准备中缺失的授权锁和租户上下文，并将旧巡检规则数据更新为现行规则和真实抽样计算证据；未放宽生产校验。
- 页面ESLint、前端TypeScript检查通过，日志 `output/pqc-packaging-eslint.log`、`output/pqc-packaging-types.log`。
- 菜单迁移关联检查通过（402项累计变更），日志 `output/pqc-packaging-menu-check.log`；这是整个当前脏工作区的累计检查数量，不是本功能变更数量。
- 本机后端构建成功，并通过源码/包指纹核验，日志 `output/pqc-packaging-package.log`。仅替换本线程此前启动的48081后端运行实例；新实例日志 `output/pqc-packaging-runtime.log`。

## 真实页面结果

租户：芋道源码（1）。所有准备和验收动作均由Playwright操作真实页面完成，未调用接口或数据库完成被验收动作。

1. 管理员新增“PQC包装组长”角色，ID `990819697`；从用户列表选用赵丽娜 `zhaolina`（170），保留“审批中心入口”原角色并授予新角色。已通过页面重置所选测试账号并完成首次改密；凭据不写入本文件。
2. 赵丽娜进入工作台显示“PQC包装组长”，新列表读取当前全部10条活跃订单，原生产组长均为用户1；赵丽娜不是这些订单的生产组长。
3. 赵丽娜的检验员列表初始为空。候选列表显示其他组长占用人员不可选；选择未占用的宋青泉 `songqingquan`（690），确认关联成功，自己的列表仅显示宋青泉一人。
4. 宋青泉原角色为“审批中心入口、PQC权限角色”。真实一线页面最初报缺少操作权限，随后通过管理员页面保留原角色并补充既有“eDHR测试PQC提交补充”角色；迁移SQL同时补齐正式PQC角色实际需要的一线读取/巡检权限。该补充测试角色与正式迁移SQL不能视作同一验证结果。
5. 宋青泉已通过页面重置并完成首次改密，进入“一线PQC”，订单选择面板显示同样10条订单。2条可选择，另8条因缺少正式检验规程来源快照明确禁用。已分别选择2条可选订单，均在检验任务读取时失败，未实际提交检验记录。
6. 赵丽娜的“PQC管理”提交列表为空；空列表不等于已完成真实签名审批或越权审批的页面反例。隔离规则通过后端回归验证，实际提交至审批闭环尚未通过。

页面证据：`output/playwright/pqc-workbench.yml`、`pqc-personnel-linked.yml`、`pqc-personnel-candidates.yml`、`pqc-employee-orders.yml`、`pqc-second-order.yml`。截图：`pqc-packaging-leader-orders.png`、`pqc-packaging-employee-orders.png`。

## 剩余阻塞

- 8条旧订单缺少检验规程来源快照；页面提示“请选本次更新后新建的活跃订单”。
- 另外2条订单进入任务读取后报 `PQC task has no frozen matrix sampling evidence`。日志节点为 `MesPqcInspectionItemPlan.requireSampling` → `MesFrontlinePqcContextServiceImpl.toPqcTaskOptionRespVO`，不是包装角色查询权限错误。
- 此前本线程使用“复制测试单”准备新订单时，正式校验已拒绝QA版本125：缺少FIRST/PATROL/FINAL规则。未通过改角色、补造历史证据或跳过校验绕过。
- 完整E2E需要一条正式QA配置完整的新测试订单，包括规程来源快照、适用首检/巡检/末检规则及冻结抽样证据，才能执行员工签名提交、组长签名审批、其他组长不可审批的页面反例。本轮没有修改既有共享发布规程或历史检验记录。

当前结论：功能已实现、集中回归通过、订单可见及自己的人员管理通过真实页面验证；完整提交与审批E2E受现有测试数据阻塞，不能标记全部验收通过。
