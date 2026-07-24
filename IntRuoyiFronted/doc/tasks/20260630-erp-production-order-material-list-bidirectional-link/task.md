# 任务：生产订单与生产用料清单双向关联展示（前端）

- Task ID: `20260630-erp-production-order-material-list-bidirectional-link`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

在前端生产工单页与 ERP 生产用料清单页中展示正式双向关联，并提供列表内跳转/查看入口；保持 IntPP 风格的紧凑运营界面，不引入无关重设计。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-test-server-zhaojie-replan-preview-permission-fix\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，可开始本次新任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `docs\powershell-memory.md`、`docs\login-access.md`、`D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Vue/TS/Markdown 统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 若做真实页面复验，必须先走 `login-preflight.mjs`。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`
  - 保持生产订单列表式密集工具栏 + 表格壳 + 链接式主键风格。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。前端只消费后端正式关联字段，不自行拼接推断。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 生产工单页显示生产用料清单入口 -> Given 某生产工单存在关联生产用料清单 / When 打开生产工单页 / Then 行内显示生产用料清单链接或摘要，点击后跳到生产用料清单页。`
- `BDD: 生产工单页对无关联数据明确留空 -> Given 某生产工单没有关联生产用料清单 / When 打开生产工单页 / Then 行内显示无或留空，不渲染错误链接。`
- `BDD: 生产用料清单页显示对应生产工单 -> Given 某生产用料清单存在关联生产工单 / When 打开生产用料清单主表或明细 / Then 页面显示生产工单编号链接，点击后跳到生产工单页并打开目标工单。`
- `BDD: 生产用料清单未映射时不伪造跳转 -> Given 某生产用料清单没有对应生产工单 / When 打开页面 / Then 页面显示无对应生产订单，不渲染假链接。`

## Milestones

1. M1：建立前端任务文档并锁定页面边界。`completed`
2. M2：补 RED 静态合同测试。`completed`
3. M3：实现生产工单页与生产用料清单页双向展示。`completed`
4. M4：定向验证、证据回填与 E2E 预检。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js`
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\frontend-feature-evidence.md`

## Final Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-order-material-link-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\erp-production-material-list-static.spec.js` -> PASS
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-erp-production-order-material-list-bidirectional-link\frontend-feature-evidence.md` -> PASS
- 本地真实页面只读验证 -> PASS：
  - 登录预检可进入生产工单页和生产用料清单页
  - 新路由参数落点有效，已自动回填/打开目标页面状态
  - 当前测试租户暂无真实双向关联样本，因此未做“点击真实关联记录后闭环跳转”验收

## Current Blockers

- 无代码阻塞；仅当前本地测试租户暂无真实关联样本。
