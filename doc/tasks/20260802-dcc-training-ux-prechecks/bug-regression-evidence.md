# Bug Regression Evidence

## Bug Summary And Expected Behavior

- 培训对象看不到计时是否有效以及确认按钮为何不可用，容易误以为系统卡住。
- 文控详情缺少完成率、最近确认时间和未完成人员的集中摘要。
- 待正式下发但当前账号无动作权限时，页面缺少 `DISTRIBUTE` 和分发规则提示。
- 培训规则入口未提示培训对象需要 `dcc:controlled-file:training:mine`。
- 期望行为：页面显式呈现真实状态和缺失前置条件，不改变后端状态机、权限或接口。

## Reproduction Command Or Path

- Path: `/dcc/controlled-file/training-task/:progressId`
- Path: `/dcc/controlled-file/detail/:id`
- Path: DCC 培训规则只读页和文件类别培训规则页
- Reproduction contract: `pnpm --dir "E:\IntRuoyi\IntRuoyiFronted" e2e:dcc:training-ux-prechecks:static`

## Root Cause

现有页面已经具备预览、计时会话、培训状态和动作权限数据，但只显示简单会话文案或分散明细，没有把预览加载、页面聚焦、剩余时长、完成汇总和权限前置条件转换为可操作的用户提示。

## Regression Test

新增 `IntRuoyiFronted/tests/e2e/dcc-training-ux-prechecks-static.spec.cjs`，覆盖计时状态、确认原因、管理汇总、未完成人员、正式下发权限缺口和培训对象权限预检。

## RED And GREEN

- RED: 任务专用契约在实现前因缺少 `dcc-training-task-countability-state` 失败。
- GREEN: 任务专用契约、详情培训摘要契约、培训规则上下文契约和 `pnpm ts:check` 全部通过。

## Verification

- 任务专用静态契约证明新增状态、原因、汇总和权限预检提示存在。
- 相邻静态契约证明详情培训摘要和培训规则上下文未回归。
- `pnpm ts:check` 证明 Vue/TypeScript 类型检查通过。

## Risk And Regression Scope

- 只新增前端派生状态、展示组件和静态契约；没有修改 API、数据库、角色权限、培训确认写入或发布状态机。
- 当前没有可用于自动检查每个培训对象菜单权限的正式 API，因此页面只做明确的人工预检提醒，不伪造自动校验结果。
- 实现位于混合基线提交 `1606947b7`；本任务不处理同一提交内的其它并行改动。

## Blockers And Follow-Up

- 两组无关历史静态契约仍因旧页面标记或缺失历史 SQL 路径失败。
- 当前分支含无关提交和脏改动，本任务不代为提交或推送。
