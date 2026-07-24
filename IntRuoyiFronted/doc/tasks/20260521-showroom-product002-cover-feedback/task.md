# Task: product_002 封面生成反馈修复

## Goal

修复 `product_002` 在产品基础弹窗里点击 `AI生成` 时“没有反应”的问题，并保证：

- 若当前产品不允许生成封面，必须明确提示原因；
- 若真实请求成功，必须明确提示成功；
- 若真实请求失败，必须明确提示失败原因；
- 用户不能处于“点了按钮但没有任何反馈”的状态。

## Scope

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- 必要的前端定向回归测试
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-product002-cover-feedback\**`

## Non-Scope

- 不修改封面生成后端业务逻辑，除非复现证明前端调用路径根本没发出请求且根因在接口契约。
- 不处理无关在途改动。

## Previous Task Check

- Previous same-repo task record: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260521-showroom-cover-e2e-rerun\task.md`
- Status before this task: `Completed on 2026-05-21`
- Impact: 封面 E2E 已完成，本次继续收口 `product_002` 的用户反馈体验问题。

## Milestones

1. 创建任务文档并用真实数据复现 `product_002` 的点击路径。
2. 先补 RED，锁定“不管成功失败都必须给出反馈”的前端契约。
3. 最小修复前端按钮反馈逻辑。
4. 跑通源码回归与真实页面复验。

## Current Status

- Status: Completed
- Completed work:
  - 已用真实数据复现 `product_002` 点击 `AI生成` 时无请求、无提示的用户体感问题。
  - 已补齐点击即提示“AI封面生成中，请稍候”，并在生成中重复点击时提示“AI封面仍在生成中，请稍候”。
  - 已用真实页面复验 `product_002` 点击后能即时收到提示。
- Remaining blockers:
  - None.

## Final Verification Result

- PASS: `node --test scripts/showroom-admin-product-cover-field.test.mjs`
- PASS: 真实 Playwright 复验 `product_002` 点击 `AI生成` 后，页面出现 `AI封面生成中，请稍候`
