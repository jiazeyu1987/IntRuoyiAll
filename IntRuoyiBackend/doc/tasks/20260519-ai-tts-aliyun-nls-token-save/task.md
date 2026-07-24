# 任务：保存可更换的 Aliyun NLS AccessToken

## Task Goal

让当前 TTS 测试流程中的阿里云 NLS `AccessToken` 可以在系统里修改并保存，后续 `aliyun_nls` 合成使用保存后的 token，不再只能依赖进程环境变量。

## Milestones

- [x] M1: 记录 BDD/TDD 场景和后端接口契约。
- [x] M2: 增加后端失败测试，覆盖 token 保存、读取脱敏状态、合成使用保存 token。
- [x] M3: 实现后端 token 持久化接口和合成 token 解析。
- [x] M4: 完成后端回归验证。
- [ ] M5: 使用有效阿里云 NLS AccessToken 完成真实音频生成验证并提交。

## Expected Verification

- `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test`
- `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`
- 使用真实前端/后端保存新的 Aliyun NLS AccessToken 后生成音频。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260519-ai-tts-aliyun-nls-token-save/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-ai-tts-aliyun-nls-token-save --mode preview`

## Current Status

Blocked by external credential. 后端保存、读取脱敏状态、合成使用保存 token、错误脱敏均已实现并通过内部验证；真实页面保存也已通过。新提供的阿里云 NLS AccessToken 已保存成功，但阿里云返回 `Meta:ACCESS_DENIED:The token '****' is invalid!`，无法完成“生成可播放音频”的最终外部验证。

## Verification Result

- PASS: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test`，17 tests passed.
- PASS: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`.
- PASS: Playwright 真实页面保存 Token 后显示 `已保存` 和脱敏值。
- PASS: 过期 Token 错误不再向前端回显明文 AccessToken。
- BLOCKED: Playwright 真实页面生成音频被阿里云拒绝；上一枚 AccessToken 过期，新提供的 AccessToken 被判定 invalid。

## Blocker And Impact

- Blocker: 需要新的有效阿里云 NLS AccessToken 才能完成真实音频生成验证。
- Impact: 当前代码可以保存并使用配置的 token，但由于外部凭证过期，无法证明阿里云返回可播放音频；按提交策略暂不提交本任务改动。
