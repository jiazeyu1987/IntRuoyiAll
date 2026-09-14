# Request

用户要求“提醒状态列也需要可以排序和过滤搜索”。

## Baseline

注册证当前列表已有服务端分页排序框架，但“提醒状态”之前因显示值由后端页面结果二次计算而未开放排序；当前筛选条件也未提供提醒状态筛选。

## Classification

已接受的范围变更：从纯展示提醒状态扩展为可服务端排序和筛选的列表查询能力。

## Impact

- Product: 当前列表用户可按提醒状态快速定位即将到期、正常或已处理记录。
- Design: “提醒状态”列表头启用排序，筛选区新增提醒状态条件。
- Data: 不改表结构，使用证书正式有效期和提醒规则口径计算查询状态。
- API: 当前列表查询增加 `reminderState` 条件和 `sortField=reminder`。
- Test: 补前端静态契约和后端查询服务测试。
- Release: 需要前后端同时更新，避免前端发送后端旧版本不认识的筛选字段。
- Operations: 无新增运维任务。

## Decision

accept

## Required Approvals

用户当前消息已明确要求该行为变更，无额外审批。

## Downstream

- frontend-feature-delivery
- backend-api-delivery
- task-closeout-cleanup
- project-experience-consolidation

## Blockers

后端定向 JUnit 运行被同模块既有 NAS 测试缺类阻塞，缺失类为 `DccNasOriginalPathSyncReqVO`、`DccNasOriginalPathSyncFileDO`、`DccNasOriginalPathSyncFileMapper`；本次变更代码已通过前端静态契约、前端类型检查和后端主代码编译。

## Next Action

收尾清理本任务临时验证产物，并记录最终验证结果。
