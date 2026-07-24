# 任务：生产工单补齐 ERP 截图字段（前端）

- Task ID: `20260630-erp-production-order-field-alignment`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在生产工单列表页和详情页补齐 ERP 截图字段展示，保留现有“生产用料清单”关联列和现有页面结构。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - 中文 Vue/测试文件统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持现有密集运营风格，只补列不重设计。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 工单列表展示 ERP 截图字段 -> Given 后端返回 ERP 车间/BOM版本/冲领料/图号/备注1助记码/排产状态 / When 打开生产工单页 / Then 列表可见这些字段且保留生产用料清单关联列。`
- `BDD: 工单详情只读展示 ERP 字段 -> Given 打开工单详情 / When 工单存在 ERP 新字段 / Then 页面只读展示这些字段，不开放人工编辑。`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-field-alignment-static.spec.js`

## Final Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-field-alignment-static.spec.js` -> PASS
- `node scripts/preflight/login-preflight.mjs --base-url http://localhost:8081 --tenant 测试租户 --username aoteman --password 111111 --target-path /mes/pro/work-order --target-text 生产工单` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\output\playwright\20260630-erp-production-order-field-alignment-real-page.mjs` -> PASS
