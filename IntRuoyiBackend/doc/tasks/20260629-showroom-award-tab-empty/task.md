# 任务：展厅产品管理奖项页签无数据

- Task ID: `20260629-showroom-award-tab-empty`
- Workspace: `D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro`
- Created: `2026-06-29`
- Current Status: `blocked`

## Task Goal

确认并修复 `GET /admin-api/showroom/award/page` 到前端奖项列表之间的真实数据契约问题；若后端返回结构与前端列表预期不一致，需以正式契约方式修正。

## Previous Task Check

- 上一个后端任务：`D:\ProjectPackage\Int\IntRuoyi\ruoyi-vue-pro\doc\tasks\20260629-showroom-hall-bu-layout-allow-empty\task.md`
- 状态：`completed`
- 处理说明：上一后端任务已完成，不阻塞本次任务。

## 经验门禁

- `D:\ProjectPackage\Int\IntRuoyi\docs\powershell-memory.md`：中文任务文档与日志必须显式 UTF-8。
- `D:\ProjectPackage\Int\IntRuoyi\docs\login-access.md`：若需要真实登录/接口联调验证，先按登录门禁执行。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是。若存在分页接口契约与前端列表契约偏差，直接修正正式数据结构或配套回归，不用默认空值掩盖。
- `是否存在临时补丁或绕过`：否。

## BDD 场景

- `BDD: 奖项分页返回可显示字段 -> Given 后端存在真实奖项和当前显示修订 / When 调用 /showroom/award/page / Then 返回行可被前端直接解析出名称、颁发单位、日期/期限和封面字段。`

## Milestones

1. M1：建立任务文档并定位 `/showroom/award/page` 契约。`completed`
2. M2：根据复现结果补 RED 回归。`blocked`
3. M3：实现最小后端修复并执行 GREEN。`pending`
4. M4：更新文档并完成收尾。`pending`

## Expected Verification

- 若涉及后端修改，补充对应测试命令

## Current Blockers

- 当前 `/admin-api/showroom/award/page` 在本机真实账号下返回正常数据，尚无后端空数据复现证据；需补充用户实际上下文后再判断是否需要后端修复。
- 新需求 `20260629-showroom-award-generate-cover-version` 已单独立项，本任务不再继续混入新功能改动。
