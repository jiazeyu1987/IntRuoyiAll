# 删除排产工艺路线导入导出按钮

## Task Goal

删除排产设置弹窗黄色框中的“导出排产工艺路线”和“导入排产工艺路线”两个按钮，保留其它排产设置保存能力不变。

## Milestones

- [x] 建立任务记录、BDD 场景和基线提交证据。
- [x] 定位排产设置页面组件和现有测试入口。
- [x] 先补充/调整静态契约形成 RED。
- [x] 删除目标按钮并通过 GREEN 验证。
- [ ] 完成回归验证、收尾清理和提交推送。

## Expected Verification

- 静态契约证明排产设置中不再渲染“导出排产工艺路线”和“导入排产工艺路线”按钮。
- 静态契约证明“导出全部数据包”“导入全部数据包”“保存策略”等非目标按钮仍保留。
- 相关前端类型或相邻静态测试按影响范围执行；若存在历史阻塞，记录准确影响。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接删除目标 UI 入口及其无用绑定，不用 CSS 遮挡或权限绕过。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 适用 `IntRuoyiBackend/docs/system/mes-scheduling-domain-contracts.md#手动重排数据包门禁`：本任务只删除“排产工艺路线”入口，不得影响“导出全部数据包/导入全部数据包”及其完整数据包口径。
- 适用 `docs/database-rules.md#工艺路线跨租户导入导出数据包完整性门禁`：不改后端工艺路线导入导出接口、SQL 或跨租户数据链路，仅移除当前排产设置弹框 UI 入口。

## Cleanup Keep

- doc/tasks/20260730-schedule-route-buttons-remove/frontend-feature-evidence.md
