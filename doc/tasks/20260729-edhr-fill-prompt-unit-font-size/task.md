# eDHR 填写页提示词与单位字号调整

## Task Goal

- 将截图所示填写页输入框内提示词与右侧单位字号增大一倍。
- 只调整目标输入提示词和单位显示样式，不改变表单数据、接口、保存或校验链路。

## Milestones

- [ ] 定位填写页提示词与单位渲染组件及样式来源。
- [ ] 先补充聚焦静态合同，证明提示词与单位字号需要增大一倍。
- [ ] 实现最小样式调整。
- [ ] 运行聚焦验证并记录结果。
- [ ] 收尾清理并更新任务状态。

## Expected Verification

- 聚焦静态合同先 RED 后 GREEN。
- 相关前端静态检查或最小样式合同通过。
- 不引入 fallback、降级、吞异常、接口或数据链路变更。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是复用现有组件样式变量或局部样式规则完成正式展示调整。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- `docs/frontend-development.md#element-plus-选择框显示门禁`：涉及 Element Plus 输入/选择控件内文本展示时，需保证文本完整可见、布局不重叠。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：若可读取，按项目前端页面/表格/样式经验执行；若缺失则记录为非阻塞样式参考缺失。
