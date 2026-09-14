# 批记录测试判定改为业务方向判定

## Task Goal

将批记录测试里的测试方法从“严格实现正确性判定”调整为“业务方向是否偏离判定”，避免在测试按钮场景下因缺少 Service/Mapper/完整测试证据而直接阻塞方向性检查。

## Milestones

- [x] 定位批记录测试方法和当前严格判定规则。
- [x] 先补充或调整回归测试，证明当前严格判定会误阻塞业务方向检查。
- [x] 修改测试方法，使其围绕业务方向、入口、接口、权限、文案和职责边界判定。
- [x] 执行定向验证并记录结果。

## Expected Verification

- 定向测试命令能够覆盖批记录测试判定逻辑。
- 回归证据证明“方向符合但实现细节证据不足”不再被判定为 BLOCKED。
- 不引入 fallback、降级、吞异常或默认成功。

## Current Status

ready_for_closeout

实现和验证已完成，等待 task-closeout-cleanup preview/apply 后标记 completed。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，目标是调整测试按钮语义对应的判定标准，而非屏蔽失败。
- 是否存在临时补丁或绕过：否。

## Applicable Experience Gates

- 测试管理测试节点闭环门禁：批记录测试固定项和测试方法必须面向业务测试人员，避免技术化、完整实现审计化口径覆盖业务方向检查。
- CODE_READONLY 只读分析门禁：只读 Runner 必须使用正式源码证据，但业务方向类检查不能因缺少完整 Service/Mapper/测试证据直接 BLOCKED。
