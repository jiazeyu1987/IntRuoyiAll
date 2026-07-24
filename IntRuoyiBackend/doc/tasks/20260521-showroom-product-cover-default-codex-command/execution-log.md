# Execution Log: 展厅封面生成默认本机 Codex 命令

BDD: 未显式配置 command 也能生成封面 -> Given 运行环境未设置 `yudao.ai.codex-cli.command` 且本机存在默认 `codex.cmd` / `codex` 可执行命令 / When 管理员点击生成封面 / Then 后端应默认使用本机 Codex 命令继续生成，而不是直接报 `codex cli command is required`。

BDD: 默认命令缺失时仍 fail-fast -> Given 未显式配置 `command` 且本机默认 Codex 命令不可执行 / When 管理员点击生成封面 / Then 后端必须暴露明确执行失败，不得降级到其他图片服务或占位图。

BDD: 封面提示词按 generate-ai-scene-image 结构组织 -> Given 封面生成仍走 Codex 原生图片生成 / When 后端构造图片提示词 / Then 提示词应包含清晰的 `Scene / Style / Composition / Lighting and mood / Details / Constraints / Avoid` 结构，减少随机图像偏差。

RED: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> FAIL，`resolveCodexCommand(null)` 仍抛出 `SHOWROOM_COVER_GENERATION_FAILED: codex cli command is required`，且 `buildPrompt(...)` 尚未满足 `generate-ai-scene-image` 的 prompt shape 断言。

GREEN: `mvn --% -pl yudao-module-showroom -DskipTests -Dmaven.compiler.useIncrementalCompilation=false compile` -> PASS。

GREEN: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false surefire:test` -> PASS，4 tests green，已覆盖默认命令解析、prompt shape、显式命令成功路径与缺失文件 fail-fast。

GREEN: `codex --version` -> PASS，当前机器默认命令可执行，返回 `codex-cli 0.128.0`。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command\bug-regression-evidence.md` -> PASS。

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260521-showroom-product-cover-default-codex-command --mode preview` -> PASS，preview 状态 `ready`，默认保留 `task.md` / `execution-log.md`，若 apply 会删除 `bug-regression-evidence.md`。

GREEN: `git commit -m "任务: 修复封面生成默认Codex命令"` with `TDD_TASK_DIR=D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-product-cover-default-codex-command` -> PASS，创建 commit `c6a1af416d`。
