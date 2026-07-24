# Execution Log

BDD: TTS 测试页选择阿里云 NLS -> Given 用户进入 AI 音乐管理页的 `TTS 测试` When 选择 `阿里云 NLS` Then 页面显示 NLS 音色下拉并默认选择 `xiaoyun`

BDD: TTS 测试页提交 NLS 音色 -> Given 用户输入一句测试文本并选择 `阿里云 NLS` 和 `xiaoyun` When 点击生成音频 Then 前端请求体提交 `provider=aliyun_nls` 和 `voice=xiaoyun`

BDD: TTS 测试页播放和暂停音频 -> Given NLS 音频生成成功 When 用户点击播放按钮 Then 音频开始播放且按钮显示 `暂停播放`；When 再次暂停 Then 按钮恢复为 `播放音频`

RED: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> FAIL, 初始 TTS 请求类型没有 provider 和 voice 字段，页面没有 NLS provider 和音色选择

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS

GREEN: Playwright route `http://localhost:8081/ai/console/music` -> PASS, `TTS 测试` 页显示 `阿里云 NLS`

GREEN: Playwright NLS voice selector -> PASS, options were `xiaoyun 女声`, `xiaogang 男声`, `ruoxi 女声`, `siqi 女声`

GREEN: Playwright request probe -> PASS, generated request body was `{"text":"今天是周一，天气挺好的。","provider":"aliyun_nls","voice":"xiaoyun"}`

GREEN: Playwright response probe -> PASS, response headers included `content-type: audio/wav;charset=UTF-8`, body saved by Playwright as WAV

GREEN: Playwright playback probe -> PASS, after generating longer NLS audio, clicking `播放音频` changed the button to `暂停播放`
