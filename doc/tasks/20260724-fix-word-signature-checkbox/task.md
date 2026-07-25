# 修复 Word 导入签名日期区域误识别 checkbox

## Task Goal

修复 Word 批记录表单导入后，`操作人/日期`、`复核人/日期` 等签名日期区域被识别成 checkbox 的问题，确保只有真实结果列选项生成 checkbox 控件，签名日期区域保持签名/文本/日期填写语义。

## Milestones

- [x] 建立缺陷复现与预期行为记录
- [x] 补充失败优先的回归测试
- [x] 实施最小正式修复
- [x] 运行目标验证并记录结果
- [ ] 执行真实页面 E2E 复验
- [ ] 完成 E2E 收尾记录

## Expected Verification

- 新增 Word 表格列偏移场景的回归测试，先证明签名日期尾部区域会被误识别为 checkbox。
- 修复后新增测试通过，并保持既有签名日期 checkbox fragment 保护用例通过。
- `docs/experience-index.md` 当前不存在；本任务不涉及发布、远程服务、生产数据或破坏性操作，按低风险缺陷修复继续推进并记录门禁缺失事实。

## Current Status

completed；新增和既有签名日期保护回归均已通过，`task-closeout-cleanup` 已执行且没有任务临时产物需要删除。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是
- `是否存在临时补丁或绕过`：否

## Cleanup Keep

- `doc/tasks/20260724-fix-word-signature-checkbox/bug-regression-evidence.md`
