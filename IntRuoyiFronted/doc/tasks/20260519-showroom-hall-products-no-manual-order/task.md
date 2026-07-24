# 任务：展厅管理改为只维护产品集合不手工维护顺序

## 目标

将展厅管理页签中的“维护映射”从“维护产品 + 手工维护 displayOrder”调整为“只维护当前展厅包含哪些产品”，不再要求用户手工维护产品顺序。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-showroom-hall-mapping-click-no-response/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次展厅产品集合维护改造。

## 需求摘要

- 用户要求：展厅管理页签下不用维护产品的顺序，但要维护有哪些产品。
- 业务语义：展厅内产品集合会持续变化，维护入口应以“选哪些产品”为主，不把人工顺序维护作为日常操作负担。

## 里程碑

- [x] M1：检查前置任务并创建任务文档。
- [x] M2：确认当前前后端将产品集合与 `displayOrder` 绑定的范围和影响。
- [x] M3：补充失败回归测试，覆盖“只维护产品集合、不手工维护顺序”的目标行为。
- [x] M4：完成最小实现并通过目标验证。
- [x] M5：更新任务记录并执行收尾预览。

## 预期验证

- `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs`
- `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-no-order open http://127.0.0.1:8081`
- `npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-no-order run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-products-no-manual-order\verify-showroom-hall-products-no-order.mjs`

## 当前状态

已完成：前端维护入口已改为只维护产品集合，真实页面验证通过；后端保存 blocker 已在配套后端任务中修复并通过单测验证。

## 当前结论

- 展厅列表已去掉“排序明细”，用户操作入口由“维护映射”改为“维护产品”。
- 映射弹窗已改为“多选产品集合 + 已选产品列表”，不再暴露手工 `displayOrder` 输入。
- 前端保存时仍走真实 `/showroom/hall/update-product-mapping` 契约，但 `displayOrder` 改为按当前产品集合顺序自动生成，不再要求用户人工维护。
- 真实页面还暴露出两个运行时事实：
  - 仅使用产品管理首屏 20 条产品作为候选会导致部分展厅已有产品无法打开维护弹窗；
  - 当前运行中的旧后端实例保存 hall 产品集合时会因逻辑删除与唯一键冲突而失败。
- 本次已一并处理：
  - 前端弹窗改为主动翻页加载完整产品候选集合；
  - 后端配套任务中已修复 hall 产品映射替换逻辑。

## 最终验证

- PASS：`D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS：`$env:NODE_PATH='D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules'; D:\Programs\node.exe D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js src/views/showroom-admin/components/HallListTable.vue src/views/showroom-admin/components/HallProductMappingDialog.vue src/views/showroom-admin/hall/contracts.ts src/views/showroom-admin/hall/HallWorkbench.vue scripts/showroom-admin-hall-list.test.mjs scripts/showroom-admin-product-hall-operability.test.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-no-order run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-products-no-manual-order\verify-showroom-hall-products-no-order.mjs`
- PASS：`npx.cmd --yes --package @playwright/cli playwright-cli -s=showroom-hall-products-save run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-products-no-manual-order\verify-showroom-hall-products-save.mjs`
- PASS：`python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --task-id 20260519-showroom-hall-products-no-manual-order --mode preview`

## 运行时说明

- 当前 `http://127.0.0.1:8081` 下的前端页面已经验证通过。
- 本次会话已将本机 `48081` 后端切换到 `D:\ProjectPackage\Int\IntRuoyi\output\runtime\backend-20260519-214110.jar`，随后已验证真实“保存产品”成功。

## 写入边界

- `src/views/showroom-admin/**`
- `scripts/showroom-admin-*.mjs`
- `doc/tasks/20260519-showroom-hall-products-no-manual-order/**`

## 风险与约束

- 不得新增 mock/fallback 行为掩盖真实映射保存契约。
- 若后端仍强制要求 `displayOrder`，需要明确记录并采用最小自动生成策略，而不是继续把手工输入暴露给用户。
