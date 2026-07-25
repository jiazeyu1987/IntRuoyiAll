# eDHR 损耗单打开动作修复

## Task Goal

修复 eDHR 批次执行详情页选择“损耗单”时错误触发跳过逻辑，导致提示“必填路线表单不允许跳过”的问题。预期在关闭前、可填写状态下，点击损耗单应打开填写表单，而不是调用跳过接口。

## Milestones

- [ ] 复现并定位损耗单卡片点击动作分派错误
- [ ] 先补充静态回归测试覆盖必填动态表单不得走跳过接口
- [ ] 实施最小前端修复，保持现有批次详情交互契约
- [ ] 运行目标回归验证并记录证据
- [ ] 完成收尾检查和任务文档更新

## Expected Verification

- `node tests\e2e\<focused-static-test>.spec.js` 先 RED 后 GREEN
- 相关 eDHR 批次详情静态回归测试通过
- `git diff --check` 通过

## Current Status

in_progress

## 经验门禁

- 待读取 `docs/experience-index.md` 后补充本次命中的 eDHR 动态表单、损耗单、静态合同门禁。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，目标是修正点击动作路由，不绕过后端必填校验。
- `是否存在临时补丁或绕过`：否。
