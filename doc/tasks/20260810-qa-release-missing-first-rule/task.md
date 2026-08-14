# 20260810 QA 发布缺少 FIRST 规则修复

## Task Goal

修复 QA 规程发布时误报“缺少必要检验规则：FIRST”的问题，确保页面已配置首检项目时发布校验能够按正式页面选择和保存载荷识别 FIRST 规则。

## Milestones

- [ ] 定位 FIRST 必要规则校验来源和页面/保存载荷契约。
- [ ] 补充可复现误报的 BDD/回归测试并记录 RED。
- [ ] 实施最小正式修复，不引入 fallback、默认成功或吞异常。
- [ ] 运行目标测试和相关回归验证。
- [ ] 写入验证报告、bug 证据并完成任务状态更新。

## Expected Verification

- 目标回归测试先 RED 后 GREEN，覆盖首检 FIRST 已配置但发布误报缺失的场景。
- QA 规程发布相邻静态测试通过。
- `pnpm ts:check` 和 `git diff --check` 通过或记录明确阻塞。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是校正 FIRST 必要规则的正式来源与发布载荷契约。
- 是否存在临时补丁或绕过：否。
