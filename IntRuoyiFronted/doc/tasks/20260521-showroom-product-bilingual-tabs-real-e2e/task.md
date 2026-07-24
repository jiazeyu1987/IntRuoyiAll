# 任务：展厅产品双语 Tab 真实数据 E2E 验证

## Goal

使用真实测试租户、真实前端页面 `http://localhost:8081/showroom/product`、真实后端接口与真实产品数据，对“产品基础/详细信息双语 Tab 与英文语音编辑”做一次完整 E2E 验证，确认：

- 基础信息弹窗真实渲染 `中文 / English` 两个 tab；
- English tab 真实存在英文名称、英文描述字段、英文讲解稿、`AI翻译`、`生成语音` 与中英文音频播放器；
- 详细信息弹窗真实渲染 `中文 / English` 两个 tab，且 English tab 真实存在英文高级字段和 `AI翻译`；
- 产品列表真实不再显示单条 `语音` 按钮；
- `AI翻译` 与 `生成语音` 走真实接口；若当前运行实例未包含后端新契约，必须 fail-fast 暴露真实阻塞点。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs-real-e2e\**`
- Playwright 真实浏览器回放
- 必要的真实接口核对

## Non-Scope

- 不修改业务逻辑，除非 E2E 直接证明当前运行实例未加载本次前后端代码且存在可立即修复的前端运行态问题。
- 不新增测试专用前端控件或 mock 数据。
- 不处理与本次产品双语 E2E 无关的在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs\task.md`
- Status before this task: `Completed with backend-dependent real-path blocker on 2026-05-21`
- Impact: 前端源码级改动已完成；本次专门验证真实运行时是否已经具备可点击验收条件，并复核此前记录的 backend blocker 是否仍存在。

## Milestones

1. 创建 E2E 任务记录并确认 `8081` 前端入口、`48081` 后端接口可访问。
2. 用 Playwright 真实登录 `测试租户 / aoteman / admin123`，进入 `showroom/product`。
3. 验证列表中无单条 `语音` 按钮，并定位真实可操作产品。
4. 打开基础信息与详细信息弹窗，验证双语 tab 和英文编辑区；尽可能执行 `AI翻译` / `生成语音` 真实点击。
5. 写入验证结果并按任务收尾规范预览 cleanup。

## Expected Verification

- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-bilingual-tabs-real-e2e run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs-real-e2e\scripts\verify-showroom-product-bilingual-tabs-real-e2e.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-bilingual-tabs-real-e2e --mode preview`

## Current Status

Completed with backend runtime blocker on 2026-05-21.

## Blockers And Impact

- Blocker:
  - `/admin-api/showroom/product/translate-fields-to-en`
- 具体表现：
  - 真实点击 `AI翻译` 后，当前运行实例返回 `code=500`，消息为 `No static resource admin-api/showroom/product/translate-fields-to-en.`
- Impact:
  - 产品双语 tab、详细信息 English tab 和 `生成语音` 已能做真实验证。
  - `AI翻译` 的真实成功链路仍需等待后端运行实例加载本次新接口。

## Completed Work

- 已修复当前工作区 `CompanyWorkbench.vue` 的模板解析错误，消除了最初阻断 `showroom-admin` 路由加载的 Vite overlay。
- 已用真实租户进入 `http://localhost:8081/showroom/product`。
- 已验证列表不再显示单条 `语音` 按钮。
- 已验证基础信息与详细信息弹窗都真实渲染 `中文 / English` tab。
- 已验证 `生成语音` 真实返回 `code=0`，并在基础信息 English tab 中挂载出 2 个音频播放器。

## Final Verification Result

- PASS: 真实产品页加载成功且首屏存在真实产品列表。
- PASS: 基础信息与详细信息双语 tab 真实可见。
- PASS: 真实 `生成语音` 返回 `code=0`，且 `audioCount=2`。
- FAIL: 真实 `AI翻译` 返回 `No static resource admin-api/showroom/product/translate-fields-to-en.`
- PASS: `verification-report.md`
