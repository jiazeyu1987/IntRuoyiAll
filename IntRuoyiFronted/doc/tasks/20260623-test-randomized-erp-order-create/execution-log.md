# Execution Log: MES 生产工单测试用随机 ERP 建单前端

## BDD 场景

BDD: 行操作明确提示测试建单语义 -> Given 用户位于生产工单列表 / When 页面渲染行操作 / Then 按钮和确认文案必须明确说明会复制工单并随机生成编码和数量。

BDD: 成功提示返回真实随机 ERP 单号 -> Given 后端成功创建测试 ERP 订单 / When 前端收到响应 / Then 页面展示后端返回的真实 ERP 单号并刷新列表。

BDD: 后端失败直接暴露 -> Given 后端因配置或金蝶错误返回失败 / When 用户点击测试建单 / Then 前端不吞异常，不显示默认成功。

## 执行证据

- 2026-06-23：创建任务并读取 `FRONTEND_STYLE.md`；本轮只改现有生产工单列表行操作文案与提示，不做无关页面改版。
- RED: `node tests/e2e/workorder-create-erp-order-static.spec.js` -> FAIL，生产工单列表仍显示旧文案“创建ERP订单”，且确认/成功提示没有明确“随机生成编码和数量”的测试语义。
- GREEN: `node tests/e2e/workorder-create-erp-order-static.spec.js` -> PASS，按钮、确认提示、成功提示都已切换为测试复制建单语义。
- GREEN: `$env:NODE_OPTIONS='--max-old-space-size=8192'; pnpm ts:check` -> PASS，前端类型检查通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence yudao-ui-admin-vue3\doc\tasks\20260623-test-randomized-erp-order-create\frontend-feature-evidence.md` -> PASS，前端证据文档格式通过。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260623-test-randomized-erp-order-create --mode preview` -> PASS，预览仅建议删除中间证据文档。
- GREEN: `python C:\Users\BJB110\.codex\skills\task-closeout-cleanup\scripts\task_closeout.py --task-id 20260623-test-randomized-erp-order-create --mode apply` -> PASS，已删除 `frontend-feature-evidence.md`，保留 `task.md` 与 `execution-log.md`。
