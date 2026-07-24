# Task: 展厅接入共享阿里云 NLS 默认 TTS 配置

## Goal

将展厅后端 `generate-audio` 从“缺 adapter 显式失败”改为真实接入共享阿里云 NLS，并在展厅“讲解工作台”提供默认音色、AppKey 与 AccessToken 配置入口，且与“音乐管理 / TTS 测试 / 阿里云 NLS”共用同一套全局保存值。

## Scope

- 共享 AI TTS 配置从“仅保存阿里云 NLS Token”扩展为“保存 Token + 默认音色 + AppKey”。
- 展厅后端新增真实 `ShowroomAudioGenerationAdapter`，调用共享阿里云 NLS，生成音频后写入 `infra` 文件中心并回填 `audioFileId`、`audioDurationSeconds`、`voice`。
- 展厅后台新增讲解工作台配置入口，允许在展厅内读取、保存、刷新共享默认音色、共享 AppKey 与共享 Token。
- 音乐管理阿里云 NLS 测试页同步读取、保存这套共享默认音色、共享 AppKey 与共享 Token。
- 补齐相关后端、前端、集成回归测试与任务证据。

## Non-Scope

- 不新增展厅里的“生成音频”按钮或完整讲解工作流。
- 不引入 Windows、DashScope 或其它 provider 的回退分支。
- 不把共享配置改成租户隔离。
- 不修改无关的展厅审批、预览图、前台播放路由。

## Previous Task Check

- Previous same-repo blocking task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-ai-tts-aliyun-nls-token-save\task.md`
- Status before this task: blocked by external credential.
- Impact: 共享阿里云 NLS Token 的保存/读取代码已完成并通过内部验证，但真实音频生成仍依赖有效外部 Token；本任务继续复用该共享配置能力，并保留真实验收前提。

- Previous showroom task: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260519-showroom-remediation-b5-narration-preview-assets\task.md`
- Status before this task: completed.
- Impact: 当前展厅旁白持久化、读取和前台展示链路已具备，本任务只补共享 NLS 配置与真实音频生成接线。

## Milestones

- [x] M1: 建立任务记录，确认共享 NLS 配置、展厅旁白现状和当前 staged 基线。
- [x] M2: 先补 RED 测试，覆盖共享默认音色、展厅真实 adapter、音频文件落库与显式失败场景。
- [x] M3: 实现共享 AI 配置扩展、展厅真实阿里云 NLS adapter 与文件 ID 落库能力。
- [x] M4: 实现展厅讲解工作台配置页与音乐管理页共享配置同步。
- [x] M5: 跑 GREEN 测试、前端验证、收尾预览并按任务边界提交。

## Expected Verification

- `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest,AliyunNlsTtsSynthesizerTest" test`
- `mvn -pl yudao-module-showroom "-Dtest=ShowroomPersistentNarrationServiceTest,ShowroomNarrationLifecycleTest,ShowroomHttpApiIntegrationTest,ShowroomAudioGenerationContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `pnpm -C yudao-ui-admin-vue3 exec vitest` or targeted frontend/unit command if repo already has narrower coverage for touched showroom/ai views.
- Playwright or browser-driven real-path verification from `http://localhost:8081` for:
  - 音乐管理阿里云 NLS 默认音色/Token 读取与保存
  - 展厅讲解工作台默认音色/Token 读取与保存
- 真实阿里云 NLS 音频生成只在提供有效 AccessToken 时做最终验收；若 token 无效，必须记录外部阻塞，不得伪造成功。

## Current Status

Completed. Shared Aliyun NLS defaults, showroom adapter, file ID persistence helper, music management UI, and showroom narration workbench are implemented and internally verified. The shared config now includes default voice, AppKey, and AccessToken; after saving AppKey into shared config and performing a standard restart, both AI music TTS generation and showroom narration `generate-audio` passed live verification without command-line appkey injection.

## Blockers

- 无。

## Final Verification Result

- PASS: `mvn -pl yudao-module-ai "-Dtest=AiTtsServiceImplTest,AiTtsAliyunNlsCredentialServiceTest" test`
- PASS: `mvn -pl yudao-module-infra "-Dtest=FileServiceImplTest" test`
- PASS: `mvn -pl yudao-module-showroom -am "-Dtest=ShowroomAliyunNlsAudioGenerationAdapterTest,ShowroomPersistentNarrationServiceTest,ShowroomHttpApiIntegrationTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- PASS: `pnpm ts:check` with `NODE_OPTIONS=--max-old-space-size=8192`
- PASS: `pnpm build:local`
- PASS: `mvn -pl yudao-server -am -DskipTests package`
- PASS: `cmd /c restart-ruoyi.bat`
- PASS: authenticated `GET /admin-api/showroom/narration/tts-defaults` on `48081` returned `code=0`
- PASS: authenticated `GET /admin-api/ai/tts-test/aliyun-nls-defaults` on `48081` returned `code=0`
- PASS: `GET http://localhost:8081/showroom/narration-workbench` returned HTTP `200`
- PASS: authenticated real `PUT /admin-api/ai/tts-test/aliyun-nls-appkey` saved shared AppKey and `GET /admin-api/ai/tts-test/aliyun-nls-defaults` showed `appKeySource=saved`
- PASS: authenticated real `GET /admin-api/showroom/narration/tts-defaults` returned the same `appKeySource=saved` and masked AppKey
- PASS: after shared AppKey save, authenticated real `POST /admin-api/ai/tts-test/generate` with `provider=aliyun_nls` returned `audio/wav`
- PASS: after shared AppKey save, authenticated real `POST /admin-api/showroom/narration/generate-audio` returned `code=0` and persisted `audioFileId=2274`, `voice=xiaoyun`, `audioDurationSeconds=22`
- PASS: after standard `restart-ruoyi.bat` with no runtime appkey injection, authenticated real `POST /admin-api/ai/tts-test/generate` still returned `audio/wav`
- PASS: after the same standard restart, authenticated real `POST /admin-api/showroom/narration/generate-audio` still returned `code=0`, persisted `audioFileId=2275`, `voice=xiaoyun`, `audioDurationSeconds=22`
- PASS: latest standard-restart verification still returned shared defaults with `appKeySaved=true`, `appKeySource=saved`, and live generation succeeded again for AI music plus showroom company narration (`audioFileId=2276`, `voice=xiaoyun`, `audioDurationSeconds=28`)

## Cleanup Keep

- `doc/tasks/20260519-showroom-aliyun-nls-shared-tts/backend-api-evidence.md`
- `doc/tasks/20260519-showroom-aliyun-nls-shared-tts/frontend-feature-evidence.md`
