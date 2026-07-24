# 任务：发布展柜产品矩形布局

## 任务目标

手动发布展厅时，后端发布包中的每个展柜产品必须携带编辑画布保存的矩形布局字段：`layoutX`、`layoutY`、`layoutWidth`、`layoutHeight`。发布阶段发现布局缺失或非法时必须明确失败，不得生成默认平均网格或静默降级。

## Previous Task Check

- 上一后端任务：`doc/tasks/20260606-showroom-hall-canvas-layout/task.md`。
- 检查结果：状态为 `completed`。

## BDD 场景

- BDD: 手动发布携带展柜矩形布局 -> Given 展柜产品已保存合法矩形布局 / When 用户执行手动发布展厅 / Then 发布输出中的对应产品包含 `layoutX/layoutY/layoutWidth/layoutHeight`。
- BDD: 发布阶段拒绝非法布局 -> Given 展柜产品布局字段缺失、非数字或宽高无效 / When 后端组装 Website 发布包 / Then 发布失败并指出非法布局，不得输出默认布局。

## 里程碑

- [x] M1：确认上一任务已完成并创建本任务文档。
- [x] M2：补 RED 测试覆盖发布布局字段与非法布局失败。
- [x] M3：实现发布契约最小变更。
- [x] M4：运行目标测试和回归验证。
- [x] M5：更新证据并提交后端改动。

## 预期验证

- `ShowroomReleaseWebsiteIndexAssemblyTest` 目标测试。
- `ShowroomReleasePublisherServiceTest` 相关回归测试。
- 后端发布契约证据校验。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。布局缺失或非法时必须失败。
- `是否从根因和长期维护角度解决`：是。发布契约直接携带编辑源布局，Website 不自行推导替代布局。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## 完成记录

- 已新增发布包布局字段断言，覆盖非均分矩形输出。
- 已新增缺失布局 fail-fast 测试，污染 `showroom_hall_product.layout_width = NULL` 后发布必须失败。
- 已让发布读取原始映射，不继承列表读取层的默认网格补齐。
- 已将布局纳入 website-index、legacy projection、hall snapshot hash 与 mapping hash。

## Cleanup Keep

- `doc/tasks/20260606-showroom-publish-hall-layout/backend-api-evidence.md`
- `doc/tasks/20260606-showroom-publish-hall-layout/verification-report.md`
