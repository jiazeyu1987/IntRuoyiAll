# 任务：TTS 测试页保存 Aliyun NLS AccessToken

## Task Goal

在 TTS 测试页为阿里云 NLS 增加 AccessToken 配置区，支持输入新的 token 并保存；页面只展示保存状态和脱敏 token，不回显完整 token。

## Milestones

- [x] M1: 记录前端 BDD/TDD 场景和验收点。
- [x] M2: 增加 TTS Token API 封装。
- [x] M3: 增加 Aliyun NLS token 配置区和保存交互。
- [x] M4: 完成类型检查和真实页面保存/脱敏验证。
- [ ] M5: 使用有效阿里云 NLS AccessToken 完成真实音频生成验证并提交。

## Expected Verification

- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 登录 `http://localhost:8081`，进入 `/ai/console/music`，在 `TTS 测试` 页选择 `阿里云 NLS`，保存 AccessToken 后生成音频。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260519-ai-tts-aliyun-nls-token-save/frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260519-ai-tts-aliyun-nls-token-save --mode preview`

## Current Status

Blocked by external credential. 页面 API、Token 配置区、保存交互、脱敏状态展示均已完成；新提供的阿里云 NLS AccessToken 已保存成功，但阿里云返回 invalid，无法完成可播放音频验证。

## Verification Result

- PASS: `node doc\tasks\20260519-ai-tts-aliyun-nls-token-save\scripts\verify-tts-token-ui.mjs`。
- PASS: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`。
- PASS: Playwright 登录 `http://localhost:8081`，进入 `/ai/console/music`，选择 `阿里云 NLS` 后显示已保存和脱敏状态。
- BLOCKED: Playwright 点击生成音频后，上一枚 AccessToken 返回 expired；新提供的 AccessToken 返回 invalid，无法生成可播放音频。
