# 重排班次小时默认值回归

## Task Goal

修复点击重排仍提示 `排产资源缺少班次小时配置，routeProcessId=926632, workstationId=980008` 的回归：重排剩余任务时，当前工艺路线/工作站缺少班次小时也必须按用户确认的默认 `10.5` 小时计算，不应由旧报工或旧任务资源快照决定后续排产。

## Milestones

- [x] 建立任务记录、BDD 场景和适用门禁。
- [x] 定位重排实际报错路径并补充 RED 回归测试。
- [x] 实现最小后端修复，保持缺工作站/缺产能等资源错误 fail-fast。
- [x] 运行目标 Maven 回归和证据验证。
- [x] 收尾清理、提交并推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest#*" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `git diff --check -- IntRuoyiBackend/yudao-module-mes/src/main/java IntRuoyiBackend/yudao-module-mes/src/test/java doc/tasks/20260806-replan-shift-hours-default-regression`
- backend/bug regression evidence validators.

## Applicable Gates

- `docs/backend-development.md#第三方报工直报正式链路门禁`：旧报工只扣已完成/剩余量，剩余任务按当前工艺路线排产；重排资源校验必须看当前路线剩余工序。
- `docs/backend-development.md#第三方报工直报正式链路门禁`：工作站班次小时缺失按默认 `10.5` 计算，不作为手动重排 blocker；缺当前路线工作站、产线或产能仍 fail-fast。
- `docs/powershell-memory.md#maven-单模块陈旧依赖门禁`：目标 Maven 使用 `-pl yudao-module-mes -am`。
- `docs/powershell-memory.md#同文件并行改动选择性暂存门禁`：主工作区有并行任务脏改动，提交只暂存本任务 hunks。
- `docs/task-closeout-rules.md`：记录 RED/GREEN，cleanup 后再 completed。

## Current Status

completed

实现提交 `c76f3fec0` 已创建；收尾记录完成，等待本收尾提交随任务一起推送验证。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：是；用户明确要求班次小时缺失默认 `10.5`。触发条件仅限班次小时为空或无效。
- `是否从根因和长期维护角度解决`：是；需要覆盖重排路径，而不是只修自动排产/创建路径。
- `是否存在临时补丁或绕过`：否。
