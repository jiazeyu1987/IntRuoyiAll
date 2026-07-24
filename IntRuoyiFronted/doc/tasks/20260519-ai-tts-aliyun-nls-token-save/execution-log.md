# Execution Log

BDD: TTS 测试页保存 Aliyun NLS AccessToken -> Given 用户进入 `TTS 测试` 并选择 `阿里云 NLS` When 输入新的 AccessToken 并点击保存 Then 页面提示保存成功并刷新脱敏配置状态

BDD: AccessToken 不回显完整值 -> Given 系统已保存 Aliyun NLS AccessToken When 页面加载配置状态 Then 页面只显示脱敏值，不显示完整 token

BDD: 保存后生成音频 -> Given 已保存新的 AccessToken When 用户点击生成音频 Then 请求使用 `provider=aliyun_nls` 并由后端返回可播放音频

RED: `node doc\tasks\20260519-ai-tts-aliyun-nls-token-save\scripts\verify-tts-token-ui.mjs` -> FAIL, API and pane did not yet expose token status/load/save contract.

GREEN: `node doc\tasks\20260519-ai-tts-aliyun-nls-token-save\scripts\verify-tts-token-ui.mjs` -> PASS, the API and TTS pane expose token status, save flow, hidden input, and masked display.

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS.

GREEN: Playwright login/save/status -> PASS, saved Aliyun NLS AccessToken and refreshed masked status on the TTS test page.

BLOCKED: Playwright generate audio -> BLOCKED, Aliyun NLS rejected the request because the provided AccessToken is expired.

GREEN: Playwright save new token/status -> PASS, saved the new AccessToken and refreshed to the new masked status.

BLOCKED: Playwright generate audio with new token -> BLOCKED, Aliyun NLS rejected the request with `Meta:ACCESS_DENIED:The token '****' is invalid!`.
