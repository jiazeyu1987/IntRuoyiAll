# 任务：展厅产品基础/详细信息双语 Tab 与英文语音编辑（前端）

## Goal

在 `showroom/product` 后台管理页中，为产品基础信息弹窗和详细信息弹窗新增 `中文 / English` tab；英文内容走真实后端字段与接口，不做前端临时态 fallback。基础信息英文 tab 需要支持英文名称、各英文描述字段、英文讲解稿编辑、`AI翻译`、`生成语音` 和中英文音频播放器；详细信息英文 tab 需要支持英文高级字段与 `AI翻译`。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ProductDetailDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\contracts.ts`
- 与本任务直接相关的 `scripts\*.test.mjs` / `tests\e2e\*.spec.js`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs\**`

## Non-Scope

- 不改动前台展厅 `Website` 的产品多语言消费。
- 不新增 mock、fallback、隐藏错误或兼容旧临时字段。
- 不改变批量 `一键语音` 的入口位置与业务含义。
- 不顺带重做产品管理页整体布局。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-batch-cover-mode-parallel-cli\task.md`
- Status before this task: `Completed with commit-boundary blocker on 2026-05-21`
- Impact: 上一任务已经显式记录并行脏改动边界；本次继续在 `showroom/product` 同一模块叠加双语编辑能力，不回退上一任务已完成的批量封面模式选择。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在公司双语与批量封面等未提交改动。
- Impact: 本任务只允许修改产品双语 tab、产品讲解稿/语音入口、相关 API 类型、定向测试与本任务文档；不得覆盖无关在途改动。

## Milestones

- [x] M1：创建任务文档并确认上一同仓任务状态。
- [ ] M2：先补 RED，锁定基础信息/详细信息双语 tab、英文 tab `AI翻译 / 生成语音`、列表行去掉单条 `语音` 按钮的可观察行为。
- [ ] M3：完成前端最小实现，接入新后端翻译接口与英文讲解稿编辑链路。
- [ ] M4：运行定向源码测试、TypeScript 检查与真实 Playwright 路径验证。
- [ ] M5：更新前端证据、执行 closeout preview，并准备同仓提交边界。

## Expected Verification

- `node --test scripts/showroom-admin-product-bilingual-tabs.test.mjs`
- `node tests/e2e/showroom-product-publish-entry.spec.js`
- `node tests/e2e/showroom-product-detail-basic-info.spec.js`
- `node tests/e2e/showroom-product-basic-info-narration-move.spec.js`
- `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/product/ProductDetailDialog.vue src/views/showroom-admin/product/contracts.ts scripts/showroom-admin-product-bilingual-tabs.test.mjs --format stylish`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-product-bilingual-tabs run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs\scripts\verify-showroom-product-bilingual-tabs.mjs`
- `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs\frontend-feature-evidence.md`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260521-showroom-product-bilingual-tabs --mode preview`

## Current Status

Completed with backend-dependent real-path blocker on 2026-05-21.

## Assumptions

- 本次只做后台编辑与发布链路，不改前台产品详情消费。
- 新增英文产品字段默认可选，不纳入当前 `incomplete / publish required` 规则。
- 基础信息中没有英文对应值的共享项继续只保留在中文 tab。

## Completed Work

- 已在基础信息弹窗中补齐 `中文 / English` 双语 tab，并把英文名称、英文描述字段、英文讲解稿、`AI翻译`、弹窗内 `生成语音` 与中英文音频播放器接入到真实前端状态。
- 已按后续产品体验要求收口音频展示：`中文音频` 仅显示在中文 tab，`English` tab 只保留 `英文音频`。
- 已在详细信息弹窗中补齐 `中文 / English` 双语高级字段，并增加英文高级字段 `AI翻译`。
- 已移除产品列表单条 `语音` 按钮，保留批量 `一键语音`。
- 已更新定向源码回归、旧兼容断言与前端证据文档。

## Blockers And Impact

- Blocker: 本地 backend showroom 模块当前无法编译到可运行版本，导致本次前端无法做可信的真实 Playwright 联调验证。
- Impact: 已完成源码回归、lint 和 `vue-tsc` 放行；真实页面点击 `AI翻译 / 生成语音` 仍需等待后端编译阻塞解除后再跑。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-bilingual-tabs.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-product-narration-action-disabled.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-company-field-layout.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS: `node tests/e2e/showroom-product-publish-entry.spec.js`
- PASS: `node tests/e2e/showroom-product-detail-basic-info.spec.js`
- PASS: `node tests/e2e/showroom-product-basic-info-narration-move.spec.js`
- PASS: `node tests/e2e/showroom-product-whole-assignment.spec.js`
- PASS: `pnpm exec eslint src/api/showroom-admin/index.ts src/views/showroom-admin/index.vue src/views/showroom-admin/product/ProductDetailDialog.vue src/views/showroom-admin/product/contracts.ts src/views/showroom-admin/components/ProductListTable.vue scripts/showroom-admin-product-bilingual-tabs.test.mjs scripts/showroom-admin-product-narration-editor.test.mjs scripts/showroom-admin-frontend.test.mjs scripts/showroom-admin-product-list.test.mjs scripts/showroom-product-narration-action-disabled.test.mjs --format stylish`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product-bilingual-tabs\frontend-feature-evidence.md`
- NOT RUN: 真实 Playwright 路径验证；当前后端 showroom 模块无法编译到包含本次新接口的运行版本。
