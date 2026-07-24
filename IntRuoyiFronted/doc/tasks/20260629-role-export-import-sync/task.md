# 任务：角色管理导出导入联通（前端）

- Task ID: `20260629-role-export-import-sync`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

在权限角色、组织角色、审批角色三个页面补上“导出后可再次导入”的前端入口与交互，保证按钮、上传、错误提示和下载文件名与新合同对齐。

## Previous Task Check

- 上一前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-keyword-label\task.md`
- 状态：`completed`
- 处理说明：无未完成阻塞项。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\experience-index.md`：任务开始前先命中相关经验并摘录。
- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文文档和输出保持 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：延续现有角色管理工具栏与页面密度，不做无关 UI 重构。

## 设计约束检查

- 是否引入 fallback/降级/吞异常：否。
- 是否从根因和长期维护角度解决：是。直接接入统一导入导出合同与页面入口，不做临时“只下载 JSON、不支持导入”的半成品。
- 是否存在临时补丁或绕过：否。

## BDD

- BDD: 权限角色页提供导入导出闭环 -> Given 用户进入权限角色页 / When 点击导出并再次上传导出产物 / Then 页面能完成下载与导入调用，并对成功失败给出明确反馈。
- BDD: 组织角色页提供导入导出闭环 -> Given 用户进入组织角色页 / When 点击导出并再次上传导出产物 / Then 页面能完成下载与导入调用，并对成功失败给出明确反馈。
- BDD: 审批角色页提供导入导出闭环 -> Given 用户进入审批角色页 / When 点击导出并再次上传导出产物 / Then 页面能完成下载与导入调用，并对成功失败给出明确反馈。

## Milestones

- M1: 阅读现有三页实现与现有静态测试。状态：completed。
- M2: 增加 RED 静态测试锁定入口与合同。状态：completed。
- M3: 完成 API 封装与页面交互。状态：completed。
- M4: 跑静态测试并回填证据。状态：completed。

## Expected Verification

- `node yudao-ui-admin-vue3/tests/e2e/...`

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\role-config-package-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\role-management-split-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\node_modules\eslint\bin\eslint.js ...` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\role-config-package-roundtrip-real.e2e.js` -> BLOCKED，真实浏览器链路已完成登录、导出、导入请求与回放比对框架，但最终被后端运行态阻塞，尚未完成三页面“导入导出完全一致”验收。
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\role-config-package-roundtrip-real.e2e.js` -> PASS，三页面真实浏览器链路均完成导出、原包导入、再次导出，并通过完全一致比对。

## Current Blockers

- 无。
