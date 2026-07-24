# 任务：展厅封面 503 错误信息收敛（后端）

## Goal

修复一键封面在上游 OpenAI native `image_generation` 返回错误文本时，本地代码把错误文本误当作文件路径解析，最终暴露出 `Illegal char <:>` 噪音前缀的问题；要求错误信息直接暴露真实上游失败原因。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\cover\ShowroomProductCoverImageServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-cover-503-error-message-hardening\**`

## Non-Scope

- 不新增 503 自动重试或 fallback。
- 不修改 OpenAI 图片生成上游服务。
- 不修改一键封面前端交互。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260522-showroom-cover-failed-items-preview-asset-repair\task.md`
- Status before this task: `Completed`
- Impact: 上一任务已修复 preview asset 缺失问题，不阻塞本次继续收敛 503 错误信息。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Current state: 仓库存在并行未提交改动。
- Impact: 本任务仅修改封面生成错误信息收敛逻辑、定向测试与任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定“错误文本不应再被当作路径解析”的可观察行为。
2. 先补 RED，构造 `--output-last-message` 返回错误文本的场景。
3. 最小实现错误文本识别与更干净的 fail-fast 提示。
4. 跑定向回归并更新证据。
5. 执行 closeout preview。

## Expected Verification

- `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview`

## Current Status

Completed.

## Completed Work

- 已在 `ShowroomProductCoverImageService` 中把“读取到的最后一条消息不是合法路径”单独收敛为更可读的 fail-fast 异常。
- 修复后，上游 `image_generation` 返回的错误文本会直接作为 `SHOWROOM_COVER_GENERATION_FAILED` 的正文暴露，不再先经过 `Path.of(...)` 导致 `Illegal char <:>` 噪音。
- 已补充定向测试，覆盖“`--output-last-message` 输出的是错误文本而不是路径”的场景。

## Verification Result

- PASS: `mvn -pl yudao-module-showroom -am clean "-Dtest=ShowroomProductCoverImageServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260522-showroom-cover-503-error-message-hardening --mode preview`

## Final Assessment

- 当前 `503 Service temporarily unavailable` 本身仍是上游图片生成服务问题，未在本任务中处理。
- 但本地系统现在会更干净地暴露真实上游错误，不再用 `Illegal char <:>` 混淆排障。
