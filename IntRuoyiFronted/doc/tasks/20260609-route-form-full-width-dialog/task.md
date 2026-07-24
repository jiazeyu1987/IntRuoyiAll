# 工艺路线弹框满屏宽度调整

## 任务目标

将 MES 工艺路线新增、编辑、启用、详情弹框从固定 `1320px` 宽度调整为接近视口满宽，减少组成工序表格中工作站、资源产能等列被压缩或需要频繁横向滚动的问题。

## Previous Task Check

- 前序同仓库相关任务：`doc/tasks/20260609-route-structured-scheduling-resource-implementation/task.md`。
- 检查结果：该任务已标记 `completed`，本任务只在其前端工艺路线弹框展示基础上调整宽度，不改接口、资源计算或保存逻辑。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。本任务只调整弹框布局，不新增兜底逻辑。
- `是否从根因和长期维护角度解决`：是。将工艺路线主弹框宽度改为视口约束，适配数据密集表格长期扩展。
- `是否存在临时补丁或绕过`：否。不通过隐藏列、删除列或压缩业务信息绕过宽度问题。

## BDD 场景

- BDD: 工艺路线弹框接近满屏宽度 -> Given 用户打开工艺路线新增、编辑或详情弹框 / When 页面渲染弹框 / Then 弹框宽度使用视口宽度计算，不再固定为 `1320px`。
- BDD: 组成工序表格列不因弹框固定宽度被过度压缩 -> Given 工艺路线弹框中显示组成工序表格 / When 用户查看工作站和资源列 / Then 弹框提供接近满屏的横向空间，表格仍保留既有列和操作。

## 里程碑

- [x] M1：添加静态契约 RED 测试，确认 `RouteForm.vue` 不再使用固定 `1320px`。
- [x] M2：调整 `RouteForm.vue` 弹框宽度为视口满宽约束。
- [x] M3：运行静态契约测试、类型检查和真实页面宽度验证。

## 预期验证

- `node tests\e2e\mes-route-form-full-width-dialog.spec.js`
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json`

## 当前状态

completed

## 完成记录

- `RouteForm.vue` 主弹框从固定 `1320px` 调整为 `calc(100vw - 32px)`。
- 弹框左右保留 16px 视口边距，组成工序表格获得接近满屏的展示空间。
- 未改工艺路线接口、资源计算、资源保存或表格列数据。

## 最终验证

- `node tests\e2e\mes-route-form-full-width-dialog.spec.js` -> PASS。
- `node --max-old-space-size=8192 node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- Playwright 真实页面验证 `http://127.0.0.1:8081/mes/pro/route?openId=900026` -> PASS，1440px 视口下弹框宽度为 `1408px`。

## Cleanup Keep

- `doc/tasks/20260609-route-form-full-width-dialog/frontend-feature-evidence.md`
