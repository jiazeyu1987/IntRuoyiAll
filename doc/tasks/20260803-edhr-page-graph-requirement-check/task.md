# 批记录页面关系图要求检查与修正

## Task Goal

检查当前“批记录页面关系图”是否符合既定页面关系要求；若发现不符合，按现有前端契约最小修正，并保持 VueFlow 只读节点可点击、页面跳转真实可用、错误不降级。

## Milestones

- [x] 定位页面关系图入口、组件、既有静态合同和历史验收要求。
- [x] 对照要求补充或更新最小 RED 静态合同。
- [x] 实施最小前端修正，避免引入 fallback、mock 或坐标点击绕过。
- [x] 运行定向 GREEN、相邻回归、证据 validator 和必要的结构验证。
- [x] 更新 verification report 与 closeout 状态，选择性提交本任务文件。

## Expected Verification

- `node tests/e2e/edhr-batch-page-graph-tab-static.spec.js`
- 受影响组件的相邻静态合同或最小扫描命令。
- `python C:\Users\BJB110\.codex\skills\frontend-feature-delivery\scripts\validate_frontend_feature.py --evidence doc/tasks/20260803-edhr-page-graph-requirement-check/frontend-feature-evidence.md`
- `git diff --check`

## Applicable Gates

- 前端 VueFlow 只读图点击层级门禁：节点必须真实可点击并进入目标路由，禁止 `force: true`、坐标点击、API-only 或隐藏 pane 冒充通过。
- 工艺路线三类配置术语契约：批记录表单、表单槽位、工序开始三类入口不得互相替代。
- 前端静态契约隔离门禁：如全量检查受无关并行改动阻塞，使用当前需求专用最小静态合同记录 RED/GREEN。

## Design Constraint Check

- `是否引入 fallback/降级/吞异常`：否。
- `是否从根因和长期维护角度解决`：是；以页面关系图正式节点/路由契约为准，不靠测试绕过。
- `是否存在临时补丁或绕过`：否。

## Current Status

completed

- 已读取 `docs/task-closeout-rules.md`、`docs/powershell-encoding.md`、`docs/powershell-memory.md`、`docs/frontend-development.md`、`docs/e2e-rules.md`、`docs/experience-index.md`、`frontend-feature-delivery` 技能、`project-experience-consolidation` 技能和 `task-closeout-cleanup` 技能。
- 已保存任务开始前既有脏改基线提交：`a653452fc`、`ca51198aa`、`a985e2497`。
- 发现基线后仍有并行任务改动持续出现；本任务只选择性暂存页面关系图相关文件。
- 已完成本任务定向 GREEN；相邻页签合同失败于既有 PQC “长度”断言，记录为非本任务 blocker。
- cleanup preview/apply 已通过，仅删除任务临时 `frontend-feature-evidence.md`，保留 `task.md`、`execution-log.md`、`verification-report.md`。
