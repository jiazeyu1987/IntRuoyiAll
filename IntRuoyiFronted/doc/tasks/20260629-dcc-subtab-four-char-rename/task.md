# 任务：DCC 文控中心子页签改为四字名称

- Task ID: `20260629-dcc-subtab-four-char-rename`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `completed`

## Task Goal

统一 `DCC文控中心` 侧边栏子页签标题为不带 `DCC` 前缀的 4 字名称，且各子页签名称互不重复，不改变既有路由 path、name 与页面功能。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-srm-nas-locator-keyword-label\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，本次继续处理 DCC 菜单文案统一。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文任务文档与日志维护必须显式 UTF-8。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次只改菜单/标题文案，不改变页面布局、颜色和操作台结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。前端路由标题、必要页面页头与静态断言一起统一，避免菜单与页面名称不一致。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: DCC 文控中心子页签显示四字标题 -> Given 用户进入系统并展开 DCC 文控中心 / When 子页签渲染 / Then 文控权限之外的目标页签均显示无 DCC 前缀且互不重名的 4 字名称。`

## Milestones

1. M1：建立前端任务文档与执行日志。`completed`
2. M2：先更新静态断言并执行 RED。`completed`
3. M3：修改路由与页面标题文案并执行 GREEN。`completed`

## Expected Verification

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mdm-tenant-package-real-setup-static.spec.js`

## Current Blockers

- 无。

## Final Verification Result

- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\dcc-subtab-four-char-static.spec.js` -> PASS
- `node D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\tests\e2e\mdm-tenant-package-real-setup-static.spec.js` -> PASS

## Completion Result

- 前端隐藏路由标题已同步为 `模板配置`、`文件审计`。
- 套餐静态菜单契约已同步 `文件提交 / 个人文件 / 文件查阅`，打印模板页头同步为 `模板配置`。
