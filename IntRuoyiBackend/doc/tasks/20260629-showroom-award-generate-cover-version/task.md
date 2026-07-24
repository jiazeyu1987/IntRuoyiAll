# 任务：奖项行内生图并自动发布新版本

- Task ID: `20260629-showroom-award-generate-cover-version`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `in_progress`

## Task Goal

新增 `POST /showroom/award/generate-cover-image` 正式接口：基于当前奖项封面图与奖项元数据生成优化后的新封面，替换当前封面并自动发布新的奖项修订版本。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-showroom-award-tab-empty\task.md`
- 状态：`blocked`
- 处理说明：上一任务未复现接口空数据问题，已保留阻塞说明；本次需求独立，不复用其未完成判断。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文日志、命令与文档统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：奖项列表行内操作维持密集操作台样式，使用同排文字按钮，不破坏表格结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。通过正式奖项封面生成与版本发布链路完成，不用前端本地临时方案。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 奖项生图接口成功后自动发布新版本 -> Given 奖项当前封面可读且当前版本中英文语音完整 / When 调用 /showroom/award/generate-cover-image / Then 系统生成新封面、创建新修订版、克隆可复用语音并发布为当前版本。`
- `BDD: 奖项缺少当前封面时生图失败 -> Given 奖项当前封面为空或不可读 / When 调用 /showroom/award/generate-cover-image / Then 接口显式失败且不产生新发布版本。`
- `BDD: 奖项当前版本缺少可发布语音时生图失败 -> Given 奖项当前版本缺少中英文已发布语音 / When 调用 /showroom/award/generate-cover-image / Then 接口显式失败且不发布半成品修订版。`

## Milestones

1. M1：建立任务文档并补 RED。`completed`
2. M2：实现奖项生图、版本递增与语音继承。`completed`
3. M3：完成回归验证与证据记录。`completed`
4. M4：完成 AWARD-003 真实页面生图验收。`in_progress`

## Expected Verification

- `mvn --% -pl yudao-module-showroom -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`

## Current Blockers

- 当前本机代码与定向测试已通过，且后端已补上奖项生图子进程的正式 `OPENAI_*` 配置入口。
- 当前本机代码已进一步收紧为只允许 `$generate-ai-scene-image` 技能路径；若 Codex CLI 输出出现 `imagegen` / `scripts/image_gen.py` / 本地非生成式增强等 fallback 标记，后端会直接 fail fast。
- `AWARD-003` 于 `2026-06-29 16:09:14` 的最新真实页面点击已命中新的 fail-fast：Codex CLI 明确执行了 `$generate-ai-scene-image` 技能提示，但 stdout 返回 `FAIL: built-in image generation tool unavailable in this session.`；当前会话随后又落出了 `image_gen.py` 标记，因此后端按设计拒绝发布任何 fallback 产物。
- `AWARD-003` 真实页面生图仍受外部图片 provider 阻塞：截至 2026-06-29，本机已验证的三套正式凭据/地址组合分别返回 `401 Token is invalid`、`503 全部渠道不可提供当前模型，请稍后重试`、`401 invalid_api_key`，因此暂时无法真实生成并发布新图。

## Final Verification

- `mvn --% -pl yudao-module-showroom -Dtest=ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
- `mvn --% -pl yudao-module-showroom,yudao-module-ai -Dtest=ShowroomNativeImageGenerationServiceTest,ShowroomAwardGenerateCoverIntegrationTest,ShowroomProductCoverImageServiceTest -Dsurefire.failIfNoSpecifiedTests=false test`
