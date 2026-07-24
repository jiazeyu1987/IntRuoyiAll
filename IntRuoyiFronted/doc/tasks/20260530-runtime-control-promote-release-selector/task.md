# 任务：运行控制台上线发布包选择器

## 任务目标

让运行控制台“上线已验证发布包”动作与“部署发布包到测试服”一致使用 NAS 发布包选择器，并在选择列表中显示该发布包是否为“当前测试服”或“曾部署测试服”，同时为该动作补齐默认原因“默认发布”。

## 前置任务检查

- `20260529-runtime-control-all-buttons-int-main-merge` 状态为 `completed`。
- `20260529-runtime-control-build-release-defaults` 状态为 `completed`。

## BDD 场景

- BDD: 上线已验证发布包使用发布包选择器 -> Given 运维人员打开“上线已验证发布包”弹窗 / When 页面渲染发布包字段 / Then 字段必须使用与“部署发布包到测试服”相同的 NAS 发布包下拉选择器，而不是普通输入框。
- BDD: 上线已验证发布包默认原因明确 -> Given 运维人员打开“上线已验证发布包”弹窗 / When 弹窗初始化 / Then 原因字段默认值必须为“默认发布”。
- BDD: 发布包列表显示测试服使用状态 -> Given 页面已经加载当前测试服运行状态和历史成功的测试服部署记录 / When 运维人员展开发布包选择器 / Then 当前测试服正在使用的发布包应显示“当前测试服”，历史曾成功部署到测试服的发布包应显示“曾部署测试服”。

## 里程碑

- [x] M1：建立任务记录并确认前置任务完成。
- [x] M2：补充前端静态合同测试并记录 RED。
- [x] M3：确认页面实现满足新合同。
- [x] M4：运行静态测试、类型检查和真实页面验证。
- [x] M5：更新执行记录并完成提交。

## 预期验证

- `node tests\\e2e\\runtime-control-release-package-static.spec.js`
- `node node_modules\\vue-tsc\\bin\\vue-tsc.js --noEmit -p tsconfig.relaxed.json`
- Playwright 真实页面验证：`http://127.0.0.1:8081/infra/monitors/runtime-control`

## Current Status

Completed.

## 实现说明

- “上线已验证发布包”与“部署发布包到测试服”共用 NAS 发布包下拉选择器。
- “上线已验证发布包”默认原因设置为“默认发布”。
- 发布包候选根据测试服状态和成功测试服部署历史显示“当前测试服”或“曾部署测试服”。
- 当前测试服发布包优先显示绿色；曾部署测试服的非当前发布包显示黄色。

## 最终验证

- `node tests\e2e\runtime-control-release-package-static.spec.js` -> PASS。
- `node node_modules\vue-tsc\bin\vue-tsc.js --noEmit -p tsconfig.relaxed.json` -> PASS。
- Playwright 真实页面验证 `http://127.0.0.1:8081/infra/monitors/runtime-control` -> PASS，“上线已验证发布包”弹窗使用发布包选择器且默认原因为“默认发布”。
