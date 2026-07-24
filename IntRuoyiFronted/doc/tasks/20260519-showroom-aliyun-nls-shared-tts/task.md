# 任务：前端接入共享阿里云 NLS 默认配置

## 目标

让前端的 `TTS 测试` 与展厅 `讲解工作台` 共同读写一套共享阿里云 NLS 默认配置，包含默认音色、AppKey 与 AccessToken，并让本地前端通过 `/admin-api` 代理访问这些后端接口与展厅音频/预览资源。

## 前置任务检查

- 前一个同仓库任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-dcc-position-uploader-derived-roles\task.md`
- 启动前状态：已完成。
- 关联后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\task.md`

## 范围

- `src/api/ai/tts/index.ts`
- `src/views/ai/music/manager/TtsTestPane.vue`
- `vite.config.ts`
- `scripts/ai-tts-default-voice.test.mjs`
- `doc/tasks/20260519-showroom-aliyun-nls-shared-tts/**`

## 里程碑

- [x] M1：记录共享 NLS 默认配置的前端 BDD/验证边界。
- [x] M2：扩展前端 API 契约，补齐默认音色 / AppKey / Token 读写接口。
- [x] M3：更新 `TtsTestPane`，支持共享默认音色、AppKey 与 Token 的读取和保存。
- [x] M4：补齐静态回归、类型检查与构建验证。
- [x] M5：执行 closeout 预览并准备本任务提交。

## 预期验证

- `node --test scripts/ai-tts-default-voice.test.mjs`
- `node --max-old-space-size=8192 node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- `pnpm build:local`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\frontend-feature-evidence.md`
- `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-aliyun-nls-shared-tts --mode preview`

## 当前状态

已完成：共享默认音色 / AppKey / Token 的前端契约、TTS 测试页 UI 和本地 `/admin-api` 代理均已完成并通过验证。

## Current Status

completed

## 验证结果

- PASS: `node --test scripts/ai-tts-default-voice.test.mjs`
- PASS: `node --max-old-space-size=8192 node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- PASS: `pnpm build:local`
- PASS: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\frontend-feature-evidence.md`
- PASS: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-aliyun-nls-shared-tts --mode preview`

## 风险与约束

- 前端不伪造阿里云可用性；真实音频生成是否成功仍依赖后端运行时凭证。
- `vite.config.ts` 中的 `/admin-api` 代理只用于本地开发验证，不应绕开后端真实权限与错误语义。
