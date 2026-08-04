# Execution Log

## User Intent

- 用户要求按已确认方案继续设计、开发、验证：工艺路线可以选择产品，产品也可以选择工艺路线。
- 设计决策：产品侧入口放在 MES 物料产品维护，而不是 MDM 产品主数据；复用 `mes_pro_route_product.route_id + item_id` 关系。

## Baseline And Workspace State

- 已有脏工作区基线提交：`2e507d526 chore: baseline dirty workspace before route product binding`。
- 基线后仍存在无关残余修改：`IntRuoyiFronted/src/views/mes/pro/scheduleorder/index.vue`。本任务不触碰、不回滚、不暂存该文件。
- 当前分支状态：`int_main...origin/int_main [ahead 11]`。

## BDD Scenarios

- BDD: 产品侧查看当前工艺路线 -> Given MES 物料产品已通过 `mes_pro_route_product` 绑定一条工艺路线 / When 用户打开该物料产品编辑表单 / Then 表单展示当前工艺路线，数据来源是 route-product 绑定关系。
- BDD: 产品侧选择工艺路线 -> Given MES 物料产品未绑定工艺路线且存在启用工艺路线 / When 用户在物料产品表单选择工艺路线并保存 / Then 系统创建 `mes_pro_route_product` 绑定，路线侧关联产品列表同步可见该产品。
- BDD: 产品侧更换工艺路线 -> Given MES 物料产品已绑定工艺路线 A / When 用户改选工艺路线 B 并保存 / Then 系统更新同一产品的 route-product 绑定为路线 B，不新增第二条关系。
- BDD: 产品侧解除工艺路线 -> Given MES 物料产品已绑定工艺路线 / When 用户清空工艺路线并保存 / Then 系统删除该产品的 route-product 绑定，产品侧和路线侧均不再显示关联。
- BDD: 单产品唯一路线约束 -> Given MES 物料产品已绑定路线 A / When 另一请求尝试再绑定路线 B / Then 后端 fail fast 返回现有唯一性错误，不静默覆盖、不创建重复绑定。

## RED/GREEN Evidence

- RED: pending
- GREEN: pending

## Command And Verification Notes

- 2026-08-04：读取 `backend-api-delivery`、`frontend-feature-delivery`、`behavior-driven-development` 技能和前后端契约说明。
- 2026-08-04：读取任务收尾、后端开发、前端开发、PowerShell 编码、PowerShell/Git 经验和项目经验索引；适用门禁已摘入 `task.md`。

## Blockers

- 当前无实现阻塞。

