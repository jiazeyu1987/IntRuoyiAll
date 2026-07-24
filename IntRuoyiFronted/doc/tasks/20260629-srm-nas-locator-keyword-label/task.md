# 任务：SRM NAS定位 搜索标签文案改为关键词

- Task ID: `20260629-srm-nas-locator-keyword-label`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

将 NAS定位 页搜索栏中的“文件名关键字”标签与输入提示统一调整为“关键词”，不改变搜索接口、参数和页面布局。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-schedule-calendar-shift-toggle-buttons\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，本次继续处理 NAS定位 页的单点文案调整。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文文档与日志维护使用 UTF-8，避免 PowerShell 默认编码污染。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：保持 IntPP 工具栏样式，只做文案层变更，不重构布局。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。同步更新页面源码和静态/真实 E2E 断言，确保后续文案不回退。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 用户查看 NAS定位 搜索栏时看到统一关键词文案 -> Given 用户进入 /srm/nas-locator / When 搜索表单渲染 / Then 标签显示“关键词”，输入框提示显示“请输入关键词”。`

## Milestones

1. M1：创建前端任务文档与执行日志。`completed`
2. M2：先修改测试断言并执行 RED。`completed`
3. M3：修改页面文案并执行 GREEN 回归。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\srm\nas-locator-static.spec.js` -> PASS

## Completion Result

- 页面标签已更新为“关键词”，placeholder 已更新为“请输入关键词”。
- `nas-locator-static` 与 `nas-locator-real-flow` 断言已同步使用新文案，避免后续回退。
