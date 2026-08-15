# Bug Regression Evidence

## Bug Summary

- 干净 worktree 缺少两个已被正式代码引用的 DCC assignment candidate DTO，导致 DCC reactor 编译失败。
- 期望：提交必须自包含，干净 checkout 不依赖主工作区未跟踪文件即可编译。

## Expected Behavior

- 干净隔离 worktree 中，正式 controller、service、mapper 和测试引用的两个 assignment candidate DTO 必须由同一提交提供，且字段与既有调用合同一致。

## Reproduction

- RED command: `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`.
- Actual result: FAIL（退出码 1，2026-08-15 03:38:32 +08:00）；DCC 主源码编译报告 15 个 `cannot find symbol`，缺失类型仅为 `DccProjectCodeAssignmentCandidatePageReqVO` 与 `DccProjectCodeAssignmentCandidateRespVO`，未到达 Surefire。

## Root Cause

- 提交 `333029852` 纳入了 DTO 消费者但遗漏 DTO 文件本身。

## Regression Test

- 复用正式 DCC service/controller 编译边界及 `DccProjectCodeAssignmentServiceImplTest`；缺类型时在 compile/testCompile 阶段失败。

## TDD Evidence

- RED: reproduced from a clean isolated worktree before adding either DTO; the exact focused reactor command failed at DCC compile for the two missing contracts.
- GREEN: 增加且仅增加两个缺失 DTO 后，原 RED 命令 PASS（退出码 0，2026-08-15 03:42:39 +08:00）；Surefire 实际运行 13 项，Failures 0、Errors 0、Skipped 0。

## Verification

- REGRESSION: `mvn.cmd -pl yudao-module-dcc -am "-DskipITs" "-Dtest=DccProjectCodeAssignmentServiceImplTest,DccControlledFileMapperTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS（退出码 0，2026-08-15 03:46:32 +08:00）；共 23 项，Failures 0、Errors 0、Skipped 0。
- Root-cause boundary: 未编辑既有消费者或测试来绕过缺失类型，未引入兼容 DTO、fallback、mock success 或默认值。

## Risk And Regression Scope

- 风险限定为类型合同补齐；禁止顺带改变权限、分页、服务行为或数据库。

## Blockers And Follow-up

- 代码、目标测试、回归、技能 validator 与端口 guard 无 blocker；后续仍需主管代码评审和独立 Agent 验证。
- 主管预建且禁止本 executor 编辑的 `task.md` 第 39 行存在 `new blank line at EOF`；主管提交前需修正并复跑 staged `git diff --check`。本 executor 拥有的 6 个文件逐文件 whitespace check 全部通过。
