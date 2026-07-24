# Execution Log

BDD: Aliyun NLS provider 生成音频 -> Given 管理端提交 TTS 测试文本、`provider=aliyun_nls`、合法 NLS 音色 When 后端处理生成请求 Then 系统通过阿里云 NLS REST 流式接口返回可播放 WAV 音频

BDD: NLS alias 兼容 -> Given 管理端或兼容调用提交 `provider=nls` When 后端解析 provider Then 系统等价使用 `aliyun_nls` provider 且不切换到其它 provider

BDD: 阿里云 NLS 配置缺失不降级 -> Given 运行时缺少 NLS `appkey` 或 `access-token` When 后端生成 TTS Then 请求失败并暴露缺失配置，不回退到 Windows 或 DashScope

BDD: 非法阿里云 NLS 音色不透传 -> Given 请求提交未支持的 NLS voice When 后端处理请求 Then 请求失败并提示不支持该音色

RED: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AliyunNlsTtsSynthesizerTest" test` -> FAIL, `AliyunNlsTtsSynthesizer` 不存在且后端没有 `aliyun_nls` provider 分发

RED: `mvn -pl yudao-module-ai "-Dtest=AliyunNlsTtsSynthesizerTest#synthesize_sendsNlsTokenAndParams_returnsAudioBytes" test` -> FAIL, 默认发送 `speech_rate=1.0` 和 `pitch_rate=1.0`，与阿里云 NLS REST 接口整数参数要求不一致

GREEN: `mvn -pl yudao-module-ai "-Dtest=AliyunNlsTtsSynthesizerTest#synthesize_sendsNlsTokenAndParams_returnsAudioBytes" test` -> PASS, NLS 默认不再发送浮点语速和语调参数

GREEN: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AliyunNlsTtsSynthesizerTest" test` -> PASS, 12 tests, 0 failures

GREEN: `mvn -pl yudao-server -am "-Dmaven.test.skip=true" package` -> PASS

GREEN: real backend `/admin-api/ai/tts-test/generate` with `provider=aliyun_nls, voice=xiaoyun` -> PASS, HTTP 200, `audio/wav;charset=UTF-8`, 88844 bytes, `RIFF...WAVE`
