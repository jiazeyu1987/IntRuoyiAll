# Execution Log

BDD: long company narration audio should not use oversized request URI -> Given 展厅公司中文介绍超过阿里云 NLS 短文本请求可安全承载的 URI 长度 / When 后端发起语音合成 / Then 请求必须不再把全文拼进 URI，而应通过可承载长文本的请求体发送，并继续显式传递鉴权头。

BDD: long company narration audio should synthesize wav across chunks -> Given 展厅公司中文介绍超过阿里云 NLS 单次短文本长度上限 / When 后端发起语音合成 / Then 合成器必须按真实文本顺序分段请求并拼接成一个可解析的 WAV 字节结果，不得静默截断、默认成功或 fallback 到其他音频来源。

RED: `mvn --% -pl yudao-module-ai -Dtest=AliyunNlsTtsSynthesizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，新增回归显示当前实现仍对短文本和长文本都使用 `GET /stream/v1/tts`；`synthesize_sendsNlsTokenAndParams_returnsAudioBytes` 断言期望 `POST` 但实际为 `GET`，`synthesize_whenTextExceedsShortLimit_splitsRequestsAndJoinsWav` 期望分两段请求但实际只发出 1 次请求。

GREEN: `mvn --% -pl yudao-module-ai -Dtest=AliyunNlsTtsSynthesizerTest -Dsurefire.failIfNoSpecifiedTests=false test` -> PASS，合成器已改为 `POST` JSON 请求；当文本超过 300 字符时按标点优先分段请求，并把多段 WAV 合并为一个可解析的 WAV，6 条定向测试全部通过。

GREEN: `python C:\Users\BJB110\.codex\skills\bug-regression-fix-loop\scripts\validate_bug_regression.py --evidence D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-audio-414-fix\bug-regression-evidence.md` -> PASS，缺陷证据文档满足回归闭环要求。

BLOCKER: `mvn --% -pl yudao-module-showroom -Dtest=ShowroomAliyunNlsAudioGenerationAdapterTest -Dsurefire.failIfNoSpecifiedTests=false test` -> FAIL，当前工作区存在与本次任务无关的 showroom 测试编译错误：`ShowroomDisplayCompanyAnonymousContractTest` 引用了不存在的 `payload.subtitle()`，`ShowroomHttpApiIntegrationTest` 仍以 4 个参数构造 `CompanyNarrationGenerateReqVO`。这两个错误位于现有脏工作区，不是本次 `AliyunNlsTtsSynthesizer` 修改引入，但会阻断额外的 showroom 模块回归编译。
