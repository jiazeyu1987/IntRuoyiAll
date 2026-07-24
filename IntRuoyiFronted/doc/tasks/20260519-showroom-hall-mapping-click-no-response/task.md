# 任务：展厅管理维护映射点击无反应修复

## 目标

修复展厅管理列表中“维护映射”按钮点击后无响应的问题，使用户能够从真实列表行打开映射维护弹窗，并继续编辑展厅产品映射。

## 前置任务检查

- 上一个前端任务：`yudao-ui-admin-vue3/doc/tasks/20260519-test-tenant-login-baseline/task.md`
- 启动前状态：已完成。
- 影响：可独立开展本次展厅管理点击无响应缺陷修复。

## 缺陷摘要

- 用户反馈：展厅管理页签点击“维护映射”没有反应。
- 期望行为：点击任一展厅行的“维护映射”后，应打开映射维护弹窗，展示当前展厅的产品映射数据，并允许保存。

## 里程碑

- [x] M1：检查前置任务并创建任务文档。
- [x] M2：通过真实页面路径和代码链路复现“维护映射”点击无响应。
- [x] M3：补充失败回归测试，记录 RED 证据。
- [x] M4：实现最小修复并完成目标验证。
- [x] M5：补齐 GREEN 证据、更新文档并执行收尾预览。

## 预期验证

- `D:\Programs\node.exe --test scripts/showroom-admin-frontend.test.mjs`
- `D:\Programs\node.exe --test scripts/showroom-admin-hall-list.test.mjs`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-click open http://127.0.0.1:8081`
- `npx.cmd --yes --package @playwright/cli playwright-cli --session showroom-hall-mapping-click run-code --filename D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260519-showroom-hall-mapping-click-no-response\verify-showroom-hall-mapping-click.mjs`

## 当前状态

已完成：代码级修复、目标回归测试、浏览器级验证与收尾预览均已完成。

## 当前结论

- 已确认根因：`HallListTable` 发出 `openMapping` 事件，但 `src/views/showroom-admin/index.vue` 未监听该事件，也未挂接 `HallProductMappingDialog`，因此点击“维护映射”时不会打开任何弹窗。
- 已完成最小修复：后台壳页接入 `@open-mapping="openHallMapping"`，新增 `hallMappingDialogVisible` / `activeHallMappingRecord` 状态，并挂接 `HallProductMappingDialog`，保存后回刷展厅列表。
- 已完成目标回归：新增的映射接线回归测试已通过，展厅列表相关测试通过，针对本次改动的 ESLint 检查通过。
- 已完成真实页面验证：Playwright 使用测试租户认证态进入 `http://127.0.0.1:8081/showroom/hall`，确认“维护映射”按钮可点击且映射弹窗能正常打开，`保存映射` 按钮可见。
- 已完成 closeout preview：预览结果为 `ready`，仅建议保留 `task.md` 与 `execution-log.md`，其余附属验证文件可在后续按需清理。

## 写入边界

- `src/views/showroom-admin/**`
- `scripts/showroom-admin-*.mjs`
- `doc/tasks/20260519-showroom-hall-mapping-click-no-response/**`

## 风险与约束

- 必须基于真实展厅列表与真实点击路径复现，不得用 mock 成功替代。
- 若本地前端入口或真实测试租户不可用，需按 fail-fast 记录阻塞和影响。
- 本次修复只补齐“点击维护映射无反应”的后台接线；若后续需要映射弹窗跨页完整产品池，需另开任务扩展。
