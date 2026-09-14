# 20260731 生产填写页面匹配一线原型

## Task Goal

将真实系统中的 `生产填写` 页面调整为与 `output/frontline-production-operator-1920.html` 和 `output/frontline-production-operator-1920-no-device.html` 两个一线生产报工原型一致：有设备工序展示数量、不良明细和最多 3 台设备参数；无设备工序只展示数量和不良明细，不再显示设备空状态面板。追加本次变更：一线员工不再手填“上工序输入数量”，正式前后端生产模板合同中移除 `PREVIOUS_PROCESS_INPUT_QUANTITY`，记录本和资源池提交链路也不再要求或写入上一工序输入数量。

## Milestones

1. 核对真实组件、目标 HTML 原型、现有模板字段契约和静态合同。
2. 先补充聚焦静态合同，证明当前生产填写页面还未匹配目标原型。
3. 修改 `FrontlineFixedTemplatePanel.vue` 的生产填写 UI、状态和样式，不改接口、后端、DTO 或数据源。
4. 运行聚焦静态合同、相邻合同、TypeScript 检查和可行的页面验证。
5. 更新任务文档、验证报告和本线程讨论记录。
6. 移除 `PREVIOUS_PROCESS_INPUT_QUANTITY` 的前端 API 字段、生产模板默认值、页面提交 payload 和后端模板字段，并补齐前后端合同测试。

## Expected Verification

- `node src\views\mes\pro\feedback\frontline-template-render.spec.cjs`
- `node src\views\mes\pro\feedback\frontline-template-switch.spec.cjs`
- `pnpm ts:check`
- `mvn -pl yudao-module-mes -am "-Dtest=ProductionTemplateContractTest,FrontlineTemplatePayloadContractTest,MesProFrontlineFeedbackPayloadSplitterTest,MesProFrontlineFeedbackSubmitServiceTest,MesProcessPoolSubmitEventServiceAdapterTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
- `python C:\Users\BJB110\.codex\skills\backend-api-delivery\scripts\validate_backend_api.py --evidence doc\tasks\20260731-frontline-production-fill-html-match\backend-api-evidence.md`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc\tasks\20260731-frontline-production-fill-html-match\frontend-feature-evidence.md`
- 可行时用真实页面或静态浏览器截图确认 1920×1080 首屏布局与两个 HTML 原型一致。

## Current Status

ready_for_closeout

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，复用现有生产填写入口、组件状态和正式 payload 构造边界，并从后端模板目录、前端 API 常量、默认值和提交 payload 同步移除不应由员工填写的字段。
- `是否存在临时补丁或绕过`：否。

## Applicable Experience Gates

- 前端静态契约隔离门禁：当前页面复刻必须有任务专用或相邻静态合同先 RED 后 GREEN。
- 前端页面 / 表格 / 样式门禁：只改目标页面组件和样式，不扩大到全局主题或接口。
- 技能证据文件清理前归档门禁：如生成 `frontend-feature-evidence.md`，收尾前必须运行 validator 并复制关键结论到保留报告。
