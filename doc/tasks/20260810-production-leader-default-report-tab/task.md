# 生产组长默认进入报工管理

## Task Goal

点击侧边栏“生产组长”进入生产组长页面时，默认选中并展示“报工管理”，而不是其它页签。

## Milestones

- [x] 创建任务记录并登记 BDD 场景。
- [ ] 定位生产组长页面默认页签逻辑和现有测试模式。
- [ ] 先补充失败的静态契约测试覆盖默认页签。
- [ ] 实现最小前端修复并通过目标验证。
- [ ] 记录验证结果并完成收尾状态。

## Expected Verification

- 目标静态契约先 RED 后 GREEN。
- 受影响前端测试命令通过。
- `git diff --check` 通过。

## Current Status

in_progress

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，直接修正生产组长页面默认页签状态。
- `是否存在临时补丁或绕过`：否。

## Experience Gate

- 待读取 `docs/experience-index.md` 后补充。
