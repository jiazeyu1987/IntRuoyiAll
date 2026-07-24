# 任务：接入 Aliyun NLS TTS Provider

## Task Goal

在当前 TTS 测试流程中新增 `aliyun_nls` provider，通过阿里云 NLS REST 流式接口生成音频，并保持缺失配置、非法 provider、非法音色时失败快显，不回退到 Windows 或 DashScope。

## Milestones

- [x] M1: 记录 BDD/TDD 场景和后端接口契约。
- [x] M2: 增加失败测试，覆盖 NLS provider 分发、NLS alias、非法音色、缺失 token、非 200 响应。
- [x] M3: 实现 `AliyunNlsTtsSynthesizer`、配置绑定、provider 分发和音频媒体类型。
- [x] M4: 使用真实本地后端和运行时环境变量验证 Aliyun NLS 返回可播放 WAV 音频。
- [x] M5: 完成回归验证和任务收尾预览。

## Expected Verification

- `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AliyunNlsTtsSynthesizerTest" test`
- `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package`
- 使用真实登录 token 调用 `/admin-api/ai/tts-test/generate`，请求 `provider=aliyun_nls`、`voice=xiaoyun`，确认返回 `audio/wav` 和 `RIFF/WAVE` 音频字节。
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc/tasks/20260519-ai-tts-aliyun-nls-provider/backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-ai-tts-aliyun-nls-provider --mode preview`

## Current Status

Completed.

## Verification Result

- PASS: 后端单元测试覆盖 `aliyun_nls`、`nls` alias、非法 NLS 音色、缺失 access-token、NLS 非 200 响应。
- PASS: 打包 `yudao-server.jar` 成功。
- PASS: 真实后端接口返回 HTTP 200，`content-type=audio/wav;charset=UTF-8`，音频大小约 88KB，字节头为 `RIFF...WAVE`。
- PASS: 运行时 AppKey 和 AccessToken 仅通过环境变量传入，未写入代码、配置或文档。

## Notes

- DashScope 真实音频验证仍受前序任务的无效 DashScope API Key 阻塞；本任务不引入 provider fallback。
