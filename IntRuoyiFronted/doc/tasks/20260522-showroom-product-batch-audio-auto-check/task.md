# 任务：展厅产品一键语音定时续跑（前端）

## Goal

在 `showroom/product` 的现有 `一键语音` 入口上补齐“首轮批量执行 + 后端定时续跑状态反馈”前端能力，保持当前筛选条件语义不变，并向用户明确展示：

- 本次批量命中/跳过/成功/失败统计；
- 是否已开启定时检查；
- 何时会自动停止；
- 页面加载后是否仍存在进行中的自动检查批次。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\api\showroom-admin\index.ts`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\components\ProductListTable.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\scripts\showroom-admin-product-list.test.mjs`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\showroom-product-toolbar-layout.spec.js`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-batch-audio-auto-check\**`

## Non-Scope

- 不新增逐行语音按钮。
- 不改变 `一键语音` 的筛选条件语义为“全量已发布产品”。
- 不在前端自行轮询生成语音；续跑由后端定时任务负责。
- 不伪造成功、隐藏真实失败或绕开真实接口状态。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260522-showroom-product-status-column\task.md`
- Status before this task: `Completed on 2026-05-22`
- Impact: 前一任务已完成，不阻塞本次一键语音续跑前端交付。

## Repository Status Check

- Repository: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Current state: 仓库存在多组未提交 showroom / report / NAS 在途改动。
- Impact: 本任务仅允许修改一键语音相关 API、产品列表工具栏、批量结果弹窗、定向测试与本任务文档，不覆盖无关改动。

## Milestones

1. 建立任务文档并锁定前端需要承接的自动检查状态字段与交互位置。
2. 先补 RED，锁定“页面加载读取自动检查状态”“批量结果展示新增跳过统计/自动检查状态”“工具栏保留一键语音入口”的可观察行为。
3. 最小实现前端 API 类型、页面状态读取、工具栏轻量反馈与结果弹窗补充。
4. 跑定向源码回归与类型检查，必要时联动真实页面。
5. 更新证据并执行 closeout preview。

## Expected Verification

- `node --test scripts/showroom-admin-product-list.test.mjs`
- `node tests/e2e/showroom-product-toolbar-layout.spec.js`
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-audio-auto-check --mode preview`

## Current Status

Completed on 2026-05-22.

## Completed Work

- 已扩展 `showroom-admin` 前端 API，补齐批量语音自动检查状态查询契约。
- 已在 `showroom/product` 列表工具栏为 `一键语音` 增加轻量状态反馈标签，显示是否仍在定时检查。
- 已在页面初始化后读取批量语音自动检查状态，并在首轮批量语音完成后刷新该状态。
- 已在批量结果弹窗中补充：
  - 跳过已有语音
  - 跳过缺讲解稿
  - 定时检查状态
  - 剩余待处理数量
- 已更新定向源码测试与工具栏断言，锁定新的状态读取和展示行为。

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-list.test.mjs scripts/showroom-admin-frontend.test.mjs tests/e2e/showroom-product-toolbar-layout.spec.js`
- PASS: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check`
- PASS: `python -X utf8 C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260522-showroom-product-batch-audio-auto-check --mode preview`
