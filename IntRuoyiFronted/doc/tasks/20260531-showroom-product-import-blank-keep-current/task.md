# 任务：展厅产品导入空单元格保留当前值（前端）

## 任务目标

同步更新展厅产品 Excel 导入弹窗提示文案，使用户知道空白单元格会保留当前数据，不再提示空白文字单元格会清空字段。

## 前序任务检查

- 已检查 `doc/tasks/20260531-showroom-hall-product-namecn-contract/task.md`，状态为 completed，不阻塞本任务。
- 当前前端仓库存在无关改动，本任务不触碰、不提交这些改动。

## 里程碑

- [x] M1：建立任务文档、BDD 场景、预期验证和证据文件。
- [x] M2：更新导入弹窗提示文案。
- [x] M3：运行前端类型检查并记录 GREEN 证据。
- [x] M4：收尾清理预览并提交本任务直接相关改动。

## BDD 场景

- BDD: 导入提示说明空值保留 -> Given 用户打开展厅产品管理导入弹窗 / When 查看导入说明 / Then 页面提示空白单元格保留当前数据。
- BDD: 导入提示不再说明清空字段 -> Given 用户打开展厅产品管理导入弹窗 / When 查看导入说明 / Then 页面不再提示空白文字单元格会清空对应字段。

## 预期验证

- GREEN：`pnpm ts:check` 通过。

## 当前状态

status: completed

已完成导入弹窗提示文案更新、类型检查、真实前端 E2E 和收尾清理预览。

## 验证结果

- GREEN：`pnpm ts:check` 通过。
- E2E：测试租户 `测试租户/aoteman` 通过 `http://127.0.0.1:8081/showroom/product` 打开导入弹窗，页面显示“空白单元格会保留当前数据”；导入只填写 `展品编码=product_001` 的 xlsx 后返回 `跳过无变化：1`，导入后列表中文名、版本号和封面保持不变。
- CLEANUP PREVIEW：`task_closeout.py --task-id 20260531-showroom-product-import-blank-keep-current --mode preview --workspace D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3 --worktree-closeout off --json` 通过，`delete=[]`，`blocked=[]`。

## Cleanup Keep

- `doc/tasks/20260531-showroom-product-import-blank-keep-current/frontend-feature-evidence.md`
