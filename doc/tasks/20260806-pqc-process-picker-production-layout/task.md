# 20260806-pqc-process-picker-production-layout

## Task Goal

将一线 PQC 点击“工序”后的弹框大小、整体布局、子卡片大小与一线生产点击“工序”的弹框保持一致；只改前端展示层和静态契约，不改变订单、工序、员工的数据来源和接口契约。

## Milestones

- [x] M1: 建立 BDD/TDD 验收口径与任务专用静态合同。
- [x] M2: 对齐 PQC 工序弹框容器、标题、选项网格、子卡片和返回按钮布局。
- [x] M3: 运行目标静态合同、相邻工序弹框/一线填写合同和格式检查。
- [x] M4: 更新验证证据与任务状态。

## Expected Verification

- `node tests/e2e/mes-frontline-pqc-process-picker-production-layout-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-order-picker-production-layout-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-fullscreen-toggle-static.spec.cjs`
- `node tests/e2e/edhr-frontline-production-pixel-parity-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-login-employee-lock-static.spec.cjs`
- `node tests/e2e/mes-frontline-pqc-active-order-switching-static.spec.js`
- `node tests/e2e/edhr-frontline-fill-tabs-static.spec.cjs`
- `git diff --check`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260806-pqc-process-picker-production-layout/frontend-feature-evidence.md`

## Current Status

ready_for_closeout

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。本任务只统一前端样式，不新增降级分支。
- 是否从根因和长期维护角度解决：是。通过 PQC 工序专用布局类复用生产弹框尺寸 token，而不是硬编码截图特例或改变数据链路。
- 是否存在临时补丁或绕过：否。

## Boundary

- Allowed: `FrontlineFixedTemplatePanel.vue` 中 PQC 工序弹框模板与样式、任务专用静态合同、相邻订单弹框合同兼容调整、任务文档。
- Protected: 前后端 API、DTO/schema、后端服务、订单/工序/员工数据来源、测试 fixture 和业务数据。

## Experience Gate

- `docs/frontend-development.md#前端静态契约隔离门禁`：新增任务专用最小静态合同，先 RED 后 GREEN，避免被同文件其它弹框或既有大合同误伤。
- `docs/frontend-development.md#前端截图样式块静态契约门禁`：静态合同必须锁定目标选择器和状态块，不能用跨整文件宽正则判断弹框样式。
- `docs/frontend-development.md#前端选择弹框即时反馈门禁`：本次只调整工序 picker 布局，不改变 PQC 校验成功后关闭和生产模式即时关闭的既有流程。
- `docs/e2e-rules.md#Windows 换行与脚本行为同步`：静态合同读取 Vue SFC 时归一化 CRLF，并用稳定 class/data 边界定位弹框块。
