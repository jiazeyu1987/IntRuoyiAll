# 执行日志：修复公司信息页当前版本语音空白

BDD: 公司信息页当前版本应展示真实 live narration -> Given 公司信息页当前版本显示为已发布 V8 / When 页面加载当前公司信息与 live narration / Then 语音介绍文本和中英文播放器都必须显示当前真实公开语音，而不是空白或未生成。
RED: `node --test scripts/showroom-admin-company-live-narration.test.mjs` -> FAIL，`ShowroomFrontstageApi.getDisplayNarration` 仍把 `website-config` 上下文对象当成 narration payload 返回，没有真正请求 `/showroom/display/narration`。
GREEN: `node --test scripts/showroom-admin-company-live-narration.test.mjs` -> PASS，`getDisplayNarration` 已改为直连 `/showroom/display/narration`，公司工作台继续消费 `text / audioUrl`。
GREEN: `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\.pnpm\eslint@8.57.1\node_modules\eslint\bin\eslint.js src/api/showroom-frontstage/index.ts src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-live-narration.test.mjs` -> PASS。
INFO: 运行时接口复核 -> PASS，`GET /showroom/display/narration?targetType=COMPANY&targetId=1&audienceType=PUBLIC&language=ZH` 返回真实 `text / audioUrl`，证明后端 live narration 已存在。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-live-narration run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260524-showroom-company-live-narration-empty-fix\scripts\verify-showroom-company-live-narration.mjs` -> PASS，真实 `http://127.0.0.1:8081/showroom/company` 页面加载后，版本号仍显示 `V8`，中文和 English 两个 tab 的 narration 文本与音频播放器都与 `/showroom/display/narration` 接口返回一致，不再显示“未生成”空状态。
