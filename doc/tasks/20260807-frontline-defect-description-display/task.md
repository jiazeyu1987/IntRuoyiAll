# 一线生产不良明细显示详情

## 任务目标

- 修复一线生产填报页“不良明细”卡片，使红框位置只显示正式不良详情 `reasonName`，不显示内部编码 `reasonCode`。
- 保留 `reasonId`、`reasonCode` 作为提交载荷中的正式身份字段，不改变不良数量填写、汇总和提交行为。

## 里程碑

- [x] M1：根据截图定位一线生产不良明细页面与正式字段来源。
- [x] M2：补充 BDD 场景并运行失败回归测试（RED）。
- [x] M3：实施最小修复并通过目标测试（GREEN）。
- [x] M4：完成相关回归与验证报告；任务进入清理门禁。

## 预期验证

- 聚焦静态回归测试证明不良明细卡片的可见标签唯一来自 `reasonName`，禁止回退显示 `reasonCode` 或原因编号占位文案。
- 提交载荷测试证明 `reasonId`、`reasonCode` 和 `reasonName` 仍按正式结构提交。
- 相邻一线生产模板静态测试通过。
- `pnpm ts:check` 通过。
- `git diff --check` 对本任务文件通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；本任务移除可见标签对编码和编号占位文案的 fallback。
- 是否从根因和长期维护角度解决：是；明确区分用户可见详情 `reasonName` 与内部身份 `reasonCode`，并由回归合同锁定。
- 是否存在临时补丁或绕过：否。

## 经验门禁

- 已读取 `docs/experience-index.md`；本任务适用 `docs/frontend-development.md` 的“前端静态契约隔离门禁”，使用一线生产聚焦合同锁定标签字段，不扩大到不良原因维护、接口建模或其它工作台。
- 截图区域由 `FrontlineFixedTemplatePanel.vue` 的 `configuredDefectReasons` 驱动；正式运行态契约已分别提供 `reasonName` 与 `reasonCode`。
- 收尾经验已归并到 `docs/frontend-development.md#用户可见描述与内部编码隔离门禁`，并在 `docs/experience-index.md` 增加可检索关键词。

## Current Status

completed：实现与全部计划验证通过；cleanup preview/apply 无阻塞完成，任务临时缺陷证据已清理，核心记录与正式回归合同保留。
