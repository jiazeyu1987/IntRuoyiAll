# Execution Log

BDD: 保存 Aliyun NLS AccessToken -> Given 管理员在 TTS 测试页输入新的阿里云 NLS AccessToken When 点击保存 Then 后端将 token 保存到系统参数配置且标记为不可见

BDD: 合成使用保存 token -> Given 系统已保存新的阿里云 NLS AccessToken When 用户选择 `aliyun_nls` 生成音频 Then 后端使用保存 token 调用阿里云 NLS，不使用旧的环境变量 token

BDD: Token 状态脱敏展示 -> Given 系统已保存 Aliyun NLS AccessToken When 前端加载 TTS 测试页 Then 只显示是否已保存和脱敏 token，不回显完整 token

BDD: 阿里云错误信息脱敏 -> Given 阿里云 NLS 返回的错误体包含 AccessToken When 后端向前端返回错误 Then 错误信息必须隐藏完整 AccessToken

RED: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test` -> FAIL, `AiTtsAliyunNlsCredentialService` does not exist.

GREEN: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test` -> PASS, 16 tests passed after token save/read implementation.

RED: `mvn -pl yudao-module-ai "-Dtest=AliyunNlsTtsSynthesizerTest#synthesize_whenNlsErrorMentionsAccessToken_masksTokenInMessage" test` -> FAIL, error message still contained the current AccessToken.

GREEN: `mvn -pl yudao-module-ai "-Dtest=AliyunNlsTtsSynthesizerTest#synthesize_whenNlsErrorMentionsAccessToken_masksTokenInMessage" test` -> PASS, AccessToken is replaced with `****` in error body preview.

GREEN: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test` -> PASS, 17 tests passed.

GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS.

BLOCKED: Playwright real path saved the token and confirmed masked status, but Aliyun NLS rejected synthesis because the provided AccessToken is expired.

BLOCKED: Playwright real path saved the new masked token value and confirmed masked status, but Aliyun NLS rejected synthesis with `Meta:ACCESS_DENIED:The token '****' is invalid!`.
