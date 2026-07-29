# 20260729 edhr admin main submitted content

## Task Goal

批记录管理员在批次执行详情主区域只查看当前已提交的正式填写内容；其他账号提交后，管理员可在主区域看到提交内容；草稿和待打开任务不得作为主区域内容来源。

## Milestones

- [ ] 记录 BDD / RED / GREEN 证据
- [ ] 定位主区域 execution review 与 preview 切换规则
- [ ] 实现只读主区域已提交内容过滤
- [ ] 增加前端静态回归合同
- [ ] 运行定向验证并记录结果

## Expected Verification

- 静态合同证明主区域优先使用已提交 execution review。
- 静态合同证明 DRAFT / 待打开任务不会通过 task preview 或 draft execution 顶替主区域。
- 定向前端测试通过。

## Current Status

in_progress

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，按已提交 execution 状态过滤主区域展示源。
- 是否存在临时补丁或绕过：否。
