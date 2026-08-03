# 20260803 EDHR 批记录页签增加组长子页签

## Task Goal

在 eDHR 批记录页签下增加可访问“生产组长”和“PQC 组长”前端入口的子页签，保持现有批记录页面路由、权限和页面组件契约，不引入后端 fallback 或静默降级。

## Milestones

- [x] 定位现有 eDHR 批记录页签、子页签/路由配置、生产组长和 PQC 组长前端入口。
- [x] 先补充最小静态合同，证明旧状态缺少两个子页签入口。
- [x] 实现最小前端页签/路由接入，保持现有授权路由边界和组件加载方式。
- [x] 运行目标静态合同、相邻前端检查和类型检查；记录无法执行的真实 E2E 前置条件。
- [x] 更新任务证据、验证报告和收尾状态。

## Expected Verification

- `node tests/e2e/edhr-batch-record-leader-tabs-static.spec.js`
- `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- `node tests/e2e/team-leader-workbench-static.spec.cjs`
- `pnpm ts:check`
- 前端功能证据校验已在 cleanup 前通过；临时 `frontend-feature-evidence.md` 已按 closeout 清理，摘要保留在 `verification-report.md`。

## Current Status

blocked

- 已读取前端开发、E2E、数据库/权限、登录访问、PowerShell 编码、任务收尾和前端交付技能规则。
- 发现工作区已有非本任务脏改动与暂存改动；本任务仅修改 EDHR 批记录相关前端、目标静态合同和本任务文档。
- 目标功能已实现并通过任务专用静态合同、页面关系图合同、组长工作台合同和 diff 检查。
- 全量 `pnpm ts:check` 与旧相邻一线填写合同存在非本任务阻塞，已记录在验证报告。
- cleanup preview/apply 已完成，仅删除本任务临时 `frontend-feature-evidence.md`。
- 最终提交/推送未执行：当前 `int_main` 已 ahead 6，且工作区存在多项非本任务 staged/unstaged/untracked 改动；为避免混入并行任务，本任务在 Git closeout 门禁处阻塞。

## 设计约束检查

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是，按现有页签/路由契约增加正式入口，不用隐藏按钮、直链或默认成功替代权限路由。
- `是否存在临时补丁或绕过`：否。

## Applicable Gates

- 前端权限页签正向授权门禁：默认入口与隐藏子路由必须来自已授权动态子路由或明确普通用户页签，不得补回未授权页签。
- 动态菜单页签重命名/入口门禁：若涉及动态菜单入口，需同步正式菜单配置、角色绑定和登录后权限响应；本任务优先确认是否仅为前端页签聚合。
- 前端静态契约隔离门禁：若全量 `pnpm ts:check` 或既有大契约因无关历史失败阻塞，使用任务专用最小静态合同记录 RED/GREEN。
- 严格无 fallback：缺少正式生产组长/PQC 组长前端入口、路由或权限契约时必须阻塞，不得新增占位页、空成功页或静默直链。
