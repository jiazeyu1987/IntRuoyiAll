# 20260810-report-assignment-clear-button

## Task Goal

在“分配报工”弹窗的每一行增加清除按钮，点击后将该行分配数量清零，并保留现有分配、汇总和校验链路。

## Milestones

- [x] 建立 BDD 场景与前端任务证据
- [x] 定位弹窗组件、行数据模型和现有按钮模式
- [x] 先补可失败的前端静态/组件契约测试
- [x] 实现行级清除按钮和清零逻辑
- [x] 运行目标验证并记录结果

## Expected Verification

- 目标测试先 RED 后 GREEN，覆盖每行存在“清除”按钮，点击后当前行分配数量变为 0。
- 受影响前端静态测试通过。
- 如全量检查受历史问题阻塞，记录首个无关阻塞并保留本任务目标验证证据。

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否
- 是否从根因和长期维护角度解决：是，按现有行数据模型增加正式清零动作，不添加旁路状态。
- 是否存在临时补丁或绕过：否

## Experience Gate

- 适用门禁：docs/frontend-development.md#前端确认提交上下文来源门禁。
- 摘要：确认分配类写接口不得从筛选态读取必填上下文；本任务只增加行级清零动作，并保持 leaderType 继续来自当前页签 activeLeaderTab / resolveCurrentLeaderType()。

## Verification Summary

- node tests/e2e/team-leader-report-allocation-clear-static.spec.cjs -> PASS
- node tests/e2e/team-leader-report-allocation-static.spec.cjs -> PASS
- node tests/e2e/team-leader-report-allocation-dialog-hide-static.spec.cjs -> PASS
- pnpm ts:check -> PASS
