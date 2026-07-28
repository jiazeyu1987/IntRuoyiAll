# 辅助表单映射模式

## Task Goal

在现有“填写配置”弹窗中增加“辅助表单映射”模式：左侧保留原表单，中央实时展示辅助表单预览，右侧集中放置辅助行映射控制栏。用户在控制栏设置辅助行、字段类型、下拉选项、签名/日期/数字和填写人时，辅助表单预览实时更新。

## Milestones

1. `pending`：记录 BDD 场景和 RED 静态合同。
2. `pending`：实现模式切换与三栏辅助映射布局。
3. `pending`：迁移辅助行映射控件到蓝色控制栏，并保持原有保存合同。
4. `pending`：补充辅助表单实时预览和当前单元格可视反馈。
5. `pending`：运行静态合同、类型检查和必要回归。

## Expected Verification

- 新增或更新静态合同，先 RED 后 GREEN，覆盖“辅助表单映射”模式入口、三栏布局、控制栏控件归属、实时辅助表单预览。
- 运行 `node tests\e2e\edhr-visual-fill-config-static.spec.js`。
- 运行相关相邻静态合同，确保现有填写配置保存链路未退化。
- 运行 `pnpm ts:check`。

## Experience Gate Summary

- 待读取 `docs\experience-index.md` 后补充命中门禁。

## Current Status

in_progress：已按项目规则创建任务目录，准备执行 BDD + strict TDD。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否；仅调整前端交互组织和预览，不改变后端合同。
- `是否从根因和长期维护角度解决`：是；把辅助映射从原表单规则侧栏中抽成明确模式，复用现有 `assistRows`、`cellRules`、`fillAssignments`。
- `是否存在临时补丁或绕过`：否。
