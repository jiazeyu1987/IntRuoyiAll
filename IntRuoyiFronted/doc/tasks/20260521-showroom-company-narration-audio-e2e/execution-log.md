# Execution Log

BDD: company narration audio generation should succeed on real company data -> Given 真实测试租户可登录 `http://localhost:8081/showroom/company` 且当前公司存在可生成介绍的 live 数据 / When 用户在编辑弹框里先点击 `AI生成介绍` 再点击 `生成语音` / Then 页面必须返回成功结果或明确前置条件错误，不得再出现 `SHOWROOM_AUDIO_GENERATION_FAILED: aliyun_nls_tts_failed status=414`。

BDD: company narration audio verification must use real browser path -> Given 本次任务是 E2E 验证 / When 执行测试 / Then 必须通过 Playwright 驱动真实登录、真实页面和真实接口完成，不得以直接接口调用替代主链路。

RED: 本任务为修复后真实链路复验，未在当前运行时重新制造 `414` 失败；原始失败证据已记录于 companion backend 任务 `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260521-showroom-company-narration-audio-414-fix\execution-log.md` 与前序前端任务 `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-ai-script-error\execution-log.md`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e open http://127.0.0.1:8081/login?redirect=%2Fshowroom%2Fcompany` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-narration-audio-e2e\scripts\verify-showroom-company-narration-audio.mjs` -> PASS，真实测试租户登录后成功打开 `公司信息` 编辑弹框，`AI生成介绍` 和 `生成语音` 两个接口请求都返回成功，脚本内断言确认 `generate-narration-audio` 响应 `code=0` 且中英文 narration 都带有 `audioFileId`，未出现 `414 Request-URI Too Large`。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e snapshot` -> PASS，成功页停留在 `http://127.0.0.1:8081/showroom/company`，页面标题为 `瑛泰管理系统 - 公司信息`，编辑弹框中可见已回填的中英文介绍以及中英文音频播放器。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e eval \"() => Array.from(document.querySelectorAll('textarea')).map((el, index) => ({ index, length: el.value.length, disabled: el.disabled }))\"` -> PASS，当前弹框共有 7 个 textarea；按当前页面结构，语音区最后两个 textarea 长度分别为 `194`（中文介绍）和 `943`（英文介绍），且都处于可编辑状态。

GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-narration-audio-e2e eval \"() => ({ audioElementCount: document.querySelectorAll('audio').length, buttonTexts: Array.from(document.querySelectorAll('button')).map((el) => el.innerText.trim()).filter(Boolean) })\"` -> PASS，当前页共渲染 `4` 个 `audio` 元素（只读区 2 个 + 弹框草稿区 2 个），说明双语音频已生成并挂载到页面。
