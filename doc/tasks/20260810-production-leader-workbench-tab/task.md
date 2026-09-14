# 任务：生产组长页签增加生产组长工作台

## Task Goal

将现有生产组长工作台作为一个独立 tab 放到生产组长页签下，让具备生产组长身份的用户都能看到并进入该工作台。

## Milestones

- [x] 定位生产组长页签与现有生产组长工作台组件、路由和可见性条件。
- [x] 编写最小静态合同，覆盖新增 tab、生产组长可见性和 PQC 相邻隔离。
- [x] 实现 tab 接入，保持现有工作台逻辑和接口不变。
- [x] 运行定向验证并记录结果。

## Expected Verification

- 定向静态合同先 RED 后 GREEN。
- 受影响前端代码通过定向静态验证。
- 如现有项目条件允许，运行相关前端类型或静态回归检查。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，复用现有生产组长工作台组件并接入正式页签。
- 是否存在临时补丁或绕过：否。

## Experience Gate

- 已读取 `docs/experience-index.md`，匹配 `docs/frontend-development.md#前端角色内容页签拆分口径门禁`。
- 本次需求按“生产组长页面内部功能模块 Tab”处理：同步所有重复生产组长 module tab 条，新增独立 `workbench` key、独立 gate、正式列表加载 watcher，并复跑 PQC 相邻合同。

## Cleanup Candidates

- doc/tasks/20260810-production-leader-workbench-tab/frontend-feature-evidence.md
