# 20260729 eDHR 产品信息输入框叠加修复

## Task Goal

定位并修复 eDHR 填写辅助模式选择“产品信息”工序后，部分网格单元格出现两个输入控件叠加的问题。每个辅助网格位置必须只渲染一个正式字段卡片和一个输入控件，不得用 CSS 遮挡重复字段。

## Milestones

- [x] 根据用户截图定位受影响页面和渲染链路。
- [ ] 使用真实只读页面确认重复 DOM、字段身份和网格坐标。
- [ ] 建立聚焦回归测试并记录 RED。
- [ ] 实施最小正式修复。
- [ ] 运行目标测试、相邻回归、类型检查和真实页面 E2E。
- [ ] 完成经验沉淀、清理、提交和推送。

## Expected Verification

- 产品信息辅助网格中，每个 `ASSIST_GRID` 位置最多对应一个渲染字段。
- 页面每个 `.edhr-fill-workspace__assist-row` 内只出现其字段类型对应的一个输入控件。
- 同一字段被多条 `assistRows` 引用时按正式字段身份去重，不改变原始 `fieldIdentity`、网格位置、草稿值或保存键。
- 若同一网格位置错误配置了不同字段，必须明确暴露配置冲突，不得静默选第一条、遮挡或 fallback。
- 聚焦静态合同、相邻辅助网格回归、`pnpm ts:check` 和真实只读 Playwright 通过。

## Applicable Gates

- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：辅助网格必须按正式 `assistRows` 和原始字段坐标恢复，不推断或改写 rowKey。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用本缺陷专用最小合同完成 RED/GREEN。
- `docs/e2e-rules.md#真实 E2E 页面加载判据门禁`：先等待任务预览接口，再断言目标辅助网格 DOM 和无写请求。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；先确认重复来源，再在字段构造边界保证一格一字段。
- `是否存在临时补丁或绕过`：否。

## Current Status

in_progress
