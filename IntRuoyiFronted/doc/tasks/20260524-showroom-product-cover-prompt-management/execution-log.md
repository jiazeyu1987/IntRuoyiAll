# 执行日志：展柜产品封面提示管理页签

BDD: 展柜后台新增平级提示管理页签 -> Given 用户进入展柜后台 / When 查看顶层页签路由 / Then 应出现位于公司版本、产品管理、讲解工作台同层的 `提示管理` 页签，并进入独立工作台。

BDD: 当前版本与历史版本同时可见 -> Given PRODUCT_COVER 已存在当前提示词版本和历史版本 / When 用户打开提示管理页 / Then 页面必须同时展示当前生效版本摘要、可编辑保存区和历史版本列表。

BDD: 保存新版本后立即刷新当前版本 -> Given 用户修改模板文本并填写版本说明 / When 保存成功 / Then 页面必须刷新当前版本卡片和历史列表，显示新的版本号与时间。

BDD: 历史版本详情只读查看 -> Given 历史列表中存在旧版本 / When 用户点击查看内容 / Then 页面必须展示完整模板内容和版本元数据，但不出现直接生成按钮。

BDD: 非法模板输入必须显式失败 -> Given 用户提交空模板、未知占位符或缺少产品名占位符的模板 / When 点击保存 / Then 页面必须展示明确错误，不得伪造保存成功。

RED: `vite` 开发态实时报错 -> FAIL，`[plugin:vite:vue] Error parsing JavaScript expression: Unexpected token, expected "}"` 指向 `src/views/showroom-admin/prompt/PromptWorkbench.vue` 中原始写法 `{{ \`{{${placeholderCode}}}\` }}`，说明模板内直接嵌套 moustache 文本会让 Vue SFC 表达式解析失败。

GREEN: 将占位符展示改为 `formatPlaceholder(placeholderCode)` 后，`node --test scripts/showroom-admin-prompt-management.test.mjs` -> PASS。

GREEN: `pnpm exec eslint src/router/modules/showroom.ts src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/prompt/PromptWorkbench.vue scripts/showroom-admin-prompt-management.test.mjs` -> PASS。

GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS。

GREEN: `node --test scripts/showroom-admin-prompt-management.test.mjs` -> PASS，当前工作区下的提示管理路由、shell section 与工作台模板结构校验通过。

GREEN: `pnpm exec eslint src/router/modules/showroom.ts src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/prompt/PromptWorkbench.vue scripts/showroom-admin-prompt-management.test.mjs` -> PASS，提示管理新增前端代码 lint 通过。

GREEN: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260524-showroom-product-cover-prompt-management --mode preview` -> PASS，预览结果仅保留 `task.md` 与 `execution-log.md`，未发现 blocked 项。

RED: `playwright-cli --session showroom-prompt-management run-code ...verify-showroom-prompt-management.mjs` 首轮失败 -> FAIL，真实前端已切到新页签源码，但本地 48081 仍是旧运行时，`/admin-api/showroom/prompt/current` 返回 `No static resource admin-api/showroom/prompt/current.`。

GREEN: 本地运行库补 migration 并重启后，真实前端提示管理页保存 -> PASS，当前版本已更新到 `V3`，`changeNote=playwright verify 1779559450331`。

BLOCKER: 真实单图 `AI生成` 响应等待 -> FAIL，Playwright 在等待 `/admin-api/showroom/product/generate-cover-image` 响应 120 秒后超时；失败截图位于 `output/playwright/showroom-prompt-management-red.png`。

INFO: 后端运行日志 `output/runtime/backend-20260524-020047.out.log` 显示上述真实请求最终完成，耗时约 `764819 ms` 与 `1054806 ms`；当前 prompt 版本 `useCount=5`，说明真实生成链路已命中新版本 prompt。

INFO: 用户于 2026-05-24 明确要求提交当前前后端代码；本次前端提交边界锁定为 `showroom prompt management` 相关源码、脚本测试与任务文档，同仓 `showroom-frontstage-removal`、分页排查等未完成任务文件继续留在工作区。
