# Bug Regression Evidence

## Bug Summary

点击“一线生产”页签时，正式设备账号工序接口因路线 `922119` 的工序 `922985` 缺少正式工作站绑定而返回 `工艺路线工序缺少正式工作站绑定`。

## Expected Behavior

目标路线工序存在可追溯、存在且启用的正式工作站绑定时，一线生产工序接口返回业务码 `0` 和该工序候选；正式来源缺失时继续 fail-fast，不做默认值、过滤或替代数据源降级。

## Reproduction

pending - 待通过正式接口或真实页面记录修复前业务码与响应。

## Root Cause

pending - 已确认直接触发条件为 `mes_pro_route_process.workstation_id` 对应运行态值为空，待完成正式来源核对。

## Regression Test

pending。

## RED

pending。

## GREEN

pending。

## Risk And Regression Scope

- 风险范围：路线工序工作站绑定、一线设备账号可切换工序、工作站/设备候选映射。
- 不得影响：工序开始配置、批记录表单绑定、表单槽位 `formBindings`、生产组长工序配置授权。

## Blockers And Follow-up

- 当前必须先确认目标工序唯一正式工作站来源；来源不唯一或缺失时应阻塞并请用户补充正式配置决策。
