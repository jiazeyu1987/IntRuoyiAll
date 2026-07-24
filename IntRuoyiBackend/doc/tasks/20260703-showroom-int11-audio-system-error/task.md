# 任务：修复产品语音未生成时系统异常

- Task ID: 20260703-showroom-int11-audio-system-error
- Created: 2026-07-03
- Current Status: completed

## Current Status

completed

## Task Goal

修复测试服 `INT-11 / Revision 5502` 打开产品语音弹窗时，中英文语音未生成却返回 `code=500 系统异常` 的问题，使缺少语音记录时返回可识别业务错误，前端按现有逻辑展示“未生成”。

## Milestones

1. 复现测试服语音读取系统异常。completed
2. 增加缺语音业务错误回归测试。completed
3. 最小修复后端 `getNarration` 缺记录错误映射。completed
4. 运行目标回归测试。completed
5. 记录证据并提交改动。pending

## Expected Verification

- `getNarration(PRODUCT, ..., ZH/EN)` 在缺少语音版本时不再走系统异常。
- 前端 `ProductAudioDialog` 现有 `SHOWROOM_TARGET_NOT_FOUND: narration not found` 判断可识别该错误并展示未生成状态。
- 目标 Maven 回归测试通过。

## 经验门禁

- 已读取 PowerShell、测试服访问、登录访问和缺陷修复门禁。
- 测试服写入仅限用户已授权的旧产品清理；本次代码修复在本地仓库完成，测试服运行态需随后发布才能生效。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是，将缺语音记录从未分类运行时异常改为业务错误。
- 是否存在临时补丁或绕过：否。

## Final Verification Result

- RED: 测试服真实接口 `/admin-api/showroom/narration/get?targetType=PRODUCT&targetId=906&audienceType=PUBLIC&language=ZH/EN` 返回 `code=500`、`msg=系统异常`。
- GREEN: `mvn -f D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\pom.xml -pl yudao-module-showroom "-Dtest=ShowroomProductNarrationRegressionTest#getProductNarrationShouldReturnBusinessErrorWhenNarrationMissing" test` -> PASS。
- 注意：测试服“生成中英文语音”仍因运行环境缺少 `codex` CLI 报系统异常，属于生成链路前置缺失；本次修复的是语音读取弹窗缺记录时不应系统异常。

## Current Blockers

- 测试服运行态尚未发布本地代码修复；下次发布后弹窗读取缺语音将显示未生成，不再显示系统异常。