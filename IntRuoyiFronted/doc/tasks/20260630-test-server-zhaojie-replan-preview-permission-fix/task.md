# 任务：测试服 zhaojie 预览重排无权限修复（前端）

- Task ID: `20260630-test-server-zhaojie-replan-preview-permission-fix`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-30`
- Current Status: `completed`

## Task Goal

修复排产工单页手动重排抽屉的权限展示合同：只有拥有 `mes:pro-auto-schedule:replan` 的用户才暴露“手动重排/预览重排/应用重排”入口，并在程序化打开抽屉时 fail fast 提示，避免出现按钮可见却被后端拒绝的误导性体验。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260630-commit-frontend-code\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成；本次进入新的权限缺陷修复任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`
  - 命中 `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`、`D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`
  - Vue 页面、静态测试与任务文档统一显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`
  - 若后续需要真实登录复验，必须先走官方最小路径。
- `D:\ProjectPackage\Int\IntRuoyi\docs\server-access.md`
  - 本轮只改本机前端源码与静态门禁，不直接连接测试服。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。直接按真实后端权限合同绑定前端入口，消除错配。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 无 replan 权限时不暴露手动重排入口 -> Given 用户缺少 mes:pro-auto-schedule:replan / When 打开排产工单页 / Then 工具栏不显示“手动重排”按钮。`
- `BDD: 无 replan 权限时程序化打开也应 fail fast -> Given URL 或其他动作触发 openReplanDrawer / When 当前用户缺少 mes:pro-auto-schedule:replan / Then 页面提示无手动重排权限且不打开抽屉。`
- `BDD: 有 replan 权限时预览与应用入口一致受控 -> Given 用户拥有 mes:pro-auto-schedule:replan / When 打开手动重排抽屉 / Then “预览重排”和“应用重排”入口继续可见并与后端合同一致。`

## Milestones

1. M1：确认前端当前误展示点与可复用权限工具。`completed`
2. M2：补 RED 静态门禁测试。`completed`
3. M3：实现最小权限门禁修复并跑到 GREEN。`completed`
4. M4：回填前端证据与风险说明。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js`

## Final Verification Result

- 已把排产工单页手动重排入口与 `mes:pro-auto-schedule:replan` 对齐：
  - 工具栏“手动重排”按钮受 `v-hasPermi="['mes:pro-auto-schedule:replan']"` 保护。
  - 抽屉内“预览重排”“应用重排”仅在 `hasReplanPermission` 为真时渲染。
  - `openReplanDrawer` 对无权限用户 fail fast 提示“当前账号没有手动重排权限”。
- 已通过：
  - `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-usability-static.spec.js`
  - `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mes-pro-schedule-order-pool-static.spec.js`

## Current Status

- `completed`

## Current Blockers

- 浏览器级最终复验需要后续单独授权连接测试服并使用 `zhaojie` 当前有效口令。
