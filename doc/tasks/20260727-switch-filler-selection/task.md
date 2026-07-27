# 20260727 Switch Filler Selection

## Task Goal

删除“切换填写人”弹窗截图红框内的冗余展示内容：标题右侧 `批处理表单 + 表单槽位`，候选人行内的表单来源标签，以及行尾 `可填写` 状态标签；保留填写人姓名、表单名称、候选项选择能力和取消按钮。

## Milestones

- [x] 建立任务证据并记录 BDD/TDD 验证要求
- [ ] 定位“切换填写人”弹窗红框文案来源
- [ ] 增加最小静态回归测试，先复现红框残留再修复
- [ ] 实施最小前端展示修复并运行定向验证
- [ ] 完成验证报告与收尾状态更新

## Expected Verification

- `node tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js` 先 RED 后 GREEN，覆盖截图红框文案删除且表单名称保留。
- `pnpm exec eslint src/views/mes/pro/edhr/ExecutionPage.vue tests/e2e/edhr-switch-filler-redbox-cleanup-static.spec.js --format stylish` 通过。
- 本次仅做前端展示静态契约验证，不启动真实 E2E；不使用 API-only 或 mock 替代真实页面路径。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是在弹窗候选项副标题生成处移除冗余标签。
- `是否存在临时补丁或绕过`：否。

## 经验门禁

- 命中 `docs/e2e-rules.md#eDHR 右侧红框元信息隐藏门禁` 的展示清理口径：删除截图红框时必须确认保留必要卡片信息和操作链路。
- 本任务适用裁剪：删除“切换填写人”弹窗冗余标签，同时通过静态契约确认标题、填写人候选菜单和表单名称仍保留。
