# 一线生产无设备工序提交限制修复

## Task Goal

修复一线生产点击提交时对无设备工序错误提示“当前工序缺少正式设备配置，无法提交”的问题：没有正式设备配置的工序不应因此被阻止提交；已配置设备的工序仍按正式设备配置校验设备参数。

## Milestones

1. 定位一线生产提交前设备校验、正式设备来源和现有测试。
2. 先补充可复现的 BDD/TDD 回归测试并确认 RED。
3. 实施最小修复，使无设备与有设备两条行为边界分别成立。
4. 运行定向测试、前端类型/静态回归验证并记录结果。
5. 按任务收尾规则完成清理和最终状态记录。

## Expected Verification

- `BDD` 覆盖无正式设备配置工序提交成功，以及有正式设备配置工序仍执行设备参数校验。
- 回归测试先 RED 后 GREEN。
- 受影响前端定向静态合同或测试通过。
- `pnpm ts:check` 或记录其明确阻塞原因。
- `git diff --check` 通过。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否；仅区分“无设备配置”与“已有设备配置”，不返回默认成功或吞异常。
- 是否从根因和长期维护角度解决：是；提交前校验按正式设备配置是否存在决定是否进入设备参数校验。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 一线生产正式提交必须保持单次正式提交接口、正式设备参数校验和提交事件链路，不能以空设备成功替代已配置设备的校验。
- 前端提交前严格验证只能发生在显式提交链路，不能改变草稿态计算或吞掉提交错误。

## Current Status

completed

实现、验证、经验归档和 task-closeout-cleanup 均已完成；无剩余 blocker。

## Cleanup Keep

- doc/tasks/20260807-frontline-submit-optional-equipment/task.md
- doc/tasks/20260807-frontline-submit-optional-equipment/execution-log.md
- doc/tasks/20260807-frontline-submit-optional-equipment/verification-report.md
