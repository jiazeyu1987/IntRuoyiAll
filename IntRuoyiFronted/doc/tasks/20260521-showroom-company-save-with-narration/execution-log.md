# Execution Log: 展厅公司底部保存同时保存语音

BDD: 公司页去掉独立保存语音入口 -> Given 用户在 `http://localhost:8081/showroom/company` 打开“编辑公司信息”弹框并已生成双语语音草稿 / When 页面渲染语音介绍操作区 / Then 页面不应再显示单独的 `保存语音` 按钮，避免同一弹框出现两个保存入口。

BDD: 公司页底部保存统一发布公司内容与语音 -> Given 用户已经在当前弹框里修改公司内容，且中英文语音草稿已生成完成 / When 用户点击右下角 `保存` / Then 前端必须在同一保存链路内完成公司内容保存，并把当前双语语音草稿一并发布为 live，刷新后仍可播放，不得要求额外再点一次 `保存语音`。

RED: `node --test scripts/showroom-admin-company-dashboard-history.test.mjs` -> FAIL，`CompanyWorkbench.vue` 仍保留 `保存语音` 按钮、旧提示文案 `确认无误再保存语音`，且底部 `保存` 仅在公司字段变更时可用，未覆盖“仅语音草稿待保存”的新需求。

GREEN: `node --test scripts/showroom-admin-company-dashboard-history.test.mjs` -> PASS。
GREEN: `pnpm exec eslint src/views/showroom-admin/company/CompanyWorkbench.vue scripts/showroom-admin-company-dashboard-history.test.mjs --format stylish` -> PASS。
GREEN: `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-company-save-with-narration open http://127.0.0.1:8081/showroom/company` + `run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\scripts\verify-showroom-company-save-with-narration.mjs` -> PASS，真实测试租户登录后确认编辑弹框无 `保存语音` 按钮，语音草稿生成后底部 `保存` 从禁用变为可用，点击后命中 `/showroom/company/publish-narration` 并提示 `公司双语语音已保存`。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-company-save-with-narration\frontend-feature-evidence.md` -> PASS，证据文件结构满足前端技能校验规则。
GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-company-save-with-narration --mode preview` -> PASS，预览仅会清理本任务附属证据、截图与一次性 Playwright 脚本，`task.md / execution-log.md` 保留。
