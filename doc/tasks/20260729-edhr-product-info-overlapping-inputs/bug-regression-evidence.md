# Bug Regression Evidence

## Bug Summary

选择产品信息工序后，部分辅助网格单元格出现两个输入控件叠加。

## Expected Behavior

每个辅助网格位置只允许一个正式字段卡片和一个匹配字段类型的输入控件；预览快照必须按当前填写人隔离个人责任主体网格，重复字段引用应去重，不同字段位置冲突应 fail fast。

## Reproduction

- 用户截图：产品信息辅助网格中“三通旋塞”等单元格出现 textarea 与普通输入框叠加。
- 真实只读页面：产品信息辅助模式共 `125` 条辅助行，检出 `52` 个重复 CSS Grid 位置。
- 代表位置 `2/1` 同时渲染 `ASSIST_GRID_U795_R1_C1` 与 `ASSIST_GRID_U810_R1_C1`，两个元素边界矩形完全一致。

## Root Cause

产品信息未开始任务通过 `batchTaskPreview=1` 加载完整运行快照。快照同时包含 `U795` 和 `U810` 两套独立填写人辅助网格，`ExecutionPage.vue` 解析 rowKey 后只保留行列坐标，未保留或筛选责任主体，最终把两套相同行列坐标铺入同一个 CSS Grid。单个字段模板本身使用互斥分支，不是输入组件重复创建。

## Regression Test

`tests/e2e/edhr-assist-grid-current-filler-isolation-static.spec.js` 覆盖：

- 正式 `USERS/ROLE` 与旧版 `U` rowKey 解析。
- 未过滤快照按当前填写人隔离。
- 角色责任范围缺失时 fail fast。
- 同一坐标不同字段冲突阻断。
- 同一坐标同一字段重复引用去重。
- 禁止使用 `z-index` 遮盖重叠。

## RED

- RED: `node tests/e2e/edhr-assist-grid-current-filler-isolation-static.spec.js` -> FAIL，首个失败为缺少 `AssistGridSubjectType`。

## GREEN

- GREEN: `node tests/e2e/edhr-assist-grid-current-filler-isolation-static.spec.js` -> PASS。
- GREEN: 配置网格、辅助填写模式、批次详情网格一致性三个相邻静态合同通过。

## Verification

- 待执行。

## Risk And Regression Scope

- 风险集中在辅助网格字段构造和位置冲突检测。
- 不修改后端批次任务来源、批记录表单绑定、表单槽位、填写权限和保存接口。

## Blockers And Follow-up

- 当前无阻塞。
