# 任务：展厅产品管理奖项页签无数据

- Task ID: `20260629-showroom-award-tab-empty`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3`
- Created: `2026-06-29`
- Current Status: `blocked`

## Task Goal

修复 `展厅 -> 产品管理 -> 奖项` 列表未展示真实奖项数据的问题，确保前端正确加载并归一化奖项分页接口返回的数据结构。

## Previous Task Check

- 上一个前端任务：`D:\ProjectPackage\Int\IntRuoyi\yudao-ui-admin-vue3\doc\tasks\20260629-dcc-subtab-four-char-rename\task.md`
- 状态：`completed`
- 处理说明：上一前端任务已完成，不阻塞本次任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文日志与任务文档必须显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：如需真实登录页签验证，先按登录门禁执行。
- `D:\ProjectPackage\Int\IntPP\FRONTEND_STYLE.md`：本次仅修复既有列表数据链路，不改变既有页面风格与结构。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。优先对齐奖项分页接口契约与前端列表归一化/触发时机。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 奖项页签展示真实奖项列表 -> Given 奖项分页接口返回真实数据 / When 用户进入奖项页签 / Then 表格显示奖项编码、名称、颁发单位、日期/期限和封面。`
- `BDD: 奖项列表接受嵌套修订结构 -> Given 奖项行字段位于 displayRevision 或 revision / When 前端归一化奖项行 / Then 列表可平铺渲染，不因嵌套结构而空白。`

## Milestones

1. M1：建立任务文档并定位前端奖项列表链路。`completed`
2. M2：先补静态回归并执行 RED。`blocked`
3. M3：修改最小前端实现并执行 GREEN。`pending`
4. M4：更新文档并完成收尾。`pending`

## Expected Verification

- `node scripts/showroom-admin-award-list.test.mjs`

## Current Blockers

- 本机真实页面在 `测试租户/aoteman` 与 `芋道源码/admin` 下都能看到奖项数据，暂未复现用户描述；需补充实际租户、账号或环境差异后再继续。
- 新需求 `20260629-showroom-award-generate-cover-version` 已单独立项，本任务不再继续混入新功能改动。
