# 提交当前前后端代码

## Task Goal

盘点当前 `int_main` 工作区的前后端实现、测试和迁移改动，按任务归属完成定向验证、提交并推送到 `origin/int_main`；不提交未验证的资源文件、临时产物或未完成任务改动。

## Milestones

- [x] M1：识别当前脏改动、任务归属和提交边界。
- [x] M2：为纳入提交的后端、前端和迁移改动执行 BDD/TDD 定向验证。
- [x] M3：完成 staged 清单、敏感信息和大文件检查，提交实现。
- [x] M4：执行 task closeout，提交收尾记录并推送 `int_main`。

## Expected Verification

- 受影响后端模块的定向 Maven 回归或明确记录阻塞。
- 前端 `pnpm ts:check` 与受影响静态合同。
- SQL 静态合同和全量 release migration policy gate（若纳入 SQL）。
- `git diff --cached --check`、禁止产物扫描、推送后分支不 ahead。

## Design Constraints Check

- 不提交 `resource/` Office 文件、`target/`、运行日志、临时配置或凭据。
- 不覆盖或回滚并行任务改动；无法验证的任务保持工作区原状并记录。
- 不执行真实 E2E、数据库写入或 `int_main` 重启，除非本轮另行授权。
- 不引入 fallback、默认成功或吞异常。

## Current Status

completed

实现提交与 cleanup 已完成；最终收尾提交和 push 在本记录提交后执行。
