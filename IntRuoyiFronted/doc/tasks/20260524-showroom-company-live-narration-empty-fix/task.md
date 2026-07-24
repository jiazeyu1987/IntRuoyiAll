# 任务：修复公司信息页当前版本语音空白

## 任务目标

- 修复 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3` 中 `showroom/company` 页面出现“当前版本显示为 V8，但语音区为空”的回归。
- 保证公司信息页的 live narration 文本和音频与当前运行时真实公开语音一致，不再把错误的前台 API 返回结构当成 `{ text, audioUrl }` 使用。

## 非目标

- 不修改 `ruoyi-vue-pro` 后端接口合同。
- 不修改公司版本页签、版本中心页或历史 bundle 逻辑。
- 不引入 mock narration、fallback 字段或静默吞错。

## 前序任务检查

- 已检查上一任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-version-audio-preview\task.md`
- 上一任务状态：`已完成`
- 影响：上一任务已完成，不阻塞本次公司信息页 live narration 空白修复。

## 里程碑

- [x] M1：建立任务记录并复现“公司信息页版本号正常但语音区空白”的当前行为。
- [x] M2：先补 RED 测试，锁定 `getDisplayNarration` 消费了错误返回结构。
- [x] M3：最小修复前端 live narration 加载链路。
- [x] M4：完成定向测试、真实页面验证与任务证据更新。

## 预期验证

- `node --test scripts/showroom-admin-company-live-narration.test.mjs`
- `pnpm exec eslint src/api/showroom-frontstage/index.ts src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-live-narration.test.mjs`
- 真实验证：`http://localhost:8081/showroom/company`

## 当前状态

- 状态：已完成

## Completed Work

- 已确认根因不在后端语音缺失，而在前端 API wrapper：
  - `CompanyWorkbench.vue` 把 `ShowroomFrontstageApi.getDisplayNarration(...)` 当作 `{ text, audioUrl }` narration payload 使用。
  - 但原来的 `ShowroomFrontstageApi.getDisplayNarration` 实际返回的是 `website-config` 上下文对象，导致 `liveNarration.zhText / zhAudioUrl / enText / enAudioUrl` 全部变空。
- 已把 `src/api/showroom-frontstage/index.ts` 中的 `getDisplayNarration` 改为真正请求 `/showroom/display/narration`。
- 已保留 `CompanyWorkbench.vue` 现有消费方式，因此公司信息页无需改动业务结构即可恢复 live narration 显示。

## Verification Evidence

- `node --test scripts/showroom-admin-company-live-narration.test.mjs` -> RED 后 GREEN
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src/api/showroom-frontstage/index.ts src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-live-narration.test.mjs` -> PASS
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-live-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-live-narration-empty-fix\scripts\verify-showroom-company-live-narration.mjs` -> PASS
