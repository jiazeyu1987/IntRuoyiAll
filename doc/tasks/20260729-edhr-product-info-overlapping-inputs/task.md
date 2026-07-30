# 20260729 eDHR 产品信息输入框叠加修复

## Task Goal

定位并修复 eDHR 填写辅助模式选择“产品信息”工序后，部分网格单元格出现两个输入控件叠加的问题。每个辅助网格位置必须只渲染一个正式字段卡片和一个输入控件，不得用 CSS 遮挡重复字段。

## Milestones

- [x] 根据用户截图定位受影响页面和渲染链路。
- [x] 使用真实只读页面确认重复 DOM、字段身份和网格坐标。
- [x] 建立聚焦回归测试并记录 RED。
- [x] 实施最小正式修复。
- [x] 运行目标测试、相邻回归、类型检查和真实页面 E2E。
- [x] 完成经验沉淀、清理、提交和推送。

## Expected Verification

- 产品信息辅助网格中，每个 `ASSIST_GRID` 位置最多对应一个渲染字段。
- 页面每个 `.edhr-fill-workspace__assist-row` 内只出现其字段类型对应的一个输入控件。
- 未经过后端范围过滤的预览快照必须按当前填写人隔离个人辅助网格，不得把多个填写人的坐标空间合并。
- 同一字段被同一辅助格重复引用时按正式字段身份去重，不改变原始 `fieldIdentity`、网格位置、草稿值或保存键。
- 若同一网格位置错误配置了不同字段，必须明确暴露配置冲突，不得静默选第一条、遮挡或 fallback。
- 聚焦静态合同、相邻辅助网格回归、`pnpm ts:check` 和真实只读 Playwright 通过。

## Applicable Gates

- `docs/frontend-development.md#eDHR 辅助模式当前工序 assistRows 路由门禁`：辅助网格必须按正式 `assistRows` 和原始字段坐标恢复，不推断或改写 rowKey。
- `docs/frontend-development.md#前端静态契约隔离门禁`：使用本缺陷专用最小合同完成 RED/GREEN。
- `docs/e2e-rules.md#真实 E2E 页面加载判据门禁`：先等待任务预览接口，再断言目标辅助网格 DOM 和无写请求。

## 经验门禁

- `assistRows` 的 rowKey 同时承担责任主体和网格坐标语义；未过滤快照必须按当前填写人隔离，不能把不同责任主体的坐标空间直接合并。
- 角色责任主体必须有正式当前填写人范围才能进入字段构造；缺少范围或同一坐标映射不同字段时必须 fail fast。
- 真实 E2E 只读验证必须断言责任主体集合、网格坐标唯一、每格控件数量和无 MES 写请求。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；在 `assistRows` 进入字段构造前恢复责任主体语义，隔离当前填写人的坐标空间并校验位置冲突。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

## Verification Result

- 聚焦静态合同、相邻辅助网格合同和 `pnpm ts:check` 通过。
- 真实产品信息只读页面以填写人 `795`、`810` 分别验证，重复网格位置均为 `0`，每行最多一个可见原生控件，MES 写请求和页面错误均为 `0`。
- 经验已合并到 `docs/frontend-development.md` 既有 eDHR assistRows 门禁，并同步 `docs/experience-index.md`。
- 清理预览与应用通过，仅删除本任务中间证据和临时 Playwright 产物，核心任务记录保留。
