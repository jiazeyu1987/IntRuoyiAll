# 任务：展厅产品讲解稿移入基础信息

## 目标

将展厅产品管理中当前位于“详细信息”弹窗的“讲解稿”内容区移动到“基础信息”编辑弹窗内展示，保持现有真实接口与讲解稿生成能力不变。

## 范围

- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\index.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\src\views\showroom-admin\product\ProductDetailDialog.vue`
- `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\**`
- `D:\ProjectPackage\Int\IntRuoyi\doc\tasks\20260520-showroom-product-narration-move-to-basic-info\**`

## 非范围

- 不改动讲解稿生成接口
- 不改动产品详情其他字段的业务含义
- 不新增 fallback、兼容分支或 mock 数据路径

## 上一任务检查

- 上一任务：`doc/tasks/20260520-showroom-product-detail-basic-info-cleanup/task.md`
- 状态：`completed`
- 说明：上一任务已完成且已记录真实页面阻塞，本次可继续处理产品详情弹窗结构调整。

## 里程碑

- [x] M1：确认当前组件结构、讲解稿区位置与本次验收边界，并记录 BDD 场景。
- [x] M2：先补失败测试，锁定“讲解稿移入基础信息”的可观察行为。
- [x] M3：完成详情弹窗最小结构调整。
- [x] M4：执行前端测试与必要校验，记录 RED/GREEN 证据。
- [x] M5：更新任务文档、执行日志与证据文档。

## 预期验证

- 测试可证明“讲解稿”区域在“基础信息”编辑弹窗内渲染。
- 测试可证明“详细信息”弹窗不再渲染“讲解稿”区域与“生成讲解稿”按钮。
- 页面保留原有讲解稿编辑文本域与“生成讲解稿”按钮。
- 不引入新的 UI fallback 或静默降级。

## 当前状态

Completed.

## 完成结果

- 已将讲解稿文本域与“生成讲解稿”按钮迁移到 `index.vue` 的产品基础信息编辑弹窗。
- 已在基础信息弹窗接入真实讲解稿读取、保存草稿与生成脚本接口。
- 已从 `ProductDetailDialog.vue` 移除讲解稿 UI、读取逻辑与生成按钮，详细信息弹窗仅保留高级产品字段。
- 已补充并更新源码回归测试，分别锁定基础信息入口承接讲解稿、详细信息入口不再保留讲解稿。

## 最终验证结果

- PASS：`node .\tests\e2e\showroom-product-basic-info-narration-move.spec.js`
- PASS：`node .\tests\e2e\showroom-product-detail-basic-info.spec.js`
- PASS：`pnpm exec eslint src\views\showroom-admin\index.vue src\views\showroom-admin\product\ProductDetailDialog.vue tests\e2e\showroom-product-basic-info-narration-move.spec.js tests\e2e\showroom-product-detail-basic-info.spec.js`
- NOT RUN：真实页面 Playwright 路径本次未重新执行；当前工作区此前已记录共享测试租户缺少归属部门，会在审批路线预解析阶段阻塞产品基础信息弹窗打开

## Cleanup Keep

- `doc/tasks/20260520-showroom-product-narration-move-to-basic-info/frontend-feature-evidence.md`
