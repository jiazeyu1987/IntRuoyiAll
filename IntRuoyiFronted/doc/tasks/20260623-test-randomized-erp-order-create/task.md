# 任务：MES 生产工单测试用随机 ERP 建单前端

## 任务目标

把生产工单列表现有的“创建ERP订单”前端入口调整为测试语义：明确提示会复制当前工单内容到 ERP，但随机生成编码和 `10~1000` 的数量，并继续展示真实成功/失败结果。

## 当前状态

COMPLETED

## Current Status

COMPLETED

## 上一任务检查

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260622-edhr-batch-row-readiness\task.md`
- 状态：`COMPLETED`
- 处理：上一任务已完成，不阻塞本次生产工单测试功能调整。

## 经验门禁

- 已读取：
  - `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
- 本任务适用强制门禁：
  - 生产工单列表仍沿用 IntPP 紧凑操作台样式，行级操作继续使用轻量 link button，不做无关视觉重构。
  - 前端必须明确暴露“测试建单 + 随机编码/数量”语义，不能继续用原正式文案误导操作者。
  - 前端失败必须直接暴露后端真实错误，不得静默成功。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否
- `是否从根因和长期维护角度解决`：是；在测试范围内显式改文案和交互语义，避免把随机测试行为伪装成正式 ERP 建单。
- `是否存在临时补丁或绕过`：是；这是用户明确要求的测试入口，风险是现有按钮语义会转为测试用途，后续若恢复正式建单需再拆回正式入口或独立按钮。

## BDD 场景

- `BDD: 行操作明确提示测试建单语义 -> Given 用户位于生产工单列表 / When 页面渲染行操作 / Then 按钮和确认文案必须明确说明会复制工单并随机生成编码和数量。`
- `BDD: 成功提示返回真实随机 ERP 单号 -> Given 后端成功创建测试 ERP 订单 / When 前端收到响应 / Then 页面展示后端返回的真实 ERP 单号并刷新列表。`
- `BDD: 后端失败直接暴露 -> Given 后端因配置或金蝶错误返回失败 / When 用户点击测试建单 / Then 前端不吞异常，不显示默认成功。`

## 里程碑

1. M1：建立任务文档与证据文档，确认只改生产工单列表相关文案/API 调用链。
2. M2：补前端 RED 静态契约，锁定测试文案与成功提示。
3. M3：改行操作文案、确认提示和成功提示，保持 loading 与真实错误暴露。
4. M4：运行静态验证并更新证据。

## 预期验证

- `node tests/e2e/workorder-create-erp-order-static.spec.js`
- `pnpm ts:check`

## 完成结果

- 生产工单列表行操作已改成明确测试语义：按钮文案为“测试创建ERP订单”。
- 确认提示明确说明会复制当前工单到 ERP，并随机生成编码与 `10~1000` 的数量。
- 成功提示继续展示后端返回的真实 ERP 单号，错误仍直接暴露。

## 最终核验

- `node tests/e2e/workorder-create-erp-order-static.spec.js` -> PASS
- `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS
