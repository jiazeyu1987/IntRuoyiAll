# 任务：TTS 测试页支持 Aliyun NLS Provider

## Task Goal

在当前 TTS 测试页增加 `阿里云 NLS` provider 和与 RagInt 一致的 NLS 音色选择，生成请求携带 `provider=aliyun_nls` 和选中 `voice`，生成成功后可通过按钮播放和暂停音频。

## Milestones

- [x] M1: 记录前端 BDD/TDD 场景和验收点。
- [x] M2: 扩展 TTS 请求类型，允许提交 provider 和 voice。
- [x] M3: 增加 provider 单选、NLS 音色下拉和 provider 切换时的音色默认值。
- [x] M4: 修正播放按钮状态，播放成功后显示暂停，暂停后恢复播放。
- [x] M5: 使用真实前端和后端路径完成 Playwright 验证。

## Expected Verification

- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 登录真实前端 `http://localhost:8081`，进入 `/ai/console/music`，切换 `TTS 测试`，选择 `阿里云 NLS`，确认 NLS 音色列表为 `xiaoyun`、`xiaogang`、`ruoxi`、`siqi`。
- Playwright 生成音频，确认请求体为 `{"provider":"aliyun_nls","voice":"xiaoyun"}` 且响应为 `audio/wav`。
- Playwright 点击播放按钮，确认按钮可切换为 `暂停播放`。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-ai-tts-aliyun-nls-provider/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-ai-tts-aliyun-nls-provider --mode preview`

## Current Status

Completed.

## Verification Result

- PASS: 前端类型检查通过。
- PASS: TTS 测试页显示 `阿里云 NLS` provider。
- PASS: NLS 音色下拉显示 `xiaoyun 女声`、`xiaogang 男声`、`ruoxi 女声`、`siqi 女声`。
- PASS: 真实页面生成请求为 `provider=aliyun_nls`、`voice=xiaoyun`，后端返回 WAV 音频。
- PASS: 使用较长文本验证播放按钮可从 `播放音频` 切换到 `暂停播放`。
