# 任务：修复提示词模板正文乱码

## Goal

- 修复 `展柜 -> 提示管理` 页中 `PRODUCT_COVER` 当前提示词模板正文显示乱码的问题。
- 保持现有提示词版本接口、页面入口和版本历史结构不变，只修正后端对已存储乱码文本的读取修复逻辑。
- 确保当前运行库中已存在的 `PRODUCT_COVER` 历史版本在不改库的前提下可恢复为正常中文，用于页面显示和封面生成。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\main\java\cn\iocoder\yudao\module\showroom\prompt\ShowroomImagePromptVersionService.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\yudao-module-showroom\src\test\java\cn\iocoder\yudao\module\showroom\prompt\ShowroomImagePromptVersionServiceTest.java`
- `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-showroom-prompt-template-garbled-text-fix\**`

## Non-Scope

- 不改 `yudao-ui-admin-vue3` 前端页面布局、输入框组件或接口调用方式。
- 不直接重写当前运行库 `showroom_image_prompt_version` 表中的历史版本数据。
- 不扩展其他 AI 提示词场景或新增兜底模板逻辑。
- 不顺手修改提示词版本管理的权限、版本号策略或图片生成业务流程。

## Previous Task Check

- Previous same-repo task record:
  `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260524-version-center-release-readiness-check\task.md`
- Status before this task: `Completed`
- Impact on this task:
  上一同仓任务已完成，不阻塞本次乱码修复；当前后端工作树开始时无未提交改动，可独立收口本任务。

## Milestones

- [x] M1：复核同仓前序任务状态，建立本任务文档与执行日志。
- [x] M2：先补 RED，复现 `windows-1252` 风格 UTF-8 乱码在当前修复逻辑下仍返回损坏文本的问题。
- [x] M3：实现严格可逆的乱码修复逻辑，避免 `ISO-8859-1` 路径把字符再次损坏为 `�` / `?`。
- [x] M4：运行后端定向测试与真实接口验证，确认当前/历史提示词都恢复正常中文。
- [x] M5：更新任务文档、记录最终验证，并检查提交边界。

## Expected Verification

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- 真实接口验证：
  `POST http://127.0.0.1:48081/admin-api/system/auth/login` with `tenant-id=122`, `aoteman/admin123`
- 真实接口验证：
  `GET http://127.0.0.1:48081/admin-api/showroom/prompt/current?sceneCode=PRODUCT_COVER`
- 真实接口验证：
  `GET http://127.0.0.1:48081/admin-api/showroom/prompt/history?sceneCode=PRODUCT_COVER`

## Current Status

- Completed on 2026-05-24.
- 已完成后端修复：
  - `normalizePromptTextContent` 改为逐行修复；
  - 使用单字节逆向映射恢复 `cp1252` 特殊字符与保留控制位混合的 UTF-8 乱码；
  - 不再依赖会把不可映射字符降成 `?` 的 `ISO-8859-1` 盲转逻辑。
- 已补充回归覆盖：
  - 保留原有 `ISO-8859-1` 风格乱码修复用例；
  - 新增混合 `cp1252` / 控制位乱码修复用例；
  - 新增“前半段乱码、后半段正常中文补充行”混合场景回归用例。
- 已完成真实运行态验证：
  - 本地 `48081` 运行库已切到 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260524-145748.jar`；
  - 真实登录测试租户后，`/admin-api/showroom/prompt/current` 与 `/admin-api/showroom/prompt/history` 返回的模板正文汉字数恢复到 `460`，且不再包含 `�` / `?`。

## Risks / Blockers

- 当前无阻塞。

## Final Verification Result

- `mvn -pl yudao-module-showroom "-Dtest=ShowroomImagePromptVersionServiceTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` -> PASS
- `mvn -pl yudao-server -am -DskipTests package` -> PASS
- `GET http://127.0.0.1:48081/v3/api-docs` -> PASS, HTTP 200
- 真实登录 `tenant-id=122 / aoteman / admin123` 后调用：
  - `GET http://127.0.0.1:48081/admin-api/showroom/prompt/current?sceneCode=PRODUCT_COVER` -> PASS
  - `GET http://127.0.0.1:48081/admin-api/showroom/prompt/history?sceneCode=PRODUCT_COVER` -> PASS
  - 两个接口的模板正文均恢复为高汉字密度中文文本，且不再包含 `�` / `?`
