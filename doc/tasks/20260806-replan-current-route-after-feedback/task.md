# 20260806-replan-current-route-after-feedback

## Task Goal

修复手动重排中“受保护任务未绑定工作站”的错误阻断：历史报工/旧任务只用于计算已完成量和剩余量；剩余未完成工序必须按当前最新工艺路线的工作站、产线和产能重新排产。

## Milestones

- M1：建立隔离 worktree、任务记录、BDD/TDD 证据。
- M2：新增回归测试，复现老报工任务缺工作站时不应阻断重排。
- M3：修改自动排产保护逻辑，使缺工作站的历史受保护任务不再阻断当前剩余量排产。
- M4：运行目标后端回归，确认不引入 fallback、吞异常或旧数据依赖。
- M5：完成验证报告、经验沉淀、cleanup、提交和推送。

## Expected Verification

- `mvn -pl yudao-module-mes -am "-Dtest=MesProAutoScheduleServiceImplTest" test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence doc/tasks/20260806-replan-current-route-after-feedback/bug-regression-evidence.md`
- 提交前运行 `scripts\preflight\branch-runtime-port-guard.ps1`。

## Current Status

ready_for_closeout

- 已创建隔离 worktree：`D:\IntRuoyiWorktree\replan-current-route-after-feedback`。
- 已创建分支：`codex/replan-current-route-after-feedback`，基于 `origin/int_main`。
- 已登记 runtime slot：`int_main slot=2`，前端 `8083`，后端 `48083`；本任务暂不启动服务。
- 已完成核心实现、证据文档、目标回归验证和分支端口预检；待提交和推送。
- Cleanup preview 已运行，无删除项；自动 merge/remove worktree 被主工作区 `E:\IntRuoyi` 脏改动阻断，需待主工作区清洁后再执行 closeout apply。

## Experience Gate

- 已读取 `docs\experience-index.md`，匹配门禁：`docs/backend-development.md#第三方报工直报正式链路门禁`。
- 本次用户已明确更正业务口径：历史报工/已完成任务的工作站缺失不得阻断剩余量按当前工艺路线重排。
- 已同步更新 `docs/backend-development.md#第三方报工直报正式链路门禁` 和 `docs/experience-index.md`，避免继续按旧门禁要求补旧任务工作站。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；从重排语义上区分历史已发生事实与当前剩余量资源选择。
- `是否存在临时补丁或绕过`：否。

## Cleanup Keep

- `doc/tasks/20260806-replan-current-route-after-feedback/bug-regression-evidence.md`
