# Task: 展厅封面生成默认本机 Codex 命令

## Goal

修复展厅产品“点击生成封面”在未显式配置 `yudao.ai.codex-cli.command` 时直接报
`SHOWROOM_COVER_GENERATION_FAILED: codex cli command is required` 的问题。

本次修复要求：

- 封面生成默认走本机可执行的 `codex.cmd` / `codex`，不再把 `command` 当作必填。
- 生成提示词继续走 Codex 原生图片生成，并按 `generate-ai-scene-image` 的图片提示结构收敛。
- 如果本机默认 `codex` 命令实际不存在、执行失败、超时、未返回有效 PNG 路径，仍必须 fail-fast，不得降级。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\**`

## Non-Scope

- 不改前端接口路径或按钮权限。
- 不恢复 SiliconFlow 作为 fallback。
- 不修复与当前工作树中 `ShowroomHttpApiIntegrationTest` 历史编译漂移无关的问题。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-codex-cli-generation\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 上一条封面切换任务已完成，本次作为 follow-up 回归修复继续收口默认命令行为。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 当前仓库仍有其他目录的在途改动，但目标封面服务与独立回归测试当前干净。
- Impact: 本次只修改封面服务、独立回归测试和当前任务文档。

## Milestones

1. 创建 follow-up 任务文档、执行日志和 bug regression evidence。
2. 先补 RED 回归，锁定“缺少 command 也应默认使用本机 Codex 命令”。
3. 最小修复默认命令解析，并让提示词更贴近 `generate-ai-scene-image` 的 prompt 结构。
4. 跑通定向 GREEN、bug evidence 校验与 closeout preview。
5. 单独提交本任务范围改动。

## Expected Verification

- `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\bug-regression-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-default-codex-command --mode preview`

## Current Status

- Status: In Progress
- Completed work:
  - 已确认上一同仓封面任务完成。
  - 已定位当前报错根因：`resolveCodexCommand(...)` 仍把 `command` 视为必填。
- Remaining blockers:
  - RED/修复/GREEN 尚未完成。

## Milestone Status

### Milestone 1

- Status: Completed
- Completed work:
  - 已创建 follow-up 任务文档、执行日志和 bug regression evidence。
  - 已确认上一同仓封面任务完成，不阻塞本次修复。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\task.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\execution-log.md`
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\bug-regression-evidence.md`
- Remaining blockers:
  - 需要完成 RED/修复/GREEN。

### Milestone 2

- Status: Completed
- Completed work:
  - 已新增默认命令解析与 prompt shape 回归断言。
  - 已执行 RED，确认当前行为仍直接要求显式 `command`，且 prompt 未按 `generate-ai-scene-image` 结构组织。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`（RED）
- Remaining blockers:
  - 需要完成生产代码修复。

### Milestone 3

- Status: Completed
- Completed work:
  - 已将 `resolveCodexCommand(...)` 改为默认返回本机 `codex.cmd` / `codex`。
  - 已把封面图片提示词改写为更贴近 `generate-ai-scene-image` 的 prompt shape。
- Verification evidence:
  - `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- Remaining blockers:
  - 待跑通 GREEN、校验证据并提交。

### Milestone 4

- Status: Completed
- Completed work:
  - 已确认主源码编译通过。
  - 已确认独立回归测试 4/4 通过。
  - 已确认当前机器默认 `codex` 命令可执行。
  - 已通过 bug evidence 校验与 closeout preview。
- Verification evidence:
  - `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
  - `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
  - `codex --version`
  - `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\bug-regression-evidence.md`
  - `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-default-codex-command --mode preview`
- Remaining blockers:
  - 待完成任务范围提交。

### Milestone 5

- Status: Completed
- Completed work:
  - 已将变更范围收敛到封面服务、独立回归测试和当前任务目录。
  - 已创建本任务独立 commit `c6a1af416d`。
- Verification evidence:
  - `git status --short -- yudao-module-showroom/src/main/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageService.java yudao-module-showroom/src/test/java/cn/iocoder/yudao/module/showroom/cover/ShowroomProductCoverImageServiceTest.java doc/tasks/20260521-showroom-product-cover-default-codex-command`
  - `git commit -m "任务: 修复封面生成默认Codex命令"`
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile`
- PASS: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test`
- PASS: `codex --version` -> `codex-cli 0.128.0`
- PASS: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\bug-regression-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-default-codex-command --mode preview`
- PASS: `git commit -m "任务: 修复封面生成默认Codex命令"` -> `c6a1af416d`

## Current Status

- Status: Completed
- Completed work:
  - 已移除“必须显式配置 `yudao.ai.codex-cli.command`”这一错误前提，默认改为本机 `codex.cmd` / `codex`。
  - 已将封面生成提示词改写为更贴近 `generate-ai-scene-image` 的 prompt shape。
  - 已完成独立回归测试、bug evidence 校验、closeout preview 与任务范围提交。
- Remaining blockers:
  - None within this follow-up scope.
