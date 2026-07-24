# Execution Log: 20260519-showroom-aliyun-nls-shared-tts

BDD: 共享阿里云 NLS 默认配置前端读写 -> Given 音乐管理与展厅共享一套阿里云 NLS 默认配置 / When 前端读取或保存默认音色、AppKey、AccessToken / Then 两个入口必须共用同一套接口契约和状态展示，而不是各自维护一套本地值。

BDD: TTS 测试页优先使用共享默认值 -> Given 用户切换到 `阿里云 NLS` / When 页面加载共享配置 / Then 默认音色、AppKey、Token 状态必须来自共享 defaults 接口，保存后刷新展示。

BDD: 本地前端必须能通过 `/admin-api` 访问展厅音频与预览资源 -> Given 展厅讲解工作台与前台播放使用相对 `/admin-api` 资源路径 / When 本地 Vite 环境运行 / Then `/admin-api` 资源请求必须代理到后端，而不是被 SPA fallback 吞掉。

RED: `node --test scripts/ai-tts-default-voice.test.mjs` -> FAIL before implementation, frontend API and pane did not yet expose shared defaults/default-voice/AppKey contracts.

GREEN: `node --test scripts/ai-tts-default-voice.test.mjs` -> PASS.

GREEN: `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS.

GREEN: `pnpm build:local` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-aliyun-nls-shared-tts\frontend-feature-evidence.md` -> PASS.

GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-aliyun-nls-shared-tts --mode preview` -> PASS.
