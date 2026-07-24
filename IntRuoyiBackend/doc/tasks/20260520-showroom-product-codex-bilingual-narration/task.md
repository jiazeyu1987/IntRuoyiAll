# Task: 展厅产品 Codex CLI 讲解稿与双语语音生成

## Goal

在 `展厅 / 产品管理` 中补齐产品级讲解稿生成能力：产品详情里新增讲解稿文本框与 `生成讲解稿` 按钮，点击后使用当前配置的 Codex CLI 根据产品基础资料生成中文讲解稿；随后点击 `生成语音` 时，必须基于当前中文讲解稿生成中文语音，并通过当前配置的 Codex CLI 翻译出英文讲解稿后再生成英文语音。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-ai\src\main\java\cn\iocoder\yudao\module\ai\framework\ai\core\model\codexcli\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\**`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\**`

## Non-Scope

- 不改公司级讲解稿翻译/音频链路。
- 不新增 fallback、mock 成功、兼容分支或静默降级。
- 不顺带处理 `cover_image` live schema 漂移之外的其他 showroom 运行时问题。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-cover-image-live-schema-regression\task.md`
- Status before this task: blocked
- Impact: 已按当前用户优先级切换将上一任务显式标记为 blocked；其未完成状态会继续影响真实 `/showroom/product` 页面联调，但不阻塞当前后端契约与目标测试开发。

## Milestones

- [x] M1: 处理上一任务状态并创建本次后端任务文档/执行日志。
- [x] M2: 先补 RED 测试，锁定产品讲解稿生成、Codex CLI 翻译与双语语音生成契约。
- [x] M3: 实现 Codex CLI 产品讲解稿生成与英文翻译服务。
- [x] M4: 实现产品级双语语音生成链路，并保持 fail-fast 行为。
- [x] M5: 完成目标测试、证据记录与阻塞说明。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 视实现范围补充的产品讲解服务单测
- 若运行库允许，真实调用 `/showroom/product/generate-narration-script` 与 `/showroom/product/generate-narration-audio`

## Current Status

Completed on 2026-05-20. 后端产品级 Codex CLI 讲解稿与双语语音生成已落地，integration tests 通过，验证器通过。

## Final Verification Result

- PASS: `mvn -pl yudao-module-showroom "-Dtest=ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260520-showroom-product-codex-bilingual-narration\backend-api-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro --task-id 20260520-showroom-product-codex-bilingual-narration --mode preview`

## Blockers

- 无后端 blocker。
